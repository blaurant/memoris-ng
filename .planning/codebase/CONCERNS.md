# Codebase Concerns

**Analysis Date:** 2026-03-15

## Tech Debt

**Admin email hardcoding in auth logic:**
- Issue: Admin promotion based on hardcoded email list in `backend/src/application/auth_scenarios.clj` (lines 12-13)
- Files: `backend/src/application/auth_scenarios.clj`
- Impact: Adding or removing admins requires code changes and redeployment. Cannot manage roles dynamically via admin panel.
- Fix approach: Move admin email list to configuration (environment variable or database table), implement role management in admin panel, remove hardcoded promotion logic.

**Email sending error handling is silent:**
- Issue: Email send failures in `backend/src/infrastructure/email/resend_sender.clj` are logged but swallowed for password reset (line 60). Exception thrown for verification but not for reset emails.
- Files: `backend/src/infrastructure/email/resend_sender.clj` (lines 41-61)
- Impact: Users won't know if critical emails (password resets) failed. No retry mechanism.
- Fix approach: Implement consistent error handling across all email functions. Add email delivery confirmation tracking. Implement queue with retries for failed sends.

**Password validation is weak:**
- Issue: Password minimum length only 8 characters in `backend/src/domain/user.clj` (line 21)
- Files: `backend/src/domain/user.clj`
- Impact: Doesn't enforce complexity requirements (uppercase, digits, special chars). Vulnerable to brute force.
- Fix approach: Add password complexity validation (uppercase, lowercase, digits, special chars). Consider bcrypt work factor tuning.

**Error status mapping via string matching is fragile:**
- Issue: HTTP status codes determined by `.getMessage()` string matching in `backend/src/infrastructure/rest_api/consumption_handler.clj` (lines 22-30)
- Files: `backend/src/infrastructure/rest_api/consumption_handler.clj`, `backend/src/infrastructure/rest_api/auth_handler.clj` (lines 32-35)
- Impact: Changing error message text breaks status code mapping. No type-safe error handling.
- Fix approach: Create typed exception hierarchy (e.g., `NotFound`, `Forbidden`, `Conflict`) with status codes embedded, use `:status` key in ex-data instead of string matching.

## Security Considerations

**JWT token expiration fixed at 24 hours:**
- Risk: Long-lived tokens increase exposure if compromised. No refresh token mechanism.
- Files: `backend/src/infrastructure/auth/jwt.clj` (line 12)
- Current mitigation: Fixed expiration enforced
- Recommendations: Implement refresh token endpoint. Add token rotation on login. Support shorter token lifetimes (1 hour). Log token issuance/invalidation.

**OAuth provider credentials validation:**
- Risk: If OAuth provider tokens are invalid/expired, no graceful fallback. Script injection possible if provider SDKs fail to load.
- Files: `frontend/src/app/auth/oauth.cljs` (lines 36-41, 94-96)
- Current mitigation: Warning logged to console, but user sees no error message
- Recommendations: Add visible error UI when OAuth providers fail to load. Implement fallback to email/password auth. Add timeout for SDK loading.

**Admin role hardcoding in application layer:**
- Risk: Hardcoded admin emails mean privilege escalation requires code change. No audit trail for admin promotion.
- Files: `backend/src/application/auth_scenarios.clj` (line 19-22)
- Current mitigation: Only during login, hardcoded list
- Recommendations: Implement admin management endpoint. Add admin promotion/demotion audit log. Require explicit admin approval process.

**Missing CSRF protection:**
- Risk: No CSRF token validation on state-changing requests (POST/PUT/DELETE)
- Files: All REST API handlers in `backend/src/infrastructure/rest_api/`
- Current mitigation: Relies on SameSite cookie policy and CORS
- Recommendations: Add CSRF token generation and validation. Include token in request body for all mutations.

**Plaintext error messages exposed in APIs:**
- Risk: Stack traces and detailed error messages leak implementation details
- Files: Error handling in `backend/src/infrastructure/rest_api/consumption_handler.clj`, `backend/src/infrastructure/rest_api/auth_handler.clj`
- Current mitigation: Uses `.getMessage()` from exceptions
- Recommendations: Map exceptions to generic error messages in production. Log full details server-side only. Never expose ex-data to clients.

**No rate limiting on auth endpoints:**
- Risk: Brute force attacks on login/password reset, email enumeration
- Files: `backend/src/infrastructure/rest_api/auth_handler.clj`
- Current mitigation: None
- Recommendations: Implement rate limiting per IP and email address. Add exponential backoff. Monitor failed login attempts.

## Known Issues

