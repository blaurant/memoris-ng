
# Project Backend

These are instruction for backend project for a clojure/clojurescript app.

## General Guidelines

- when creating code for the backend : use a TDD process : First, write failing tests that cover every acceptance criterion. Then implement the code to make each test pass one at a time. After each implementation change, run tests and if any test fails, analyze the output and fix the code. Do not stop until all tests pass. Commit after each passing test.

- ONLY write code in clojure
- ONLY write scripts in clojure Babashka
- USE clojure idiomatic way to write code

## General Code Conventions
- NEVER create another top-level namespace for top-level functions — `main.clj`, `system.clj` and similar files live at the root of `src/`
- Use docString on every public function, use private function otherwise.
- fait attention aux parenthèses, quand il y a beaucoup, tu as tendance à te tromper.

## Workflow Rules
- Always run all tests after any code change before reporting success.
- When renaming namespaces or files, update ALL requires/imports across the entire project before proceeding.

## The 3 layer Architecture : domain, scenarios, infrastructure

- Domain : Pure business logic, independent of any other layer.
- Application: Use cases and orchestration, depends only on layer domain.
- Infrastructure : Connectivity to the external world, depends on domain and application.
When in doubt, ask which layer before creating files.

### Domain
- Domain is located in `domain/`, there is no subdirectories in domain
- name files according to the purpose and intent of the code (for user call it user.clj, not entities)
- Domain has NO external dependencies to application and infrastructure
- In the domain, put only : Schema definitions for domain structures, Pur functions on domain structures for checking schema conformance, Pur function on domain structures and protocol for domain services.
- entities are implemented with map and equality is based on their id attribute, copy id.clj in the domain for id purpose (from .claude/domain/id.clj) use domain.id, do not use uuid.
- id (type uuid4) are never generated in domain or application or infrastructure. Id are always created in the frontend. So all entity builder should have id in parameters.
- Value-objets equality is based on their attributes
- copy datetime.clj in the domain for date purpose (from .claude/domain/datetime.clj)
- for persistance, use a [entity]-repo protocol and implements it with xtdb2 in the infrastructure
- Domain aggregate lifecycle : Domain functions own the full lifecycle of their aggregate: state transitions, invariant enforcement, and if needed calls to domain services (exemple : a title generation when message addition in a conversation) must be encapsulated inside domain functions — never in the application layer.

#### Domain services
- A domain service (in the DDD sense) is a protocol declared in the domain that defines a contract. It takes and returns domain objects. The concrete implementation can lives in the domain or in the infrastructure if its need to call an external service.
- Never use the name 'domain service port', but simply 'domain service'.
- If Implementation need an external service, Implements the domain services with a protocol, and inject infrastructure implementation with Integrant.

#### Testing the domain layer
Unit tests for the domain are mandatory for every functions.

#### code convention to follow in domain
- For creating entities or value-objects, use the function name build-<entity> (e.g., make-user) and schema validation, exemple :
```clojure
(require '[malli.core :as m]
         [domain.id :refer :all])

(def User
  [:map
   [:id id?]
   [:email string?]
   [:name string?]])

(defn build-user
  [attrs]
  (if (m/validate User attrs)
    attrs
    (throw (ex-info "Invalid user" {:attrs attrs}))))
```

- when a function is about a transition from one state to another, use a name with a reference to the intention of the function, do      
not use name like "submit-*".

In the entity definitions (malli specifications), the definitions should be split according to each state. These definitions should then be used for each state transition function.

### Application
- Application layer contains scenarios (or `uses-cases`) and are located in `application/`. - All use-case must be located in application -
- It contains use-case function : with high level steps, calling domain function. Every use-case function must be transactional.
- All use-case function must take domain value-object as parameter (aka a map built with build-XXX) and return nothing, or a domain Value-object or a domain entity. Never send to a use-case function in application layer with parameters that has not been build with a domain builder function.
- after every use-cases if their is a save, send a domain event with the modified data and use the use-case name expressed in the past for the event name (exemple : 'create-user', event='user-created')
- IDs should not be created in the application layer use-cases methods, but IDs are generated in the frontend - and validated in the gateways or adapters (these are id/id?).
- Organize scenarios by domain subject (e.g., `user_scenarios.clj`, `contract_scenarios.clj`), never by user role (e.g., no `admin_scenarios.clj`). Authorization checks (like admin guards) belong inside each scenario function. Pass the `user-id` to the scenario and let it resolve the role from the repository — never pass a pre-resolved role from the infrastructure layer.


#### Testing the application layer
- This  layer of test is functional testing : GIVEN a state, WHEN a use-case, THEN a new state
- If entities have life-cycle, they must be one test per state transition.
- Tests with given/when/then are mandatoty for every use-case.
- in tests, when creating Value object or entities before calling a domain builder, use id/build-id. Never use (java.util.UUID/randomUUID)

