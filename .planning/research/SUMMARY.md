# Project Research Summary

**Project:** Network Detail Page (Page Reseau Detaillee)
**Domain:** Energy community (autoconsommation collective) — single-network public detail view
**Researched:** 2026-03-16
**Confidence:** HIGH

## Executive Summary

The network detail page is a brownfield feature addition to an existing ClojureScript/Clojure application. No new dependencies are required: the stack (Re-frame, Reagent, Reitit, Google Maps JS API, XTDB) is already in place and sufficient. The page's core challenge is an architectural gap: `ProductionRepo` has no `find-by-network-id` method, which means backend work must precede frontend work. The recommended approach is a single aggregated endpoint (`GET /api/v1/networks/:id/detail`) that computes stats server-side and returns only public-safe fields, keeping the frontend as a pure display layer.

The feature set is well-understood from competitor analysis (Enogrid, Maps Energy). Table stakes are: an interactive map with the network zone, a statistics panel (kWc, producer count, consumer count, energy mix), a production list, generated descriptive text, and a CTA to sign up. Real-time data, consumer identity disclosure, and CMS-managed content are explicitly out of scope per project constraints. The MVP is achievable in four sequential build phases with no experimental territory.

The primary risks are security (returning sensitive production fields on a public endpoint), Google Maps lifecycle management (Reagent re-render destroying the map instance), and code duplication (a third near-identical map interop implementation emerging). All three are preventable with patterns already proven in the codebase, as long as they are addressed in Phase 1 before UI work begins.

## Key Findings

### Recommended Stack

The existing stack requires no changes for this milestone. All needed capabilities are present. The only addition is a `GOOGLE_MAPS_MAP_ID` closure define (alongside the existing `GOOGLE_MAPS_API_KEY`) to support migration from the deprecated `google.maps.Marker` class to `google.maps.marker.AdvancedMarkerElement`. The Google Maps script URL must also include `&libraries=marker`.

**Core technologies:**
- ClojureScript + Reagent + Re-frame: frontend state and rendering — already integrated, no changes
- Google Maps JS API: map display — already loaded; migrate to AdvancedMarkerElement + add Map ID
- Reitit (frontend): routing — add one parameterized route `/reseau/:id`
- Clojure + Reitit (backend): new public endpoint — follow existing handler pattern
- XTDB v1: new query `find-by-network-id` — straightforward `:where` clause on `:production/network-id`

See `.planning/research/STACK.md` for full details and anti-recommendations.

### Expected Features

**Must have (table stakes):**
- Interactive map with network zone circle + production site markers
- Statistics panel: total installed capacity (kWc), producer count, consumer count (anonymous), energy mix by type
- Production list: name/label, energy type, location
- Generated descriptive text from network data template
- CTA button linking to signup/eligibility flow
- Public route `/reseau/:id` accessible after eligibility check result

**Should have (competitive differentiators, v1.x):**
- Estimated savings indicator ("save ~X EUR/year") — after tariff assumptions validated
- Environmental impact summary (CO2 avoided, households equivalent) — static calculation
- Share buttons (URL copy, WhatsApp, email)
- Mobile-optimized layout
- Animated count-up on key statistics

**Defer (v2+):**
- Real-time production data (requires Enedis API + time-series infrastructure)
- CMS-managed network descriptions (when network count exceeds template usefulness)
- Historical production/consumption charts (requires separate data pipeline milestone)
- Network discovery/listing page (premature before many networks exist)

See `.planning/research/FEATURES.md` for full prioritization matrix and competitor analysis.

### Architecture Approach

The page fits cleanly into the existing 3-layer DDD architecture: one new scenario function in `application/`, one new handler+route in `infrastructure/rest_api/`, one new XTDB query, and a new Re-frame module (events, subs, page component) in the frontend. No new domain entities are needed. The key design principle is a single aggregated endpoint that bundles network + productions + stats into one response, avoiding frontend aggregation and race conditions from multiple API calls.

**Major components:**
1. `application/network_scenarios.clj` — new `get-network-detail` aggregating network + productions + consumer count from three repos
2. `infrastructure/rest_api/network_handler.clj` — new public GET handler with `serialize-public-production` whitelist
3. `infrastructure/xtdb/xtdb_production_repo.clj` — new `find-by-network-id` XTDB query
4. `frontend/app/network_detail/` — new Re-frame module (events, subs)
5. `frontend/app/pages/network_detail.cljs` — new page component: map + stats + production list + CTA
6. `frontend/app/routes.cljs` — add `["/reseau/:id" {:name :page/network-detail}]`

For production markers: defer geocoding to a later iteration. For MVP, show network circle only and list productions as cards below the map (Option B from ARCHITECTURE.md). This avoids adding lat/lng fields to the Production entity and removes the client-side geocoding N-calls pitfall.