**Frontend-backend token sync issues:**
- Symptoms: User token in localStorage can become stale. No mechanism to refresh if JWT expires during session.
- Files: `frontend/src/app/auth/events.cljs` (lines 25-26), `frontend/src/app/core.cljs`
- Trigger: User stays on page > 24 hours, API calls start failing with 401
- Workaround: Manual page refresh required to trigger re-authentication
- Fix: Implement automatic token refresh using refresh token endpoint. Add interceptor to retry 401 responses.

**Concurrent modification detection incomplete:**
- Symptoms: Optimistic locking only works in consumption scenarios, not all entities
- Files: `backend/src/infrastructure/xtdb/xtdb_consumption_repo.clj` (lines 40-48), but `xtdb_user_repo.clj` and `xtdb_network_repo.clj` don't implement 3-arity save
- Trigger: Multiple admin users editing same network concurrently
- Workaround: Reload entity before updating
- Fix: Implement 3-arity save with optimistic locking in all XTDB repos. Add conflict resolution UI.

**Email service dependency on Resend API:**
- Symptoms: All email sending blocked if Resend is down or API key invalid
- Files: `backend/src/infrastructure/email/resend_sender.clj`
- Trigger: Resend service outage or misconfigured API key
- Workaround: Use console sender in development (catches issues late)
- Fix: Implement fallback email provider. Add email queue with retry logic. Implement health check for email service.

## Performance Bottlenecks

**Frontend state management bloat:**
- Problem: Single re-frame app-db growing with all admin, consumptions, productions, networks in memory
- Files: `frontend/src/app/db.cljs`, `frontend/src/app/events.cljs`
- Cause: All data fetched and cached in app-db without pagination or lazy loading
- Improvement path: Implement pagination for lists. Use persistent HTTP cache headers. Add entity normalization (selector pattern).

**No pagination on list endpoints:**
- Problem: Fetching all networks, consumptions, productions returns full list
- Files: `backend/src/infrastructure/rest_api/network_handler.clj`, `backend/src/infrastructure/rest_api/consumption_handler.clj`
- Cause: Simple find-all queries without limit/offset
- Improvement path: Add limit/offset query parameters. Implement keyset pagination. Add total count header.

**Frontend file size concerns:**
- Problem: `frontend/src/app/pages/admin.cljs` is 502 lines, `production_onboarding_form.cljs` is 463 lines
- Files: Large component files in `frontend/src/app/components/` and `frontend/src/app/pages/`
- Cause: Component logic and UI mixed, no composition
- Improvement path: Break large components into smaller, reusable pieces. Separate business logic from UI. Use component composition.

**XTDB query performance unknown:**
- Problem: No query indexes defined, XTDB doing full scans for common queries
- Files: `backend/src/infrastructure/xtdb/` repo files
- Cause: Only using `:where` clauses, no explicit index configuration
- Improvement path: Add XTDB indexes for common query patterns (user-id, network-id, email). Monitor query execution time. Add query caching for read-heavy endpoints.

## Fragile Areas

**Domain entity validation complexity:**
- Files: `backend/src/domain/consumption.clj`, `backend/src/domain/production.clj`
- Why fragile: State machine with 8+ states, multi-step transitions with complex Malli schemas. Adding new states requires updates in multiple functions.
- Safe modification: Extend state enum, add new schema segments, add new transition functions. Always test state transitions in isolation.
- Test coverage: Consumption state machine has good test coverage in `backend/test/domain/consumption_test.clj`. Production has minimal tests.

**OAuth provider script injection risk:**
- Files: `frontend/src/app/auth/oauth.cljs` (lines 7-17)
- Why fragile: Dynamic script injection via `createElement` and `setAttribute` without integrity checks
- Safe modification: Use SRI (Subresource Integrity) hashes for external scripts. Validate script contents. Consider using official SDKs via npm instead.
- Test coverage: No automated tests for OAuth flows. Manual testing only.

**Error handling via exception messages:**
- Files: All handlers in `backend/src/infrastructure/rest_api/`
- Why fragile: String matching on exception messages to determine HTTP status. Renaming exceptions breaks code.
- Safe modification: Create typed exception hierarchy. Update all catch blocks. Add tests for error code mapping.
- Test coverage: Limited error path testing.

**Frontend localStorage without versioning:**
- Files: `frontend/src/app/auth/events.cljs` (lines 25-26)
- Why fragile: Storing auth-token and auth-user as JSON without schema versioning. Breaking changes in token format cause parsing errors.
- Safe modification: Add version key to localStorage. Implement migration logic. Add null checks for missing keys.
- Test coverage: No tests for storage persistence or recovery.

## Test Coverage Gaps

**Backend REST API handlers lack integration tests:**
- What's not tested: HTTP request/response cycle, error responses, validation error formatting, CORS headers
- Files: `backend/src/infrastructure/rest_api/` (auth_handler.clj, consumption_handler.clj, etc.)
- Risk: Typos in error messages, missing status codes, incorrect serialization only caught in manual testing
- Priority: High - These are critical integration points

