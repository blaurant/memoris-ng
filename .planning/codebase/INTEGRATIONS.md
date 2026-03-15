# External Integrations

**Analysis Date:** 2026-03-15

## APIs & External Services

**OAuth 2.0 Providers:**
- **Google Identity Services** - User authentication via OAuth
  - SDK: Google Identity Services JS SDK (loaded at runtime)
  - Integration point: `frontend/src/app/auth/oauth.cljs:google-sign-in!`
  - Backend validation: `backend/src/infrastructure/auth/token_verifier.clj:verify-google-token`
  - JWKS endpoint: `https://www.googleapis.com/oauth2/v3/certs`
  - Auth: `GOOGLE_CLIENT_ID` (frontend + backend verification)
  - Caching: JWKS cached for 24 hours

- **Apple Sign-In** - User authentication via OAuth
  - SDK: Apple Sign-In JS SDK (loaded at runtime from `https://appleid.cdn-apple.com`)
  - Integration point: `frontend/src/app/auth/oauth.cljs:apple-sign-in!`
  - Backend validation: `backend/src/infrastructure/auth/token_verifier.clj:verify-apple-token`
  - JWKS endpoint: `https://appleid.apple.com/auth/keys`
  - Auth: `APPLE_CLIENT_ID` (frontend + backend verification)
  - Caching: JWKS cached for 24 hours

- **Facebook Login** - User authentication via OAuth
  - SDK: Facebook JS SDK (loaded at runtime from `https://connect.facebook.net/en_US/sdk.js` v18.0)
  - Integration point: `frontend/src/app/auth/oauth.cljs:facebook-sign-in!`
  - Backend validation: `backend/src/infrastructure/auth/token_verifier.clj:verify-facebook-token`
  - Debug endpoint: `https://graph.facebook.com/debug_token` (for token validation)
  - User data endpoint: `https://graph.facebook.com/me` (fields: id, name, email)
  - Auth: `FACEBOOK_APP_ID` (frontend + backend verification)

**Email Service:**
- **Resend** - Transactional email delivery
  - SDK: clj-http (bare HTTP POST requests)
  - Integration: `backend/src/infrastructure/email/resend_sender.clj`
  - API endpoint: `https://api.resend.com/emails` (POST)
  - Auth: `RESEND_API_KEY` (Bearer token in Authorization header)
  - Email operations:
    - Verification email (24-hour token link)
    - Welcome email (account activation)
    - Password reset email (24-hour token link)
  - From address: `RESEND_ONBOARDING_EMAIL` (dev), `EMAIL_FROM` (staging/prod)
  - Redirect base: `APP_BASE_URL` (for email verification/reset links)

**Maps & Geolocation:**
- **Google Maps JS API** - Interactive map display for network coverage areas
  - SDK: Google Maps JS API (loaded at runtime)
  - API key: `GOOGLE_MAPS_API_KEY`
  - Integration: `frontend/src/app/components/google_map.cljs:load-google-maps-script!`
  - Endpoint: `https://maps.googleapis.com/maps/api/js?key={API_KEY}&callback=initGoogleMap`
  - Usage: Renders circles on map for network center + radius
  - Geolocation: Network entities use lat/lng coordinates (`network/center-lat`, `network/center-lng`)

## Data Storage

**Primary Database:**
- **XTDB v1 (1.24.4)** - Immutable, bitemporal database
  - Engine: `com.xtdb/xtdb-core`
  - Storage backend: RocksDB (`com.xtdb/xtdb-rocksdb`)
  - Data directory: `data/xtdb/` (local filesystem, relative to app working directory)
  - Lifecycle: Managed via Integrant `:xtdb/node`
  - Repositories: Eight domain entity repos (User, Network, Consumption, Production, EligibilityCheck, AlertBanner, VerificationToken, all using same XTDB node)

**File Storage:**
- Local filesystem only - No cloud storage integration detected

**Caching:**
- **JWKS Provider Caching** - Auth0 JWKS cached in-memory for 24 hours (per provider)
  - Implementation: `com.auth0.jwk.JwkProviderBuilder` with 24h TTL

## Authentication & Identity

**Auth Provider:**
- **Custom OAuth + JWT** (hybrid approach)
  - OAuth: Delegates to Google/Apple/Facebook for initial user authentication
  - Backend processing: `backend/src/infrastructure/auth/token_verifier.clj` (OAuth token verification)
  - Session token: HS256 JWT (24-hour expiry)
  - Signing: `backend/src/infrastructure/auth/jwt.clj` (uses `JWT_SECRET`)
  - Token claims: subject (user ID), email, name, role (customer/admin)

**Password Management:**
- **bcrypt** via buddy-hashers 2.0.167
  - Hasher: `backend/src/infrastructure/auth/bcrypt_hasher.clj`
  - Used for password reset flows (not OAuth login)

## Monitoring & Observability