See `.planning/research/ARCHITECTURE.md` for full data flow, code sketches, and build order.

### Critical Pitfalls

1. **Returning sensitive fields on the public endpoint** — Use `serialize-public-production` to whitelist only `id`, `energy-type`, `installed-power`, `producer-address`. Never return IBAN, PRM, or user-id on a public route. Recovery cost: MEDIUM if missed.

2. **Google Maps lifecycle mismatch with Reagent re-renders** — Use Form-3 (`r/create-class`) for the map container. Never conditionally render the map div. Store map instance and overlays in atoms, never in app-db. Apply overlay changes in `:component-did-update`, not `:reagent-render`. This pattern exists in the codebase — copy and extend it.

3. **Missing `find-by-network-id` on ProductionRepo** — This is the single biggest blocking dependency. The backend foundation (protocol method + XTDB query + scenario + endpoint) must be built and tested before frontend work starts.

4. **Duplicated Google Maps interop code** — Two near-identical implementations already exist (`google_map.cljs` and `eligibility_form.cljs`). Extract a shared `app.utils.google-maps` namespace before building the detail page map. This prevents a third divergent copy.

5. **Non-public networks accessible via URL** — The backend must return 404 for networks not in `:public` lifecycle. Guarding this in `get-network-detail` is straightforward but easy to forget.

See `.planning/research/PITFALLS.md` for the full checklist including UX pitfalls, security mistakes, and performance traps.

## Implications for Roadmap

Based on combined research, the natural build order is backend-first, then frontend wiring, then visible UI, then polish.

### Phase 1: Backend Foundation

**Rationale:** All frontend components depend on the API. The `find-by-network-id` gap is the single blocking dependency. TDD in isolation before any UI work ensures correctness and prevents security mistakes from being baked in.
**Delivers:** A fully tested, secure `GET /api/v1/networks/:id/detail` endpoint returning aggregated data with public-safe serialization.
**Addresses:** Table stakes features: productions by network, stats (kWc, counts, energy mix), public network access guard.
**Avoids:** Pitfalls 2 (data leak), 3 (missing repo method), 5 (client-side stats computation).

Tasks:
- Add `find-by-network-id` to `ProductionRepo` protocol + XTDB + in-memory implementations
- Add `get-network-detail` scenario in `application/network_scenarios.clj`
- Add `GET /api/v1/networks/:id/detail` handler + route (public, no auth)
- Add `serialize-public-production` whitelist function
- Wire new repos into `handler.clj` route builder
- Configure Google Maps API key HTTP referrer restrictions (deployment checklist)

### Phase 2: Frontend Wiring

**Rationale:** Structural setup (route, state shape, events, subs) must exist before the page component can be built. Establishes the data loading contract.
**Delivers:** A working route `/reseau/:id` that fetches network detail from the API and stores it in app-db.
**Addresses:** Route handling pitfall (SPA refresh 404), state shape design.
**Avoids:** Pitfall 6 (route handling), avoids building UI before data contract is confirmed.

Tasks:
- Extract shared `app.utils.google-maps` namespace (refactor before adding third map component)
- Add route `["/reseau/:id" {:name :page/network-detail}]` in `routes.cljs`
- Add `:network-detail/*` state keys to `db.cljs`
- Create `app/network_detail/events.cljs` and `subs.cljs`
- Add page dispatch in `views.cljs`
- Verify SPA fallback route on backend serves `index.html` for `/reseau/*`

### Phase 3: Visible Page

**Rationale:** With API and wiring in place, the page component can be built against real data. Map component is the most complex element; build it with correct Form-3 lifecycle from the start.
**Delivers:** A fully functional public detail page: map with network circle, stats cards, production list, generated description, CTA button.
**Uses:** Google Maps AdvancedMarkerElement with Map ID, existing `draw-circles!` pattern, Re-frame subscriptions.
**Implements:** `pages/network_detail.cljs`, Google Maps Form-3 component, navigation link from eligibility form result.

Tasks:
- Add `GOOGLE_MAPS_MAP_ID` closure define to `shadow-cljs.edn`
- Add `&libraries=marker` to Google Maps script URL
- Create network detail Google Maps component (Form-3, centered on network, `fitBounds` for zoom)
- Create stats cards component (kWc, producer count, consumer count, energy mix bar)
- Create production list component (name/label, energy type, location)
- Add generated description text template
- Add CTA button (`href` to `:page/signup`)
- Add "Decouvrir le reseau" link in eligibility form result

### Phase 4: Content and Polish

**Rationale:** Content and styling are independent of logic and can iterate freely once core page is validated with real users.
**Delivers:** A polished, mobile-responsive page with loading states, skeleton placeholders, and conversion-optimized layout.
**Addresses:** UX pitfalls (layout shift, no loading state, CTA visibility), differentiator features.