**Frontend OAuth flow untested:**
- What's not tested: Script loading, provider callback handling, token extraction, re-dispatch flow
- Files: `frontend/src/app/auth/oauth.cljs`
- Risk: OAuth failures only discovered in production. Silent failures if script doesn't load.
- Priority: High - Critical auth flow

**Email delivery not tested:**
- What's not tested: Resend API integration, actual email sending, template rendering, error handling
- Files: `backend/src/infrastructure/email/resend_sender.clj`
- Risk: Email templates may have syntax errors, API responses may break parsing
- Priority: Medium - Can catch with integration tests

**Frontend loading states and transitions:**
- What's not tested: Async request sequencing, concurrent request handling, race conditions between dispatch and response
- Files: `frontend/src/app/auth/events.cljs`, `frontend/src/app/events.cljs`, `frontend/src/app/admin/events.cljs`
- Risk: Duplicate submissions, stale responses processed out of order, loading states stuck
- Priority: Medium - Logic is complex but not verified

**XTDB transaction handling:**
- What's not tested: Concurrent modifications, transaction failures, node shutdown scenarios
- Files: `backend/src/infrastructure/xtdb/` repo implementations
- Risk: Data corruption or lost writes under concurrent load
- Priority: Medium - Data integrity critical

## Scaling Limits

**XTDB RocksDB on single machine:**
- Current capacity: ~1M entities before performance degrades (depends on entity size)
- Limit: RocksDB is embedded, not distributed. Single machine storage I/O becomes bottleneck.
- Scaling path: Migrate to XTDB Cloud or PostgreSQL. Implement sharding by network-id if needed.

**In-memory repositories for testing:**
- Current capacity: Works for small datasets
- Limit: Used in production for user-repo in some configs (needs verification)
- Scaling path: Ensure all repos use XTDB in production. Remove in-memory implementations or mark as dev-only.

**Frontend app-db holding all user data:**
- Current capacity: ~10K items before performance issues
- Limit: Everything stored in JavaScript objects, re-renders on any dispatch
- Scaling path: Implement selector memoization (re-reselect). Use normalization. Lazy-load lists.

**Single XTDB node per deployment:**
- Current capacity: ~TBD - depends on RocksDB disk and memory
- Limit: No read replicas, no cluster. If node goes down, writes blocked.
- Scaling path: XTDB Cloud provides managed cluster. Add read-only replicas.

## Dependencies at Risk

**mulog for logging:**
- Risk: Custom logging library with limited ecosystem. If maintenance stops, hard to migrate.
- Impact: All server-side observability depends on mulog. No replacement without rewrite.
- Migration plan: Evaluate Log4j2 or Logback as alternative. Add structured logging wrapper layer.

**Resend for email:**
- Risk: Single email provider. API changes, pricing changes, outages affect all users.
- Impact: All password resets, verification emails blocked if Resend down.
- Migration plan: Implement email sender abstraction (already have protocol). Add mailgun, SendGrid as alternatives.

**re-frame for state management:**
- Risk: Reasonable choice but growing complexity. No type safety for events/subs.
- Impact: Easy to introduce bugs in event handlers. Hard to refactor event payloads.
- Migration plan: Consider migrating to Reframe v2 (when stable) or f/fx if better type support needed.

**XTDB v1 lock-in:**
- Risk: Pre-release database. V2 has breaking changes. Migration path unclear.
- Impact: Data schema and query language tied to XTDB v1 semantics.
- Migration plan: Monitor XTDB v2 release. Plan for data export if v1 no longer maintained.

## Missing Critical Features

**No audit logging for sensitive operations:**
- Problem: No record of who changed what when for admin actions, data modifications, auth events
- Blocks: Compliance requirements (GDPR, audit trails), debugging user issues, detecting unauthorized access
- Recommendation: Add audit log table. Log all mutations with user-id, timestamp, before/after values.

**No backup/restore mechanism:**
- Problem: XTDB data is critical. No visible backup strategy.
- Blocks: Disaster recovery. Data loss unrecoverable.
- Recommendation: Implement daily XTDB snapshots. Test restore process. Document recovery procedure.

**No data retention/deletion policy:**
- Problem: Users can delete account but personal data remains in system
- Blocks: GDPR right to be forgotten. User privacy.
- Recommendation: Implement soft delete. Add retention policy configuration. Implement data anonymization/export.

**No change notification system:**
- Problem: Users can't be notified of important events (contract updates, consumption changes)
- Blocks: User experience (manual checking required). Business workflows (notifications to producers).
- Recommendation: Add notification service. Implement email and in-app notification. Add notification preferences UI.

---

*Concerns audit: 2026-03-15*
