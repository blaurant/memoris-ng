# Technology Stack

**Analysis Date:** 2026-03-15

## Languages

**Primary:**
- **Clojure** 1.12.0 - Backend business logic, REST API, domain models
- **ClojureScript** (via shadow-cljs) - Frontend SPA with React components

**Secondary:**
- **JavaScript** - Build tooling (Node ecosystem, shadow-cljs compiler)
- **HTML/CSS** - Frontend markup and styling (Nginx static assets)

## Runtime

**Environment:**
- **JVM** - Clojure backend requires Java/JRE for execution
- **Node.js** 20 - Frontend build tooling and shadow-cljs compilation

**Package Manager:**
- **Clojure CLI** (tools.deps) - Backend dependency management via `deps.edn`
- **npm** 8+ - Frontend dependency management via `package.json` and `package-lock.json`

## Frameworks

**Backend:**
- **Ring** 1.12.2 - HTTP request/response middleware and adapters
- **Reitit** 0.7.2 - HTTP routing and handler definition
- **Integrant** 0.11.0 - Component lifecycle and dependency injection
- **Aero** 1.1.6 - Environment-aware configuration (profiles: dev, staging, prod)
- **Malli** 0.16.4 - Data validation and schema definition
- **XTDB** 1.24.4 - Immutable database with RocksDB backend

**Frontend:**
- **Reagent** 1.2.0 - React component library (Form-3 components)
- **Re-frame** 1.3.0 - Centralized state management (app-db, subscriptions, events)
- **Reitit** 0.7.2 - SPA client-side routing
- **shadow-cljs** 2.28.14 - ClojureScript compiler with dev server and release builds

**Build & Development:**
- **Kaocha** 1.91.1392 - Test runner for backend (TDD-friendly)
- **clj-kondo** 2024.08.01 - Clojure linter
- **cljfmt** 0.12.0 - Clojure code formatter
- **Docker** - Multi-stage builds for containerized deployment

## Key Dependencies

**Authentication & Security:**
- **com.auth0/java-jwt** 4.4.0 - JWT signing and verification (HS256)
- **com.auth0/jwks-rsa** 0.22.1 - JWKS provider for OAuth token validation
- **buddy/buddy-hashers** 2.0.167 - Password hashing (bcrypt)

**Data & Serialization:**
- **com.brunobonacci/mulog** 0.9.0 - Structured logging with console and file publishers
- **org.clojure/data.json** 2.5.1 - JSON parsing and generation
- **metosin/muuntaja** 0.6.8 - Content negotiation (JSON, EDN, etc.)

**HTTP & APIs:**
- **clj-http** 3.13.0 - HTTP client for outbound API calls (Resend email API, OAuth verification)
- **ring-cors** 0.1.13 - CORS middleware for cross-origin requests

**Database & Persistence:**
- **com.xtdb/xtdb-core** 1.24.4 - XTDB immutable database engine
- **com.xtdb/xtdb-rocksdb** 1.24.4 - RocksDB storage backend for XTDB

**Utilities:**
- **danlentz/clj-uuid** 0.1.9 - UUID generation
- **tick** 0.7.5 - Time and date handling

## Configuration

**Environment Management:**
- Aero-based configuration in `backend/resources/system.edn` with EDN syntax
- Three profiles: `:dev`, `:staging`, `:prod`
- Environment variables injected at runtime via `#env` and `#profile` Aero readers
- No `.env` file committed; runtime variables provided via deployment platform secrets

**Key Environment Variables Required:**
- **OAuth:** `GOOGLE_CLIENT_ID`, `APPLE_CLIENT_ID`, `FACEBOOK_APP_ID`
- **Auth:** `JWT_SECRET` (HS256 signing key)
- **Email:** `RESEND_API_KEY`, `EMAIL_FROM` (staging/prod), `APP_BASE_URL` (email link redirects)
- **API:** `CORS_ORIGIN` (allowed frontend origins)
- **Database:** `data/xtdb` local directory (RocksDB storage)
- **Logging:** Dual publisher (console + file to `log/app.log`)

**Build-Time Configuration (Frontend):**
- Shadow-cljs closure-defines injected at compile time:
  - `app.config/API_BASE` - Backend API base URL
  - `app.config/GOOGLE_MAPS_API_KEY` - Google Maps JS API key
  - `app.config/APP_ENV` - "dev" | "staging" | "prod"
  - `app.config/GOOGLE_CLIENT_ID`, `app.config/FACEBOOK_APP_ID`, `app.config/APPLE_CLIENT_ID`

## Platform Requirements

**Development:**
- JDK 21+ (temurin distribution) for Clojure compilation and REPL
- Node.js 20+ for shadow-cljs compilation
- Clojure CLI (latest) for deps resolution
- Babashka (optional, for scripting)

**Production Deployment:**
- **Backend:** Docker image based on Eclipse Temurin 21 JRE (Alpine Linux)
  - Single uberjar artifact: `target/app.jar`
  - Exposed port: 8080
  - No APP_ENV default; deployment must provide it

- **Frontend:** Docker image based on nginx:1-alpine
  - Static assets served from `/usr/share/nginx/html`
  - Nginx config template with envsubst for PORT substitution
  - Exposed port: 8080

- **Hosting:** Railway platform (GraphQL API for deployments)
  - Staging: Approval required (1+ reviewer)
  - Production: Tag-based releases `v*`, approval required (2+ reviewers)
  - Container registry: GitHub Container Registry (ghcr.io)

---

*Stack analysis: 2026-03-15*
