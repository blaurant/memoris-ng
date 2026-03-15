# Coding Conventions

**Analysis Date:** 2026-03-15

## Naming Patterns

**Files:**
- Clojure: kebab-case (`consumption.clj`, `user_test.clj`)
- ClojureScript: kebab-case (`auth_events.cljs`, `auth_subs.cljs`)
- Test files: `*_test.clj` suffix (e.g., `consumption_test.clj`)

**Functions:**
- Verbs for side effects: `save!`, `find-by-id`, `find-all`, `delete!`, `reset!`
- Nouns/transformations for pure functions: `build-consumption`, `create-new-consumption`, `publish`, `unpublish`
- Guard functions use `assert-` prefix: `assert-lifecycle`, `assert-email-verified` (not `!` suffix — reserved for mutation only)
- Private functions: `defn-` prefix (e.g., `-validate`, `-assert-lifecycle`, `-fresh-repo`)
- Re-frame events/subs: keyword-namespaced (`:auth/login-with-provider`, `:auth/token`, `:auth/user`)

**Variables:**
- camelCase in domain maps with namespaced keywords: `:user/id`, `:user/email`, `:consumption/lifecycle`, `:network/radius-km`
- Local bindings: kebab-case (`user-repo`, `token-verifier`, `email-sender`)
- Sequences: plural form (`users`, `networks`, `consumptions`)
- Maps of entities: `key->value` format (e.g., `id->network` for `{id1 network1 id2 network2}`)
- Default parameter names per "Elements of Clojure": `x` (single value), `xs` (sequence), `m` (map), `f` (function)

**Types/Schemas:**
- PascalCase for Malli schemas: `BaseConsumption`, `ConsumerInformation`, `User`, `Network`
- Predicate functions: suffix with `?` (e.g., `id?`, `email?`, `onboarding?`, `contains-point?`)

## Code Style

**Formatting:**
- Tools: cljfmt (checked via `clj -M:fmt`)
- Indentation: 2 spaces
- Line wrapping: convention observed (see below)

**Linting:**
- Tool: clj-kondo v2024.08.01
- Config: `.clj-kondo/` directory (auto-configured)
- Run: `clj -M:lint src test`
- Standards: Clojure idioms, no unused variables, strict arity checking

**Comments & Docstrings:**
- Comment section headers use Markdown-style: `;; ── Section Name ──────────────────`
- Docstrings in functions describe purpose, parameters, behavior, and exceptions
- Example from `domain/consumption.clj`:
  ```clojure
  (defn create-new-consumption
        "Create a new consumption in :consumer-information state."
        [id user-id]
        ...)
  ```
- Example from `infrastructure/rest_api/auth_handler.clj`:
  ```clojure
  (defn- login-handler
    "POST /api/v1/auth/login — body {:provider \"google\" :id-token \"...\"}
    or {:provider \"email\" :email \"...\" :password \"...\"}
    Returns {:token \"jwt\" :user {...}}"
    [user-repo token-verifier password-hasher email-sender jwt-secret]
    ...)
  ```

## Import Organization

**Order (in all namespaces):**
1. Standard library (`:require [clojure.string ...]`)
2. Third-party libraries (`:require [integrant.core ...]`, `[malli.core ...]`)
3. Domain layer (`:require [domain.id ...]`, `[domain.user ...]`)
4. Application layer (`:require [application.auth-scenarios ...]`)
5. Infrastructure layer (`:require [infrastructure.rest-api.handler ...]`)
6. Java interop (`:import (java.time Instant)`)

**Namespace metadata:**
- Domain entities marked with metadata: `(ns ^{:domain/type :entity} domain.consumption ...)`
- Example: `backend/src/domain/consumption.clj`, `backend/src/domain/user.clj`, `backend/src/domain/network.clj`

**Path aliases (frontend):**
- Config defines: `API_BASE`, `GOOGLE_MAPS_API_KEY` (set via Shadow-cljs `closure-defines` in `shadow-cljs.edn`)

## Error Handling

**Pattern: Exceptions with context:**
- Use `ex-info` to throw with structured data: `(throw (ex-info "Invalid consumption" {:attrs attrs :errors (m/explain schema attrs)}))`
- Always include error message and context map
- Located in: `domain/` layer (validation), `application/` layer (business logic), `infrastructure/rest_api/` (HTTP error mapping)

**HTTP handlers (infrastructure layer):**
- Catch `clojure.lang.ExceptionInfo` at API boundary
- Map domain exceptions to HTTP status codes:
  - Validation errors → 400
  - Email not verified → 403
  - Authentication failures → 401
  - Already exists/conflicts → 400
