# Architecture

**Analysis Date:** 2026-03-15

## Pattern Overview

**Overall:** Domain-Driven Design (DDD) with 3-tier layered architecture (Domain-Application-Infrastructure)

**Key Characteristics:**
- Pure domain logic isolated from infrastructure concerns
- Scenario-based application layer orchestrating use cases
- Repository pattern for data persistence abstraction
- Integrant component lifecycle management
- Re-frame state management (frontend)

## Layers

**Domain Layer:**
- Purpose: Business logic and data validation rules. Pure functions with no external dependencies.
- Location: `backend/src/domain/`
- Contains: Entity schemas (Malli), state machines, protocols, guards, factory functions
- Depends on: Malli (validation framework), Java standard library
- Used by: Application layer

**Application Layer:**
- Purpose: Orchestration of use cases, business workflows, event logging
- Location: `backend/src/application/`
- Contains: Scenario functions that compose domain logic, mulog event publishing
- Depends on: Domain layer, repositories (via injected dependencies)
- Used by: REST API handlers

**Infrastructure Layer:**
- Purpose: External system integration, persistence, HTTP API, authentication
- Location: `backend/src/infrastructure/`
- Contains: XTDB repositories, REST handlers, JWT/auth, email senders, RocksDB configuration
- Depends on: Domain layer, Application layer, external services (XTDB, Resend, OAuth)
- Used by: Entry point (`main.clj`)

**Frontend State Layer (Re-frame):**
- Purpose: Client-side state management and UI coordination
- Location: `frontend/src/app/`
- Contains: app-db (initial state), events (state mutations), subscriptions (derived state)
- Depends on: Reagent (rendering), Re-frame (event system), Ajax (HTTP)
- Pattern: Unidirectional data flow; events dispatch state changes; subscriptions derive view data

## Data Flow

**HTTP Request → Response:**

1. Reitit router receives request at `backend/src/infrastructure/rest_api/handler.clj`
2. Auth middleware (`auth_middleware.clj`) extracts and verifies JWT token
3. Route handler parses parameters and calls application scenario
4. Application scenario calls domain entity functions (pure logic)
5. Scenario uses repository to read/write entities via XTDB
6. Response serialized and returned

**Example (Consumption Onboarding):**

1. POST `/api/v1/consumptions/:id/step/consumer-information` → `consumption_handler.clj:register-consumer-information-handler`
2. Handler extracts user-id from JWT, calls `scenarios/register-consumer-information`
3. Scenario calls `consumption/find-and-check-ownership` (security check)
4. Scenario calls `consumption/register-consumer-information` (domain transition)
5. Scenario calls `consumption/save!` with original and updated consumption (optimistic lock)
6. Scenario logs event with mulog
7. Updated consumption serialized and returned (200)

**Frontend Data Flow:**

1. User interaction (click, form submit) → `rf/dispatch` event vector
2. Event handler in `app/[module]/events.cljs` processes state change and/or triggers HTTP request
3. HTTP response → success/failure event dispatch
4. Success event updates app-db
5. Subscriptions in `app/[module]/subs.cljs` derive state
6. Reagent components render from subscribed values
7. New render → DOM update

**State Management:**
- Central app-db in `frontend/src/app/db.cljs` with structure: `:domain/list`, `:domain/loading?`, `:domain/error`
- All mutations via re-frame events (no direct mutations)
- Derived state via re-frame subscriptions (graph of dependencies)
- Local storage for auth token and user object (restored on app init)

## Key Abstractions

**Repository Protocol:**
- Purpose: Abstract persistence from domain logic
- Examples: `domain/UserRepo`, `domain/ConsumptionRepo`, `domain/NetworkRepo`
- Pattern: Each domain entity defines a protocol with read and write operations
- Implementation: XTDB-backed repositories in `infrastructure/xtdb/`, in-memory repositories in `infrastructure/in_memory_repo/` for testing