### Infrastructure
Infrastructure is located in `infrastructure/` and is organised in technical silo and contains gateways and adaptors :
- A Gateway is the door (input or output) to the external world
- An adaptor convert (if necessary) data coming from external to domain data and domain data to external world.
- Every sub-directory contains logic for input or output (or both) to an external system (database, web, cli, etc.)
- They do not share code between sub-dir, no dependencies between Silo.
- In repositories implementations, all methods that return entities (find*) MUST use the domain's build_entity.
- Use namespace like `infrastructure.antropic.haiku-gateway` for gateway and `infrastructure.antropic.haiku-adaptor` for adaptor. For exemple :
- Repositories are grouped **by storage technology**, not by domain entity:
 `infrastructure/xtdb/` -> XTDB node + all XTDB-backed repos (`xtdb_<entity>_repo.clj`)
 `infrastructure/in_memory_repo/` -> All in-memory repos for tests/dev (`mem_<entity>_repo.clj`)
 `infrastructure/init_seed/` -> Seed data inserted on startup (`<entity>.clj`)
 `infrastructure/rest_api/` -> HTTP handlers, middleware, server
 `infrastructure/auth/` -> JWT, OAuth token verification, password hashing
 `infrastructure/email/` -> Email senders (Resend, console)
Naming conventions:
- XTDB repos: `xtdb_<entity>_repo.clj` (ns `infrastructure.xtdb.xtdb-<entity>-repo`)
- In-memory repos: `mem_<entity>_repo.clj` (ns `infrastructure.in-memory-repo.mem-<entity>-repo`)
- Never create a directory per domain entity (no `infrastructure/users/`, `infrastructure/consumptions/`, etc.)

#### Testing the Infrastructure layer

## Tools to use

- **deps.edn** for build configuration
- **malli** for Schema definitions (not spec)
- **com.brunobonacci/mulog** for logging into a `log/` directory, log to console also.
- **Aero** (`aero/aero`) to read EDN config with profiles and environment variables
- **Integrant** (`integrant/integrant`) for component lifecycle management
- **Ring** + **Reitit** + **Jetty**, (Ring 2.0) for websocket communication and rest interface API

## Tools to use for tests

- **testit** from metosin/testit
- **test-gwt** (copy from .claude/test/bdd/test-gwt) for GIVEN-WHEN-THEN tests.

## Configuration

### Config Structure
```
resources/system.edn    ; single config file, profiled via #profile
src/system.clj           ; system loading and startup
```

### Config Conventions

- Single `system.edn` file using `#profile {:dev ... :staging ... :prod ...}` for values that vary across environments.
- Secrets are **always** injected via `#env` (environment variables), never hardcoded.
- Active profile is determined by the `APP_ENV` variable. Defaults to `dev` locally, but **must be explicitly set** in deployed environments — the app fails fast if `APP_ENV` is missing and a production indicator is detected (e.g. `HOSTNAME`, container runtime).
- Each Integrant component is a namespaced key (`:db/pool`, `:http/server`, etc.).
- Inter-component dependencies use `#ig/ref`.

### Example system.edn

```clojure
{:db/pool
 {:host #profile {:dev "localhost"
                  :staging "db-staging.internal"
                  :prod #env DB_HOST}
  :port 5432
  :user #env DB_USER
  :password #env DB_PASSWORD}

 :http/server
 {:port #profile {:dev 3000 :staging 8080 :prod 8080}
  :db #ig/ref :db/pool}}
```

### Example system.clj

```clojure
(ns system
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [integrant.core :as ig]))

(defn read-config [profile]
  (aero/read-config (io/resource "system.edn") {:profile profile}))


(defn- deployed-env? [env]
  (some-> (System/getenv "HOSTNAME")
          (str/upper-case)
          (str/includes? env)))

(defn active-profile []
  (if-let [env (System/getenv "APP_ENV")]
    (keyword env)
    (if (deployed-env? "PROD")
      (throw (ex-info "APP_ENV must be set in deployed environments" {}))
      :dev)))

(defn start []
  (let [config (read-config (active-profile))]
    (ig/load-namespaces config)
    (ig/init config)))

(defn stop [system]
  (ig/halt! system))
```

## Running

```bash
# Dev (default)
clj -M -m main

# Staging
APP_ENV=staging clj -M -m main

# Prod
APP_ENV=prod DB_HOST=prod-db.internal DB_USER=app DB_PASSWORD=secret clj -M -m main
```

## Standards

### rest api standard
- All routes use `/api/vX` prefix. (X the version)
- JWT tokens expire after 24 hours

## Common Commands
