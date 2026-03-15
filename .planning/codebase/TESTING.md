# Testing Patterns

**Analysis Date:** 2026-03-15

## Test Framework

**Runner:**
- Kaocha v1.91.1392 (Clojure test runner)
- Config: `backend/tests.edn`
- Base library: `clojure.test`

**Test types:**
- Unit tests: domain logic, state machines, validation
- BDD scenarios: application/use-case testing with GIVEN-WHEN-THEN structure
- No E2E tests (integration tests via API handlers)

**Run Commands:**
```bash
# All tests
cd backend && clj -M:test

# Watch mode
cd backend && clj -M:test --watch

# Coverage
# (Not detected - no coverage tool configured)
```

## Test File Organization

**Location:**
- Backend: `backend/test/` (mirrors `backend/src/` structure)
- Domain tests: `backend/test/domain/`
- Application tests: `backend/test/application/`
- BDD utilities: `backend/test/bdd/`
- System tests: `backend/test/system_test.clj`

**Naming:**
- Unit/domain tests: `*_test.clj` (e.g., `consumption_test.clj`, `user_test.clj`)
- Application tests: `*_test.clj` with BDD scenarios (e.g., `auth_scenarios_test.clj`)
- BDD framework: `test/bdd/test_gwt.clj` (custom GIVEN-WHEN-THEN DSL)

**Structure:**
```
backend/test/
├── bdd/
│   └── test_gwt.clj              # BDD DSL macros
├── domain/
│   ├── consumption_test.clj
│   ├── user_test.clj
│   ├── network_test.clj
│   ├── datetime_test.clj
│   ├── production_test.clj
│   ├── eligibility_check_test.clj
│   └── verification_token_test.clj
├── application/
│   ├── auth_scenarios_test.clj
│   ├── consumption_scenarios_test.clj
│   └── production_scenarios_test.clj
└── system_test.clj
```

## Test Structure

**Domain tests (unit):**
```clojure
(ns domain.consumption-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.consumption :as consumption]))

(def user-id (id/build-id))

(def valid-attrs
  {:consumption/id        (id/build-id)
   :consumption/user-id   user-id
   :consumption/lifecycle :consumer-information})

;; ── build-consumption ──────────────────────────────────────────────────────

(deftest build-consumption-valid
  (testing "builds a consumption with valid minimal attributes"
    (let [c (consumption/build-consumption valid-attrs)]
      (is (= :consumer-information (:consumption/lifecycle c)))
      (is (= user-id (:consumption/user-id c))))))

(deftest build-consumption-invalid
  (testing "throws when id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (dissoc valid-attrs :consumption/id))))))
```

**Patterns:**
- One `deftest` per function or behavior
- Use `testing` strings to organize test cases within a `deftest`
- Section headers: `;; ── Function Name ──────────────────────────────` (matches domain code style)
- Shared fixtures defined as `def` at top of test namespace (e.g., `valid-attrs`, `user-id`)

**BDD scenarios (application):**
```clojure
(ns application.auth-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.auth-scenarios :as auth]
            ...))

(defscenario "New user signs in — user is created with defaults"
  (GIVEN "a fresh repo and verifier" [ctx]
    (assoc ctx
           :repo     (fresh-repo)
           :verifier (mock-verifier)
           :sender   (fresh-email-sender)))

  (WHEN "a new user signs in with Google" [ctx]
    (assoc ctx :user
           (auth/login-with-provider (:repo ctx) (:verifier ctx) (:sender ctx)
                                     :google "fake-id-token")))

  (THEN "user is created with role customer and lifecycle alive, welcome email sent" [ctx]
    (assert (= :customer (:user/role (:user ctx))))
    (assert (= :alive    (:user/lifecycle (:user ctx))))
    (assert (= "alice@example.com" (:user/email (:user ctx))))
    (assert (= 1 (count @(:welcome-sent (:sender ctx)))))))
```

**DSL behavior:**
- Each step (`GIVEN`, `WHEN`, `THEN`) receives context map `[ctx]`
- Multiple expressions in a step are threaded automatically
- If expression returns a map, it becomes new context
- If expression returns nil (e.g., `assert`), context is preserved
- No need for trailing `ctx` after assertions
- Stops at first failure, reports all steps with pass/fail status