- Example from `infrastructure/rest_api/auth_handler.clj`:
  ```clojure
  (try
    (let [user (auth/login-with-provider ...)]
      {:status 200 :body {:token token :user (serialize-user user)}})
    (catch clojure.lang.ExceptionInfo e
      (let [msg (.getMessage e)]
        {:status (cond (str/includes? msg "not verified") 403 :else 401)
         :body {:error msg}})))
  ```

**Domain layer (no exceptions at boundaries):**
- Use predicate-based guards with `assert-` prefix that throw: `(defn- assert-lifecycle [c expected] (when (not= ...) (throw ...)))`
- Pure data transformations, let exceptions bubble up to application layer

**Logging:**
- Framework: mulog (console + file)
- Usage: `(mu/log ::user-registered :user-id user-id :email email)`
- Located at: application layer (e.g., `application/auth_scenarios.clj`)

## Validation

**Location: Periphery only**
- **Domain layer:** Malli schema validation in `build-*` constructors (e.g., `build-consumption`, `build-user`, `build-network`)
- **Application layer:** Business logic validation (state machine checks, existence checks)
- **Infrastructure layer (REST handlers):** Exception mapping to HTTP responses
- Never re-validate inside pure domain functions

**Pattern (Malli):**
```clojure
(def email?
  [:and
   string?
   [:re {:error/message "must be a valid email address"}
    #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"]])

(defn- validate [schema attrs]
  (if (m/validate schema attrs)
    attrs
    (throw (ex-info "Invalid consumption" {:attrs attrs :errors (m/explain schema attrs)}))))
```

## Function Design

**Size:**
- Typical: 5–20 lines (domain entities)
- Application scenarios: 10–30 lines (orchestration)
- Handlers: 15–25 lines (request parsing + business logic invocation + response formatting)

**Parameters:**
- Injected dependencies first (repos, verifiers, hashers)
- Domain data next (maps, values)
- Example from `application/auth_scenarios.clj`:
  ```clojure
  (defn login-with-provider
        [user-repo token-verifier email-sender provider id-token]
        ...)
  ```

**Return Values:**
- Pure transformations return data (maps, lists)
- Effects (`save!`, `send-*`) return the persisted/affected entity
- Handlers return Ring response maps `{:status 200 :body ...}`

## Module Design

**Exports (public API):**
- Domain entities: `build-*`, `create-*`, state transition functions, query functions, protocol definitions
- Application scenarios: use-case functions (e.g., `login-with-provider`, `register-with-email`)
- Infrastructure: implementation record types and initializers
- Example from `domain/consumption.clj`:
  - Exports: `build-consumption`, `create-new-consumption`, `onboarding?`, `register-consumer-information`, `ConsumptionRepo` (protocol)
  - Keeps private: `-validate`, `-assert-lifecycle` (marked with `-`)

**Barrel files:**
- Not used; explicit imports preferred

**Protocols:**
- Define at end of domain modules
- Example: `ConsumptionRepo`, `NetworkRepo`, `UserRepo` in domain layer
- Implemented by repository records in infrastructure layer

## Re-frame (Frontend)

**Event registration:**
- Pattern: `(rf/reg-event-fx :event-id (fn [{:keys [db]} [_ param1 param2]] {...}))`
- FX events (side effects): `-fx` suffix (HTTP calls, navigation)
- DB events (pure): `-db` suffix (updates to app-db)
- Example from `frontend/src/app/auth/events.cljs`:
  ```clojure
  (rf/reg-event-fx :auth/login-with-provider
    (fn [{:keys [db]} [_ provider id-token]]
      {:db         (-> db (assoc :auth/loading? true) ...)
       :http-xhrio {...}}))
  ```

**Subscription registration:**
- Pattern: `(rf/reg-sub :sub-id (fn [db _] (:key db)))`
- Composed subs: `:<- [:dependency-sub]`
- Example from `frontend/src/app/auth/subs.cljs`:
  ```clojure
  (rf/reg-sub :auth/admin?
    :<- [:auth/user-role]
    (fn [role _] (= "admin" role)))
  ```

## Clojure Idioms Reference

Reference: `.claude/clojure_idioms.md` (built on "Elements of Clojure" by Zachary Tellman)

**Key principles enforced:**
- **Pull / Transform / Push:** Separate fetch (pull), process (transform), output (push) phases
- **Operational vs. Functional:** Keep operational concerns (I/O, retries, errors) at edges; keep core logic pure
- **Mutual recursion:** Use `letfn` only for mutual recursion; use `let` + `fn` otherwise
- **Inequality ordering:** Write `(< a b)` not `(> b a)` for clarity
- **Atoms for mutable state:** Use atoms by default, only move to refs (STM) for multi-object consistency
- **Avoid agents:** Unbounded queues cause issues
- **Signal effects:** Use explicit `do` or `_` assignments to flag side effects to readers

---

*Conventions analysis: 2026-03-15*
