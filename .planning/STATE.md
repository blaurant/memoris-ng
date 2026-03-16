---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: in-progress
stopped_at: Completed 02-01-PLAN.md — shared utils & derived subs
last_updated: "2026-03-16T18:41:00Z"
last_activity: 2026-03-16 — Completed 02-01-PLAN.md (shared Google Maps utils + derived subs)
progress:
  total_phases: 2
  completed_phases: 1
  total_plans: 4
  completed_plans: 3
  percent: 75
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-16)

**Core value:** L'utilisateur éligible doit comprendre immédiatement ce qu'est le réseau, ce qu'il y gagne, et avoir envie d'y adhérer.
**Current focus:** Phase 2 — Page Visible

## Current Position

Phase: 2 of 2 (Page Visible)
Plan: 1 of 2 in current phase
Status: Plan 02-01 Complete
Last activity: 2026-03-16 — Completed 02-01-PLAN.md (shared Google Maps utils + derived subs)

Progress: [████████░░] 75%

## Performance Metrics

**Velocity:**
- Total plans completed: 3
- Average duration: 6min
- Total execution time: 18min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-infrastructure | 2 | 14min | 7min |
| 02-page-visible | 1 | 4min | 4min |

**Recent Trend:**
- Last 5 plans: 01-01 (6min), 01-02 (8min), 02-01 (4min)
- Trend: stable

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Contenu généré (pas administré) — simplifier le MVP
- [Roadmap]: Producteurs = productions existantes — pas de nouvelle entité
- [Roadmap]: Accès via éligibilité uniquement — pas de listing des réseaux
- [Phase 1]: `serialize-public-production` whitelist obligatoire avant tout — risque de fuite données sensibles (IBAN, PRM, user-id) sur endpoint public
- [Phase 1]: Extraction namespace partagé `app.utils.google-maps` avant de construire le composant carte — deux implémentations quasi-identiques existent déjà
- [Phase 1-01]: Return 404 (not 403) for non-public networks to avoid leaking existence information
- [Phase 1-01]: Energy-mix as percentage distribution, empty map {} when zero active productions
- [Phase 1-02]: Route-driven fetch: navigating to :page/network-detail auto-dispatches :network-detail/fetch
- [Phase 1-02]: Clear previous network-detail data on navigation to avoid stale state
- [Phase 2]: Utiliser Form-3 (`r/create-class`) pour le composant carte — jamais re-render conditionnel de la div map
- [Phase 2-01]: Shared utils namespace pattern: app.utils.google-maps centralizes all Google Maps JS interop
- [Phase 2-01]: Layer-3 subs use :<- signal syntax deriving from :network-detail/data

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 1]: Production display name non résolu — les productions n'ont pas de nom d'affichage dédié. Décider avant de définir le shape de la réponse API : join user data, champ `:production/label`, ou fallback type + adresse.
- [Phase 1]: Confirmer que `:public` est le bon lifecycle state pour "visible aux prospects" dans `network.clj`. **RESOLVED in 01-01**: Confirmed, `:public` is correct (already used in eligibility flow).

## Session Continuity

Last session: 2026-03-16
Stopped at: Completed 02-01-PLAN.md — shared utils & derived subs
Resume file: None