## Mocking

**Framework:** Custom in-memory implementations + `defrecord` mocks

**Patterns (from `application/auth_scenarios_test.clj`):**
```clojure
;; ── Mock Token Verifier ──────────────────────────────────────────────────────

(defrecord MockTokenVerifier []
  tv/TokenVerifier
  (verify-provider-token [_ _provider _id-token]
    {:subject-id "provider-user-123"
     :email      "alice@example.com"
     :name       "Alice"}))

;; ── Mock Password Hasher ────────────────────────────────────────────────────

(defrecord MockPasswordHasher []
  password-hasher/PasswordHasher
  (hash-password [_ raw] (str "hashed:" raw))
  (check-password [_ raw hashed] (= (str "hashed:" raw) hashed)))

;; ── In-memory Verification Token Repo ───────────────────────────────────────

(defrecord InMemoryVtRepo [store]
  vt/VerificationTokenRepo
  (save! [_ token]
    (swap! store assoc (:verification-token/id token) token)
    token)
  (find-by-token [_ token-string]
    (some (fn [t] (when (= token-string (:verification-token/token t)) t))
          (vals @store)))
  (delete! [_ id]
    (swap! store dissoc id)
    nil))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-repo []
  (mem-repo/->InMemoryUserRepo (atom {})))

(defn- mock-verifier []
  (->MockTokenVerifier))

(defn- fresh-hasher []
  (->MockPasswordHasher))

(defn- fresh-email-sender []
  (->MockEmailSender (atom []) (atom [])))
```

**What to Mock:**
- External services: `TokenVerifier`, `PasswordHasher`, `EmailSender` (via `defrecord` implementing protocols)
- Repositories: In-memory atom-backed versions (e.g., `InMemoryUserRepo`)
- Time-dependent behavior: Pass time values as parameters (e.g., `Instant/now` passed explicitly)

**What NOT to Mock:**
- Domain logic: Test real `build-consumption`, `publish`, state transitions
- Validation (Malli schemas): Run real validation, test error cases
- Pure functions: Test directly without mocking

## Fixtures and Factories

**Test Data:**
```clojure
(def valid-attrs
  {:consumption/id        (id/build-id)
   :consumption/user-id   user-id
   :consumption/lifecycle :consumer-information})
```

**Factories:**
```clojure
(defn- fresh-repo []
  (mem-repo/->InMemoryUserRepo (atom {})))

(defn- mock-verifier []
  (->MockTokenVerifier))
```

**Location:**
- Shared fixtures: Defined at top of test namespace as `def` (e.g., `valid-attrs`)
- Fresh instances: Created via `-` prefixed factory functions (e.g., `fresh-repo`, `mock-verifier`)
- Each test gets fresh repos via calling factory, not sharing state

**Pattern (from `application/auth_scenarios_test.clj`):**
- Mocks and fixtures co-located in test file where used
- No separate fixtures directory; in-memory implementations preferred
- Factory functions used in `GIVEN` steps to initialize context

## Coverage

**Requirements:** Not enforced (no coverage tool configured)

**Test coverage reality (inferred from file structure):**
- Domain layer: High (all state machines, validators tested)
- Application layer: High (use-case scenarios in BDD tests)
- Infrastructure layer: Medium (HTTP handlers tested indirectly via application scenarios)
- Integration: Low (no database integration tests; in-memory repos used)

## Test Types

**Unit Tests:**
- Scope: Single domain function or state machine
- Files: `backend/test/domain/*_test.clj`
- Approach: Test valid inputs, invalid inputs, error cases, edge cases
- Example: `domain/consumption_test.clj` tests each state transition
- Does NOT mock domain logic; tests real `build-*` and transition functions
- Uses `is (thrown? ...)` for exception assertions

**Application/BDD Tests:**
- Scope: Use case or business scenario (multi-step flows)
- Files: `backend/test/application/*_test.clj`
- Approach: GIVEN-WHEN-THEN format with context threading
- Example: `auth_scenarios_test.clj` tests login flows, registration flows
- Mocks external dependencies (repos, email, verifier)
- Tests real application logic (orchestration, business rules)

