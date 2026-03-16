# Page Réseau Détaillée

## What This Is

Une page publique de présentation détaillée pour chaque réseau d'autoconsommation collective. Accessible après vérification d'éligibilité, elle permet aux visiteurs de découvrir le réseau : carte géographique, productions installées, statistiques, et description engageante invitant à rejoindre le projet.

## Core Value

L'utilisateur éligible doit comprendre immédiatement ce qu'est le réseau, ce qu'il y gagne, et avoir envie d'y adhérer.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Page publique accessible après vérification d'éligibilité
- [ ] Carte géographique affichant la zone du réseau (cercle/périmètre)
- [ ] Carte affichant les points des productions sur la zone
- [ ] Liste des productions avec nom/raison sociale, type d'énergie, localisation
- [ ] Nombre de productions affiché
- [ ] Nombre de consommateurs affiché (anonyme, juste le compteur)
- [ ] Capacité totale installée du réseau (somme des puissances en kWc)
- [ ] Mix énergétique (répartition solaire/éolien/hydro en %)
- [ ] Description générée style "rejoignez l'opération d'autoconsommation collective"
- [ ] Bouton/lien vers le parcours d'inscription (tester éligibilité / créer un compte)

### Out of Scope

- Administration du contenu par un back-office — contenu généré pour le moment
- Données de production temps réel ou historique — pas dans cette itération
- Liste ou détails des consommateurs — seul le nombre est affiché
- Page listing tous les réseaux — accès uniquement via éligibilité

## Context

- Projet brownfield : l'application memoris-ng existe avec backend Clojure (XTDB) et frontend ClojureScript (Re-frame)
- Les entités Network et Production existent déjà dans le domaine
- Le check d'éligibilité existe (`POST /api/v1/networks/check-eligibility`)
- Google Maps API déjà intégrée côté frontend (GOOGLE_MAPS_API_KEY)
- Le contenu descriptif s'inspire du modèle : "Rejoignez l'Opération d'Autoconsommation Collective à [ville]" avec centrales, puissance, avantages, appel à l'action

## Constraints

- **Tech stack**: Clojure/ClojureScript existant — pas de nouveau framework
- **Carte**: Google Maps API déjà configurée
- **Données**: Productions rattachées aux réseaux dans XTDB
- **Accès**: Page publique, pas d'authentification requise

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Contenu généré (pas administré) | Simplifier le MVP, on verra l'admin plus tard | — Pending |
| Producteurs = productions existantes | Pas de nouvelle entité, réutiliser le modèle existant | — Pending |
| Accès via éligibilité uniquement | Pas de page listing des réseaux pour le moment | — Pending |

---
*Last updated: 2026-03-16 after initialization*
