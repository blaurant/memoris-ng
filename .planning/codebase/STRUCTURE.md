# Codebase Structure

**Analysis Date:** 2026-03-15

## Directory Layout

```
memoris-ng/
├── backend/                          # Clojure backend application
│   ├── src/
│   │   ├── main.clj                 # Entry point
│   │   ├── system.clj               # Integrant config loader
│   │   ├── domain/                  # Pure business logic (entities, protocols)
│   │   ├── application/             # Orchestration layer (scenarios)
│   │   └── infrastructure/          # External system integration
│   │       ├── rest_api/            # HTTP API (Reitit handlers)
│   │       ├── xtdb/                # Persistence (XTDB repositories)
│   │       ├── auth/                # Authentication (JWT, OAuth, Bcrypt)
│   │       ├── email/               # Email sending (Resend, Console)
│   │       └── in_memory_repo/      # Testing repositories
│   ├── resources/
│   │   └── system.edn               # Integrant system configuration
│   ├── test/
│   │   ├── domain/                  # Unit tests for entities
│   │   ├── application/             # Integration tests for scenarios
│   │   ├── bdd/                     # BDD framework and helpers
│   │   └── system_test.clj          # System-level tests
│   ├── data/                        # XTDB RocksDB storage (generated)
│   └── log/                         # Log files (generated)
│
├── frontend/                         # ClojureScript frontend application
│   ├── src/app/
│   │   ├── core.cljs                # Bootstrap and mount
│   │   ├── db.cljs                  # Initial app-db state
│   │   ├── routes.cljs              # Reitit routing config
│   │   ├── views.cljs               # Root component
│   │   ├── events.cljs              # Global events
│   │   ├── subs.cljs                # Global subscriptions
│   │   ├── config.cljs              # Closure defines (API_BASE, keys)
│   │   ├── auth/                    # Auth events, subs, OAuth
│   │   ├── consumptions/            # Consumption events and subs
│   │   ├── productions/             # Production events and subs
│   │   ├── admin/                   # Admin events and subs
│   │   ├── pages/                   # Page components (home, login, portal, etc)
│   │   └── components/              # Shared UI components (forms, maps, blocks)
│   ├── public/                      # Static assets (CSS, images, index.html)
│   ├── shadow-cljs.edn              # Shadow-cljs build configuration
│   └── package.json                 # NPM dependencies
│
└── .planning/codebase/              # GSD mapping documents (this directory)
```

## Directory Purposes

