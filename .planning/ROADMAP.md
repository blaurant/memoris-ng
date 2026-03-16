# Roadmap: Page Réseau Détaillée

## Overview

Ajout d'une page publique de détail réseau à l'application memoris-ng existante. La livraison se fait en deux phases : d'abord l'infrastructure (API backend + route frontend), puis la page visible avec carte, statistiques, liste des productions et CTA. Le backend doit précéder le frontend car le gap `find-by-network-id` est le seul bloquant structurel.

## Phases

**Phase Numbering:**
- Integer phases (1, 2): Planned milestone work
- Decimal phases (1.1, 1.2): Urgent insertions (marked with INSERTED)

- [x] **Phase 1: Infrastructure** - Endpoint public sécurisé + route frontend + plomberie état Re-frame (completed 2026-03-16)
- [ ] **Phase 2: Page Visible** - Carte interactive, statistiques, liste productions, description générée, CTA

## Phase Details

### Phase 1: Infrastructure
**Goal**: Le contrat API est établi et testé, la route frontend existe et charge les données depuis l'API — la page est vide mais fonctionnelle de bout en bout.
**Depends on**: Nothing (first phase)
**Requirements**: INFR-01, INFR-02, INFR-03
**Success Criteria** (what must be TRUE):
  1. `GET /api/v1/networks/:id/detail` retourne réseau + productions + stats agrégées pour un réseau public, et 404 pour un réseau inexistant ou non-public
  2. La réponse ne contient aucun champ sensible (IBAN, PRM, user-id) — vérifiable en inspectant le JSON brut
  3. La route `/reseau/:id` est accessible dans le navigateur et déclenche le fetch de l'API sans erreur 404 ou boucle de rechargement
  4. L'état Re-frame est chargé avec les données réseau et le statut de chargement est observable via les subscriptions
**Plans:** 2/2 plans complete

Plans:
- [x] 01-01-PLAN.md — Backend: find-by-network-id + get-network-detail scenario + handler + tests
- [x] 01-02-PLAN.md — Frontend: route /reseau/:id + Re-frame events/subs + placeholder page

### Phase 2: Page Visible
**Goal**: Un visiteur éligible peut voir la page `/reseau/:id` avec la carte géographique du réseau, les statistiques clés, la liste des productions et un bouton pour s'inscrire.
**Depends on**: Phase 1
**Requirements**: MAP-01, MAP-02, STAT-01, STAT-02, CONT-01, CONT-02, CONT-03
**Success Criteria** (what must be TRUE):
  1. La carte Google Maps affiche le cercle de la zone réseau centré sur les coordonnées du réseau
  2. Les marqueurs des sites de production sont positionnés sur la carte (ou listés sous la carte si les coordonnées ne sont pas disponibles)
  3. Les statistiques affichées (capacité totale en kWc, mix énergétique en %) correspondent aux données de l'API
  4. La liste des productions affiche nom/raison sociale, type d'énergie et localisation pour chaque production
  5. Le bouton CTA dirige vers le parcours d'inscription / test d'éligibilité
**Plans:** 2 plans

Plans:
- [ ] 02-01-PLAN.md — Extract shared Google Maps utilities + add derived Re-frame subscriptions
- [ ] 02-02-PLAN.md — Build full network detail page (map, stats, productions, description, CTA) + CSS

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Infrastructure | 2/2 | Complete    | 2026-03-16 |
| 2. Page Visible | 0/2 | Not started | - |
