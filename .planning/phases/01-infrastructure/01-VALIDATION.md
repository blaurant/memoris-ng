---
phase: 1
slug: infrastructure
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-16
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Kaocha (via `clj -M:test`) |
| **Config file** | `backend/tests.edn` (or deps.edn :test alias) |
| **Quick run command** | `cd backend && clj -M:test --focus-meta :phase-1` |
| **Full suite command** | `cd backend && clj -M:test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd backend && clj -M:test --focus-meta :phase-1`
- **After every plan wave:** Run `cd backend && clj -M:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-01-01 | 01 | 1 | INFR-03 | unit | `cd backend && clj -M:test --focus domain.production-test` | Exists but needs new tests | ⬜ pending |
| 1-01-02 | 01 | 1 | INFR-01 | functional (GWT) | `cd backend && clj -M:test --focus application.network-scenarios-test` | Wave 0 | ⬜ pending |
| 1-01-03 | 01 | 1 | INFR-01 | functional (GWT) | `cd backend && clj -M:test --focus application.network-scenarios-test` | Wave 0 | ⬜ pending |
| 1-01-04 | 01 | 1 | INFR-01 | functional (GWT) | `cd backend && clj -M:test --focus application.network-scenarios-test` | Wave 0 | ⬜ pending |
| 1-02-01 | 02 | 1 | INFR-02 | manual | Browser: navigate to `/reseau/:id` | Manual-only | ⬜ pending |
| 1-02-02 | 02 | 1 | INFR-02 | manual | Browser: inspect Re-frame app-db | Manual-only | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/test/application/network_scenarios_test.clj` — ADD scenarios for `get-network-detail` (public network, non-public 404, missing 404, no productions, sensitive field exclusion)
- [ ] No frontend test infrastructure exists — INFR-02 verification is manual (browser + devtools)

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Frontend route `/reseau/:id` exists and triggers API fetch | INFR-02 | No frontend test infrastructure | Navigate to `/reseau/:id` in browser, verify network tab shows API call |
| Re-frame state populated after fetch | INFR-02 | No frontend test infrastructure | Inspect Re-frame app-db via devtools after navigation |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