**backend/src/domain/**
- Purpose: Business logic isolated from external concerns
- Contains: Entity schemas (Malli), state machines, protocols, pure functions
- Key files: `user.clj`, `consumption.clj`, `network.clj`, `production.clj`, `eligibility_check.clj`

**backend/src/application/**
- Purpose: Use case orchestration, workflows, event publishing
- Contains: Scenario functions that compose domain operations
- Key files: `consumption_scenarios.clj`, `auth_scenarios.clj`, `user_scenarios.clj`, `network_scenarios.clj`

**backend/src/infrastructure/rest_api/**
- Purpose: HTTP API endpoints and request/response handling
- Contains: Reitit route definitions, auth middleware, request parsing, error mapping
- Key files: `handler.clj` (main router), `consumption_handler.clj`, `auth_handler.clj`, `network_handler.clj`

**backend/src/infrastructure/xtdb/**
- Purpose: Persistence layer backed by XTDB (embedded database)
- Contains: Repository implementations matching domain protocols
- Pattern: Each repository reads/writes entities with XTDB queries and transactions
- Key files: `xtdb_user_repo.clj`, `xtdb_consumption_repo.clj`, `xtdb_network_repo.clj`, `node.clj` (XTDB initialization)

**backend/src/infrastructure/auth/**
- Purpose: Authentication and authorization
- Contains: JWT token generation/verification, OAuth token verification, password hashing
- Key files: `jwt.clj`, `token_verifier.clj`, `bcrypt_hasher.clj`

**backend/resources/**
- Purpose: Configuration files loaded at runtime
- Key files: `system.edn` (Integrant system definition with Aero profile support)

**backend/test/domain/**
- Purpose: Unit tests for domain entities and state machines
- Pattern: Tests validate schemas, state transitions, guards
- Key files: `user_test.clj`, `consumption_test.clj`, `network_test.clj`

**backend/test/application/**
- Purpose: Integration tests for scenarios with mocked repositories
- Pattern: GIVEN-WHEN-THEN BDD scenarios using `defscenario` macro
- Key files: `consumption_scenarios_test.clj`, `auth_scenarios_test.clj`

**frontend/src/app/auth/**
- Purpose: Authentication state and events
- Contains: OAuth login/register, email/password auth, session restore
- Key files: `events.cljs` (auth dispatch handlers), `subs.cljs` (derived auth state), `oauth.cljs` (OAuth flow)

**frontend/src/app/consumptions/**
- Purpose: Consumption onboarding state management
- Contains: Events for listing, creating, updating consumption steps
- Key files: `events.cljs`, `subs.cljs`

**frontend/src/app/pages/**
- Purpose: Full-page components (route targets)
- Contains: Home, Login, Signup, Portal, Admin pages
- Key files: `home.cljs`, `login.cljs`, `consumptions.cljs`, `productions.cljs`, `admin.cljs`

**frontend/src/app/components/**
- Purpose: Reusable UI components
- Contains: Forms (eligibility, onboarding), blocks (consumption, production), maps, auth buttons
- Key files: `onboarding_form.cljs`, `consumption_block.cljs`, `google_map.cljs`, `eligibility_form.cljs`

## Key File Locations

**Entry Points:**
- `backend/src/main.clj`: Backend entry point (starts Integrant system)
- `frontend/src/app/core.cljs`: Frontend entry point (initializes Re-frame and mounts app)

**Configuration:**
- `backend/resources/system.edn`: Integrant component definitions and Aero profile config
- `frontend/shadow-cljs.edn`: Shadow-cljs build and Closure defines
- `backend/src/system.clj`: System initialization logic (reads config, resolves profiles)

**Core Logic:**
- `backend/src/domain/consumption.clj`: Consumption state machine and validation
- `backend/src/domain/user.clj`: User lifecycle and authentication states
- `backend/src/domain/network.clj`: Network entity with geo queries
- `backend/src/application/consumption_scenarios.clj`: Consumption workflows

**HTTP API:**
- `backend/src/infrastructure/rest_api/handler.clj`: Reitit router definition
- `backend/src/infrastructure/rest_api/consumption_handler.clj`: Consumption endpoints
- `backend/src/infrastructure/rest_api/auth_handler.clj`: Auth endpoints

**Persistence:**
- `backend/src/infrastructure/xtdb/node.clj`: XTDB initialization (RocksDB config)
- `backend/src/infrastructure/xtdb/xtdb_user_repo.clj`: User persistence
- `backend/src/infrastructure/xtdb/xtdb_consumption_repo.clj`: Consumption persistence

**Testing:**
- `backend/test/bdd/test_gwt.clj`: BDD framework (GIVEN-WHEN-THEN macros)
- `backend/test/domain/consumption_test.clj`: Consumption domain tests
- `backend/test/application/consumption_scenarios_test.clj`: Scenario tests

## Naming Conventions

**Files:**
- Domain entities: `domain/[entity].clj` (e.g., `domain/user.clj`)
- Scenarios: `application/[entity]_scenarios.clj` (e.g., `application/consumption_scenarios.clj`)
- HTTP handlers: `rest_api/[entity]_handler.clj` (e.g., `rest_api/consumption_handler.clj`)
- Repositories: `xtdb/xtdb_[entity]_repo.clj` or `in_memory_repo/mem_[entity]_repo.clj`
- Test files: Mirror source structure with `_test.clj` suffix

**Functions:**
- Guards: Prefix `assert-*` (e.g., `assert-lifecycle`, `assert-email-verified`)
- Queries: Descriptive predicates (e.g., `onboarding?`, `alive?`, `contains-point?`)
- Transitions: Verb + object (e.g., `register-consumer-information`, `sign-contract`)
- Handlers: Suffix `-handler` (e.g., `create-consumption-handler`)

**Variables:**
- Consumption: `c`, `c'` (original and updated)
- User: `u`, `u'`
- Request: `request`
- Database: `db`
- App-db subscriptions: `(rf/subscribe [keyword])` returns ratom

**Types/Keywords:**
- Entity keys: Namespace + name (e.g., `:consumption/id`, `:user/email`)
- Lifecycle states: Kebab-case (e.g., `:consumer-information`, `:contract-signature`)
- Event names: Slash-separated (e.g., `:auth/login-ok`, `:consumption-created`)
- Page names: Keyword (e.g., `:page/home`, `:page/portal`)

## Where to Add New Code

**New Feature (Use Case):**
1. **Domain logic:** Add entity file or extend existing (e.g., `backend/src/domain/new_entity.clj`)
   - Define schema with Malli
   - Implement state transitions and guards
   - Define repository protocol

2. **Application logic:** Add scenario file (e.g., `backend/src/application/new_entity_scenarios.clj`)
   - Implement use case functions orchestrating domain operations
   - Log events with mulog

3. **Persistence:** Implement repository in `backend/src/infrastructure/xtdb/xtdb_new_entity_repo.clj`
   - Follow existing repo pattern (see `xtdb_user_repo.clj`)
   - Implement all protocol methods

4. **HTTP API:** Add handler file (e.g., `backend/src/infrastructure/rest_api/new_entity_handler.clj`)
   - Define handlers extracting request parameters
   - Call scenarios with parameters
   - Catch exceptions and map to HTTP status codes
   - Export routes function

5. **Update router:** Add routes to `infrastructure/rest_api/handler.clj:build-router`

6. **Update config:** Add component definition to `backend/resources/system.edn` if needed

7. **Tests:** Add test files mirroring source structure
   - Domain tests: `backend/test/domain/new_entity_test.clj`
   - Scenario tests: `backend/test/application/new_entity_scenarios_test.clj`

**Frontend Page:**
1. Create page component: `frontend/src/app/pages/[page_name].cljs`
2. Add route to `frontend/src/app/routes.cljs`
3. Add event handler in module (or global) events file
4. Add subscription in module subs file
5. Dispatch navigation event `:router/navigated` with page keyword

**Frontend Component:**
1. Create in `frontend/src/app/components/[component_name].cljs`
2. Use Re-frame subscriptions to read state
3. Dispatch events on user interaction
4. Return Reagent hiccup vector

**New Re-frame Module:**
1. Create directory: `frontend/src/app/[domain]/`
2. Create files: `events.cljs`, `subs.cljs`
3. Register events with `rf/reg-event-*`
4. Register subscriptions with `rf/reg-sub`
5. Import module in `frontend/src/app/core.cljs` to load registrations

## Special Directories

**backend/data/**
- Purpose: XTDB RocksDB storage
- Generated: Yes (created by XTDB at runtime)
- Committed: No (in `.gitignore`)

**backend/log/**
- Purpose: Application logs
- Generated: Yes (created when logging writes to file)
- Committed: No (in `.gitignore`)

**frontend/public/js/**
- Purpose: Compiled ClojureScript JavaScript
- Generated: Yes (compiled by Shadow-cljs)
- Committed: No (in `.gitignore`)

**backend/.cpcache/**
- Purpose: Clojure deps cache
- Generated: Yes (created by `clj` command)
- Committed: No (in `.gitignore`)

**frontend/.shadow-cljs/**
- Purpose: Shadow-cljs analysis and dev build artifacts
- Generated: Yes (created during development)
- Committed: No (in `.gitignore`)

---

*Structure analysis: 2026-03-15*
