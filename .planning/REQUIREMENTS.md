# Requirements: Page Réseau Détaillée

**Defined:** 2026-03-16
**Core Value:** L'utilisateur éligible doit comprendre immédiatement ce qu'est le réseau, ce qu'il y gagne, et avoir envie d'y adhérer.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Carte

- [ ] **MAP-01**: Carte interactive affichant la zone du réseau (cercle centré sur les coordonnées)
- [ ] **MAP-02**: Marqueurs des sites de production positionnés sur la carte

### Statistiques

- [ ] **STAT-01**: Capacité totale installée du réseau affichée en kWc
- [ ] **STAT-02**: Mix énergétique affiché en % par type d'énergie (solaire, éolien, hydro...)

### Contenu

- [ ] **CONT-01**: Liste des productions avec nom/raison sociale, type d'énergie et localisation
- [ ] **CONT-02**: Description générée du réseau (style "Rejoignez l'Opération d'ACC à [ville]")
- [ ] **CONT-03**: Bouton CTA vers le parcours d'inscription / test d'éligibilité

### Infrastructure

- [x] **INFR-01**: Endpoint public `GET /api/v1/networks/:id/detail` retournant réseau + productions + stats agrégées
- [ ] **INFR-02**: Route frontend `/reseau/:id` accessible après vérification d'éligibilité
- [x] **INFR-03**: Méthode `find-by-network-id` sur ProductionRepo

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Statistiques avancées

- **STAT-03**: Indicateur d'économies estimées (€/an)
- **STAT-04**: Impact environnemental (tonnes CO2 évitées, équivalent foyers)

### UX & Engagement

- **UX-01**: Layout mobile-first responsive
- **UX-02**: Animations compteurs (count-up sur les stats)
- **UX-03**: Boutons de partage (copie lien, WhatsApp, email)

### Carte avancée

- **MAP-03**: Nom et localisation du réseau affichés en en-tête
- **MAP-04**: Nombre de producteurs et de consommateurs affichés

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Données de production temps réel | Nécessite intégration API Enedis, pipeline données, infrastructure time-series |
| Liste ou détails des consommateurs | Vie privée / RGPD — seul le nombre anonymisé est affiché |
| Page listing de tous les réseaux | Accès uniquement via éligibilité pour le moment |
| CMS / back-office contenu | Contenu généré — trop peu de réseaux pour justifier un CMS |
| Graphiques historiques de production | Nécessite collecte de données time-series inexistante |
| Re-vérification éligibilité sur la page | Duplique le formulaire existant, UX confuse |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| INFR-01 | Phase 1 | Complete |
| INFR-02 | Phase 1 | Pending |
| INFR-03 | Phase 1 | Complete |
| MAP-01 | Phase 2 | Pending |
| MAP-02 | Phase 2 | Pending |
| STAT-01 | Phase 2 | Pending |
| STAT-02 | Phase 2 | Pending |
| CONT-01 | Phase 2 | Pending |
| CONT-02 | Phase 2 | Pending |
| CONT-03 | Phase 2 | Pending |

**Coverage:**
- v1 requirements: 10 total
- Mapped to phases: 10
- Unmapped: 0 ✓

---
*Requirements defined: 2026-03-16*
*Last updated: 2026-03-16 — traceability filled after roadmap creation*