Tasks:
- Add skeleton placeholders for stats and map during loading
- Add error state for network not found (404) or API failure
- Test and fix mobile layout (375px viewport, map touch zoom)
- Add estimated savings indicator (v1.x — after tariff assumptions validated)
- Add environmental impact summary (CO2, households equivalent)
- Add share buttons
- Verify production list handles zero-production networks without NaN

### Phase Ordering Rationale

- Phase 1 before Phase 2: the frontend route can be wired against a mock, but real integration testing requires the API to exist. Backend-first also enforces the security contract (field whitelisting) from day one.
- Phase 2 before Phase 3: routing and state shape must be stable before building the page component that consumes them. The shared maps-interop refactor in Phase 2 ensures Phase 3 doesn't create technical debt.
- Phase 3 before Phase 4: polish is meaningless before the core page works. Mobile layout decisions depend on real content proportions.
- The 4-phase structure matches the architecture's build order exactly (ARCHITECTURE.md, "Build Order" section).

### Research Flags

Phases with standard patterns (skip research-phase — well-documented):
- **Phase 1:** XTDB queries, DDD scenario aggregation, Reitit route handlers — all established patterns in this codebase. No research needed.
- **Phase 2:** Re-frame module structure, Reitit parameterized routes — documented in CLAUDE_FRONT.md and existing codebase.
- **Phase 3:** Google Maps Form-3 pattern — already exists in `google_map.cljs`. Extend, don't invent. AdvancedMarkerElement migration is documented in Google's own migration guide.

Phases that may need targeted validation:
- **Phase 4 (Estimated savings):** Requires validated tariff differential assumptions for French ACC (CRE-regulated). Do not show a number without validating the formula with domain experts or official Enedis/CRE data. Flag for business validation before implementation.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified against live codebase. No new dependencies. Only change is AdvancedMarkerElement migration, documented by Google. |
| Features | HIGH | Table stakes features confirmed against competitor analysis (Enogrid, Maps Energy) and official Enedis ACC observatory data. Anti-features validated against project constraints. |
| Architecture | HIGH | All patterns derive from existing codebase analysis. No greenfield decisions — every component follows an established precedent in the project. |
| Pitfalls | HIGH | Critical pitfalls (data leak, Maps lifecycle) verified against codebase. Google Maps Marker deprecation confirmed in Google's official deprecation docs. |

**Overall confidence:** HIGH

### Gaps to Address

- **Production display name:** Productions currently have `:production/producer-address` but no display name (installation name or producer name). The production list needs a human-readable label. Options: join with user data (producer's name), add a `:production/label` field, or fall back to energy type + address. Decide during Phase 1 design before defining the API response shape.

- **Estimated savings formula:** The savings indicator (Phase 4, v1.x) requires a tariff differential assumption (ACC price vs. grid tariff). This is a business decision, not a technical one. Do not implement without validated numbers from CRE or business owner.

- **Network lifecycle "public" confirmation:** The architecture assumes only `:public` lifecycle networks are shown. Confirm with the domain model that `:public` is the correct lifecycle state for "visible to prospective consumers" vs. `:private` or `:active`. (The existing `network.clj` likely defines this but was not the focus of this research.)

## Sources

### Primary (HIGH confidence)
- Codebase: `frontend/src/app/components/google_map.cljs` — existing Form-3 map pattern
- Codebase: `frontend/src/app/components/eligibility_form.cljs` — existing Maps + Re-frame integration
- Codebase: `backend/src/infrastructure/rest_api/network_handler.clj` — existing API pattern
- Codebase: `backend/src/domain/production.clj`, `network.clj`, `consumption.clj` — entity schemas and repo protocols
- [Google Maps Advanced Markers Migration](https://developers.google.com/maps/documentation/javascript/advanced-markers/migration) — AdvancedMarkerElement requirements
- [Google Maps Deprecations](https://developers.google.com/maps/deprecations) — Marker deprecation timeline
- [Enedis autoconsommation observatory](https://observatoire.enedis.fr/autoconsommation) — ACC market data

### Secondary (MEDIUM confidence)
- [Enogrid tools overview](https://enogrid.com/outils/) — competitor feature set
- [Maps Energy Community Designer](https://energy.mapsgroup.it/en/energy-community-designer-en/) — competitor feature set
- [Mon energie collective FAQ](https://monenergiecollective.fr/faq/) — user expectations for ACC platforms
- [Reagent on Clojars](https://clojars.org/reagent) — version 2.0.1 (latest), breaking change assessment
- [Re-frame on Clojars](https://clojars.org/re-frame) — version 1.4.4 (latest)

### Tertiary (LOW confidence)
- [Reonic ACC guide 2026](https://reonic.com/fr-fr/blog/autoconsommation-collective-guide-complet-2026/) — French ACC market context

---
*Research completed: 2026-03-16*
*Ready for roadmap: yes*