**Error Tracking:**
- Not configured in codebase (Railway platform may provide monitoring)

**Logging:**
- **mulog** 0.9.0 - Structured event logging
  - Configuration: `backend/resources/system.edn` (multi publisher)
  - Publishers:
    - Console publisher (stdout, real-time logs)
    - Simple file publisher (to `log/app.log`)
  - Integration points:
    - Auth events: `infrastructure.auth.token_verifier` logs verification success/failure
    - Email events: `infrastructure.email.resend_sender` logs send attempts with status
    - HTTP requests: `infrastructure.rest_api.logging` captures request/response details
  - Event namespace: Keyword-based (e.g., `::email-sent`, `::http-request`)

## CI/CD & Deployment

**Hosting:**
- **Railway Platform** - Managed hosting with GraphQL API for deployments
  - Staging & Production environments separate
  - Environment variables and build args configured in Railway console

**Container Registry:**
- **GitHub Container Registry (ghcr.io)**
  - Backend image: `ghcr.io/{org}/memoris-ng/backend:{tag}`
  - Frontend image: `ghcr.io/{org}/memoris-ng/frontend:{tag}-staging` or `{tag}`
  - Authentication: `GITHUB_TOKEN` (via GitHub Actions)

**CI Pipeline:**
- **GitHub Actions**
  - Trigger: Pull requests (CI), push to main + tags (build & deploy)
  - Jobs:
    - Backend: lint (clj-kondo), format check (cljfmt), tests (Kaocha)
    - Frontend: production build check (shadow-cljs release)
  - Build outputs:
    - Backend uberjar: `backend/target/app.jar`
    - Frontend static assets: `frontend/public/js/` + `frontend/public/index.html`
  - Deploy stages:
    - Dev: Auto-deploy on main push (approval not enforced)
    - Staging: Manual trigger + approval (1+ reviewer required)
    - Production: Tag-based (`v*` tags), approval required (2+ reviewers)

**Build Args & Environment Variables:**
- Backend:
  - Docker: `APP_ENV` (injected at startup)
  - Aero: profiles selected based on `APP_ENV`
- Frontend (injected at build time via shadow-cljs):
  - `API_BASE` - Backend API base URL (empty for prod, localhost:3000 for dev)
  - `GOOGLE_MAPS_API_KEY` - Maps API key (from secrets)
  - `GOOGLE_CLIENT_ID` - OAuth client ID (from secrets)
  - `APP_ENV` - Environment name (dev/staging/prod)
  - `SITE_URL` - For OG image URL injection in index.html

## Environment Configuration

**Required Environment Variables:**

*Authentication:*
- `JWT_SECRET` - HMAC256 key for JWT signing (must be non-empty)
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `APPLE_CLIENT_ID` - Apple OAuth client ID
- `FACEBOOK_APP_ID` - Facebook OAuth app ID

*Email:*
- `RESEND_API_KEY` - Resend API key for email delivery
- `EMAIL_FROM` - From email address (staging/prod only, dev uses onboarding@resend.dev)
- `APP_BASE_URL` - Base URL for email verification/reset links

*API:*
- `CORS_ORIGIN` - Allowed frontend origin(s) for CORS policy (staging/prod)

*Optional:*
- `GOOGLE_MAPS_API_KEY` - Google Maps JS API key (if empty, map warnings logged)

**Secrets Location:**
- GitHub Actions: Stored in repository secrets and environment variables
- Railway: Environment secrets for staging/prod
- Development: `.env` file (not committed; create locally)

## Webhooks & Callbacks

**Incoming:**
- None detected

**Outgoing:**
- **Email verification callback:** Resend sends emails with embedded link to `{APP_BASE_URL}/verify-email?token={token}`
- **Password reset callback:** Resend sends emails with embedded link to `{APP_BASE_URL}/reset-password?token={token}`
- **OAuth callbacks:** Frontend redirects handled by OAuth provider SDKs (no explicit webhook, uses PKCE flow)

## Deployment Configuration

**Docker Multi-Stage Build (Backend):**
- Build stage: Clojure 21 tools-deps image, compiles to uberjar
- Runtime stage: Eclipse Temurin 21 JRE Alpine, runs compiled JAR
- Port: 8080
- Entrypoint: `java -jar app.jar`

**Docker Build (Frontend):**
- Build stage: Node 20 + OpenJDK 17 JRE, compiles shadow-cljs release build
- Build args: `API_BASE`, `GOOGLE_MAPS_API_KEY`, `APP_ENV`, `SITE_URL`, `GOOGLE_CLIENT_ID`
- OG image URL injection: Modifies index.html og:image content attribute if SITE_URL provided
- Runtime stage: nginx:1-alpine, serves static assets
- Port: 8080
- Nginx config: Uses envsubst to inject PORT environment variable

---

*Integration audit: 2026-03-15*
