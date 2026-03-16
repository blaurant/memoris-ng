---
phase: 01-infrastructure
plan: 01
subsystem: api
tags: [clojure, reitit, xtdb, ddd, rest-api, security]

# Dependency graph
requires: []
provides:
  - "GET /api/v1/networks/:id/detail endpoint with aggregated network + productions + stats"
  - "find-by-network-id method on ProductionRepo protocol (XTDB + in-memory)"
  - "serialize-public-network and serialize-public-production whitelist functions"
  - "get-network-detail scenario in application layer"
affects: [01-02, frontend-network-detail-page]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Public serialization whitelist via select-keys to prevent sensitive data leaks"
    - "Scenario-level aggregation across multiple repos (network + production + consumption)"

key-files:
  created:
    - backend/test/application/network_scenarios_test.clj
  modified:
    - backend/src/domain/production.clj
    - backend/src/infrastructure/xtdb/xtdb_production_repo.clj
    - backend/src/infrastructure/in_memory_repo/mem_production_repo.clj
    - backend/src/application/network_scenarios.clj
    - backend/src/infrastructure/rest_api/network_handler.clj
    - backend/src/infrastructure/rest_api/handler.clj
    - backend/test/domain/production_test.clj

key-decisions:
  - "Use :public lifecycle as guard for network visibility (confirmed existing pattern)"
  - "Return 404 (not 403) for non-public networks to avoid information leakage"
  - "Energy-mix computed as percentage distribution, empty map when zero productions"

patterns-established:
  - "Public serialization whitelist: serialize-public-* functions using select-keys"
  - "Scenario aggregation: get-network-detail orchestrates 3 repos + computes derived stats"

requirements-completed: [INFR-01, INFR-03]

# Metrics
duration: 6min
completed: 2026-03-16
---

# Phase 1 Plan 01: Network Detail Backend API Summary

**Public endpoint GET /api/v1/networks/:id/detail with whitelist serialization, scenario aggregation, and 5 GWT test scenarios**

## Performance

- **Duration:** 6 min
- **Started:** 2026-03-16T17:24:12Z
- **Completed:** 2026-03-16T17:30:44Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- ProductionRepo protocol extended with find-by-network-id (XTDB + in-memory implementations)
- get-network-detail scenario aggregates network + active productions + consumer count + stats (total-capacity-kwc, energy-mix)
- Public endpoint returns 200 for public networks, 404 for non-public/missing, zero sensitive fields
- 5 GWT test scenarios covering: correct aggregation, private 404, missing 404, zero productions, no sensitive fields

## Task Commits

Each task was committed atomically:

1. **Task 1: Add find-by-network-id to ProductionRepo + implementations** - `31c9341` (feat)
2. **Task 2: Create get-network-detail scenario + handler + tests** - `cc95a0f` (feat)

## Files Created/Modified
- `backend/src/domain/production.clj` - Added find-by-network-id to ProductionRepo protocol
- `backend/src/infrastructure/xtdb/xtdb_production_repo.clj` - XTDB implementation of find-by-network-id
- `backend/src/infrastructure/in_memory_repo/mem_production_repo.clj` - In-memory implementation of find-by-network-id
- `backend/src/application/network_scenarios.clj` - get-network-detail scenario + serialization whitelists
- `backend/src/infrastructure/rest_api/network_handler.clj` - get-network-detail-handler + updated routes signature
- `backend/src/infrastructure/rest_api/handler.clj` - Wire production-repo + consumption-repo to network-handler
- `backend/test/domain/production_test.clj` - Tests for find-by-network-id
- `backend/test/application/network_scenarios_test.clj` - 5 GWT scenarios for get-network-detail

## Decisions Made
- Confirmed `:public` is the correct lifecycle state for "visible to prospects" (already used in eligibility check flow)
- Return 404 (not 403) for non-public networks to avoid leaking existence information
- Energy-mix is computed as percentage distribution (e.g., `{:solar 50.0 :wind 50.0}`), returns `{}` when zero active productions
- Total-capacity-kwc returns 0 (not nil) when zero active productions

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Backend API contract is established, ready for frontend route and Re-frame state wiring (Plan 01-02)
- All 70 backend tests pass (65 existing + 5 new)

## Self-Check: PASSED

All 8 files verified present. Both commit hashes (31c9341, cc95a0f) confirmed in git log.

---
*Phase: 01-infrastructure*
*Completed: 2026-03-16*
