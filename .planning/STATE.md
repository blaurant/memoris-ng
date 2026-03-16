# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-03-16)

**Core value:** L'utilisateur éligible doit comprendre immédiatement ce qu'est le réseau, ce qu'il y gagne, et avoir envie d'y adhérer.
**Current focus:** Phase 1 — Infrastructure

## Current Position

Phase: 1 of 2 (Infrastructure)
Plan: 1 of 2 in current phase
Status: Executing
Last activity: 2026-03-16 — Completed 01-01-PLAN.md (network detail backend API)

Progress: [█████░░░░░] 50%

## Performance Metrics

**Velocity:**
- Total plans completed: 1
- Average duration: 6min
- Total execution time: 6min

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| 01-infrastructure | 1 | 6min | 6min |

**Recent Trend:**
- Last 5 plans: 01-01 (6min)
- Trend: baseline

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
- [Phase 2]: Utiliser Form-3 (`r/create-class`) pour le composant carte — jamais re-render conditionnel de la div map

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 1]: Production display name non résolu — les productions n'ont pas de nom d'affichage dédié. Décider avant de définir le shape de la réponse API : join user data, champ `:production/label`, ou fallback type + adresse.
- [Phase 1]: Confirmer que `:public` est le bon lifecycle state pour "visible aux prospects" dans `network.clj`. **RESOLVED in 01-01**: Confirmed, `:public` is correct (already used in eligibility flow).

## Session Continuity

Last session: 2026-03-16
Stopped at: Completed 01-01-PLAN.md — network detail backend API endpoint
Resume file: None