**Entity Schema (Malli):**
- Purpose: Data validation and documentation of entity structure
- Examples: `domain/user.clj:User`, `domain/consumption.clj:Consumption`
- Pattern: Maps with namespaced keys (`:user/email`, `:consumption/lifecycle`)
- Validation: At entity construction (domain) and HTTP boundary (handlers)

**State Machine (Consumption):**
- Purpose: Enforce valid state transitions for onboarding workflow
- File: `domain/consumption.clj`
- States: `:consumer-information` → `:linky-reference` → `:billing-address` → `:contract-signature` → `:pending` → `:active` (or `:abandoned`)
- Guards: `assert-lifecycle` throws if transition invalid
- Transactions: 3-arity `save!/3` uses optimistic locking (original + updated)

**Guard Functions:**
- Purpose: Precondition checks that throw `ex-info` on violation
- Pattern: Prefix `assert-*` (e.g., `assert-lifecycle`, `assert-email-verified`)
- Scope: Used in domain and application layers only, not in handlers

## Entry Points

**Backend:**
- Location: `backend/src/main.clj`
- Triggers: `java -M -m main` (REPL-friendly class)
- Responsibilities: Calls `system/start` to initialize Integrant components

**Frontend:**
- Location: `frontend/src/app/core.cljs`
- Triggers: Shadow-cljs build, loaded by `public/index.html`
- Responsibilities:
  - Dispatches `:app/initialize` event
  - Restores auth session from localStorage
  - Initializes router and mounts React root to `#app` div

**HTTP Routes:**
- Entry: `infrastructure/rest_api/handler.clj:build-router`
- Auth: `POST /api/v1/auth/login`, `POST /api/v1/auth/register`
- Networks: `GET /api/v1/networks`, `POST /api/v1/networks/check-eligibility`
- Consumptions: CRUD + 4 step PUT endpoints
- Productions: CRUD + onboarding steps
- Admin: User/network/consumption management

## Error Handling

**Strategy:** Exceptions as control flow; error information passed in `ex-info` map

**Patterns:**

*Domain Layer (Throws ex-info):*
```clojure
(when-not (alive? user)
  (throw (ex-info "User account is not alive"
                  {:lifecycle (:user/lifecycle user)})))
```

*Application Layer (Logs and propagates):*
```clojure
(when-not (= user-id (:consumption/user-id c))
  (throw (ex-info "Consumption does not belong to user"
                  {:user-id user-id :consumption-id consumption-id})))
```

*Handler Layer (Maps to HTTP status):*
```clojure
(let [msg (.getMessage ex)]
  (cond
    (str/includes? msg "not found")      404
    (str/includes? msg "does not belong") 403
    (str/includes? msg "Concurrent")      409
    :else                                  400))
```

*Frontend (Events dispatch error state):*
```clojure
(rf/reg-event-db :auth/login-err
  (fn [db [_ response]]
    (assoc db :auth/error (get-in response [:response :error]))))
```

## Cross-Cutting Concerns

**Logging:**
- Framework: `mulog` (structured logging)
- Pattern: Publish events at scenario completion (e.g., `::consumption-created`)
- Config: Console + file (`log/app.log`) via `system.edn`

**Validation:**
- Boundary: HTTP handlers (peripheral) and domain construction (internal)
- Tool: Malli schemas for structure validation
- Custom predicates: Email format, non-blank strings, ID validation

**Authentication:**
- OAuth: Google, Apple, Facebook (verified at handler level via `token-verifier`)
- Email/Password: Registration with email verification token
- JWT: HS256 token issued on login, verified via middleware
- Storage: JWT in Authorization header, user object in localStorage (frontend)

**CORS:**
- Configured: `infrastructure/rest_api/handler.clj`
- Origins: Per-environment in `system.edn` (dev: `localhost:3449`, staging/prod: env var)

---

*Architecture analysis: 2026-03-15*