**Integration Tests:**
- Approach: In-memory repo + mocked external services
- Tests: Full scenario end-to-end with real domain + application logic
- Example: BDD scenario "New user signs in — user is created with defaults" exercises:
  1. Real user creation (domain)
  2. Real save to repo (in-memory)
  3. Real email sender mock (records calls)

**E2E Tests:**
- Not found in codebase
- API handler tests done via application scenarios with mocked repos

## Common Patterns

**Async Testing:**
- Not applicable (Clojure backend is synchronous)
- Frontend uses `ajax.core` for HTTP (compiled to JS/browser environment)

**Error Testing:**
```clojure
;; Unit test error case
(deftest build-consumption-invalid
  (testing "throws when id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (dissoc valid-attrs :consumption/id))))))

(deftest build-consumption-invalid
  (testing "throws when lifecycle is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid consumption"
                         (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :invalid))))))

;; Application test error case
(defscenario "Suspended user signs in — exception thrown"
  (GIVEN "a suspended user in the repo" [ctx]
    (let [repo     (fresh-repo)
          verifier (mock-verifier)
          sender   (fresh-email-sender)
          u        (auth/login-with-provider repo verifier sender :google "token1")
          suspended (user/suspend u)]
      (user/save! repo suspended)
      (assoc ctx :repo repo :verifier verifier :sender sender)))

  (WHEN "the suspended user tries to sign in" [ctx]
    (try
      (auth/login-with-provider (:repo ctx) (:verifier ctx) (:sender ctx)
                                :google "token2")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception e))))

  (THEN "exception is thrown" [ctx]
    (assert (some? (:exception ctx)))))
```

**State Machine Testing (consumption lifecycle):**
```clojure
;; Test each transition
(deftest register-consumer-information-test
  (testing "transitions from :consumer-information to :linky-reference"
    (let [c       (consumption/create-new-consumption (id/build-id) user-id)
          network (id/build-id)
          c'      (consumption/register-consumer-information c "123 rue de Paris" network)]
      (is (= :linky-reference (:consumption/lifecycle c')))
      (is (= "123 rue de Paris" (:consumption/consumer-address c')))
      (is (= network (:consumption/network-id c')))))

  (testing "throws when not in :consumer-information state"
    (let [c (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :linky-reference))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (consumption/register-consumer-information c "addr" (id/build-id)))))))

;; Test chained transitions
(deftest sign-contract-test
  (testing "signing all 3 contracts transitions to :pending"
    (let [c  (-> (consumption/create-new-consumption (id/build-id) user-id)
                 (consumption/register-consumer-information "addr" (id/build-id))
                 (consumption/associate-linky-reference "LINKY-12345")
                 (consumption/complete-billing-address "456 avenue de Lyon"))
          c' (-> c
                 (consumption/sign-contract :elinkco "2026-03-04T10:00:00Z")
                 (consumption/sign-contract :producer "2026-03-04T10:01:00Z")
                 (consumption/sign-contract :sepa "2026-03-04T10:02:00Z"))]
      (is (= :pending (:consumption/lifecycle c')))
      (is (= "2026-03-04T10:00:00Z" (:consumption/contract-signed-at c')))
      (is (= "2026-03-04T10:01:00Z" (:consumption/producer-contract-signed-at c')))
      (is (= "2026-03-04T10:02:00Z" (:consumption/sepa-mandate-signed-at c'))))))
```

**Validation Testing:**
```clojure
;; Test valid schema
(deftest build-consumption-valid
  (testing "builds a consumption with valid minimal attributes"
    (let [c (consumption/build-consumption valid-attrs)]
      (is (= :consumer-information (:consumption/lifecycle c))))))

;; Test invalid inputs
(deftest build-consumption-invalid
  (testing "throws when id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (dissoc valid-attrs :consumption/id)))))
  (testing "throws on invalid lifecycle"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :invalid))))))
```

---

*Testing analysis: 2026-03-15*
