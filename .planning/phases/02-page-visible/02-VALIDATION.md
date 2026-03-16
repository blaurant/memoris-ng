---
phase: 2
slug: page-visible
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-16
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Kaocha (backend) / shadow-cljs compile (frontend) |
| **Config file** | backend: `tests.edn` / frontend: `shadow-cljs.edn` |
| **Quick run command** | `cd frontend && npx shadow-cljs compile app 2>&1 \| tail -5` |
| **Full suite command** | `cd backend && clj -M:test` |
| **Estimated runtime** | ~10 seconds (compile check) |

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && npx shadow-cljs compile app 2>&1 | tail -5`
- **After every plan wave:** Visual inspection in dev browser
- **Before `/gsd:verify-work`:** All 7 requirements visually verified
- **Max feedback latency:** 10 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 2-01-01 | 01 | 1 | MAP-01, MAP-02 | manual | Visual: map renders circle + productions listed | N/A | ⬜ pending |
| 2-01-02 | 01 | 1 | STAT-01, STAT-02 | manual | Visual: stats match API data | N/A | ⬜ pending |
| 2-01-03 | 01 | 1 | CONT-01 | manual | Visual: production list shows name, type, location | N/A | ⬜ pending |
| 2-01-04 | 01 | 1 | CONT-02, CONT-03 | manual | Visual: description + CTA button | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. This phase is purely frontend UI — no new test infrastructure needed.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Map renders circle at network center | MAP-01 | Visual/Google Maps JS | Navigate to `/reseau/:id`, verify circle on map |
| Productions listed below map | MAP-02 | Visual | Check production entries appear below map |
| Capacity matches API data | STAT-01 | Visual comparison | Compare displayed kWc with API response |
| Energy mix matches API data | STAT-02 | Visual comparison | Compare displayed % with API response |
| Production list shows details | CONT-01 | Visual | Verify name, type, location for each production |
| Description includes network name | CONT-02 | Visual | Check generated text contains network name |
| CTA navigates to signup | CONT-03 | Click test | Click CTA button, verify navigation |

---

## Validation Sign-Off

- [ ] All tasks have visual verification instructions
- [ ] Compilation check passes after each commit
- [ ] All 7 requirements visually verified in browser
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
