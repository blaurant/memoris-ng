# Pitfalls Research

**Domain:** Network detail page with Google Maps + stats (ClojureScript/Re-frame)
**Researched:** 2026-03-16
**Confidence:** HIGH (based on codebase analysis + domain experience)

## Critical Pitfalls

### Pitfall 1: Google Maps lifecycle mismatch with Reagent re-renders

**What goes wrong:**
The Google Maps JS API owns its DOM subtree. When Reagent re-renders the parent component (e.g., because a subscription changes), it can destroy and recreate the map container div, killing the map instance. The map goes blank or shows a grey rectangle. This is already partially handled in the existing `google_map.cljs` and `eligibility_form.cljs` (Form-3 components with `:ref`), but the detail page will be more complex -- it needs markers for productions, a network circle, and potentially info windows, all reacting to different subscriptions.

**Why it happens:**
Reagent Form-1 components re-render on every subscription change. If the map div is conditionally rendered (`when`, `if`), React unmounts it. Even with Form-3 (`create-class`), careless use of subscriptions in the render function can trigger unnecessary updates that reset the map state (zoom, pan position).

**How to avoid:**
- Use Form-3 (`r/create-class`) for the map container, as the existing code does. The map div must be unconditionally rendered (never inside `when`/`if`).
- Keep map-mutating logic in `:component-did-update`, not in `:reagent-render`. The render function should only return the static container div.
- Use a single `r/atom` or `atom` for the map instance, and separate atoms for overlay objects (markers, circles). Never store Google Maps objects in `app-db`.
- Deref subscriptions in `:reagent-render` to trigger updates, but apply changes to the map in `:component-did-update` by comparing previous and new argv.

**Warning signs:**
- Map flickers or resets zoom/position when unrelated data changes
- Grey map rectangle after navigating away and back
- Console errors about "google is not defined" on route change

**Phase to address:**
Phase 1 (Map component) -- get the lifecycle right from the start. Copy patterns from `eligibility_form.cljs` but extend for markers.

---

### Pitfall 2: No backend endpoint to fetch productions by network ID

**What goes wrong:**
The detail page needs to display productions belonging to a specific network (name, energy type, installed power, location). But `ProductionRepo` only has `find-all`, `find-by-id`, and `find-by-user-id`. There is no `find-by-network-id`. Fetching all productions and filtering client-side leaks private data (IBAN, user IDs, PRM numbers) and does not scale.

**Why it happens:**
The production entity was designed for the onboarding/consumer flow where a user manages their own productions. The "public view of a network's productions" is a new read model that does not exist yet.

**How to avoid:**
- Add `find-by-network-id` to `ProductionRepo` protocol and implement in XTDB/in-memory repos.
- Create a dedicated public endpoint (`GET /api/v1/networks/:id/productions`) that returns only public-safe fields: production name/label, energy type, installed power (kWc). Explicitly exclude sensitive fields (IBAN, PRM, user-id).
- Consider a dedicated query/read model in `application/network_scenarios.clj` that aggregates stats (total kWc, energy mix percentages, production count) server-side rather than computing them client-side.

**Warning signs:**
- Frontend code doing `(filter #(= network-id (:production/network-id %)) all-productions)` -- this means you are leaking all productions to the client
- API returning fields like `:production/iban` or `:production/user-id` on a public page

**Phase to address:**
Phase 1 (Backend API) -- this must be built before the frontend can display anything meaningful.

---

### Pitfall 3: Google Maps API key exposed without restrictions

**What goes wrong:**
The Google Maps API key is embedded in the frontend JavaScript bundle (via `closure-defines`). Without HTTP referrer restrictions on the key, anyone can extract it from the page source and use it on their own site, incurring charges on the project's billing account. Google Maps API calls are not free -- Geocoding + Maps JavaScript API can add up quickly.

**Why it happens:**
During development, keys are often unrestricted. The key is already in the codebase (`GOOGLE_MAPS_API_KEY` in `config.cljs`), injected at build time. Developers forget to configure restrictions in the Google Cloud Console before deploying to production.

**How to avoid:**
- In Google Cloud Console, restrict the Maps JavaScript API key to specific HTTP referrers (the production and staging domains).
- Set a daily quota/budget cap on the Maps API to prevent bill shock.
- The detail page adds a new map instance per network page view. If many users check eligibility and then view the detail page, map loads double. Monitor usage.

**Warning signs:**
- Google Cloud billing alerts
- API key visible in `view-source:` without referrer restrictions configured
- No budget alerts set in Google Cloud Console

**Phase to address:**
Phase 1 (Infrastructure) -- configure key restrictions before the detail page goes live. Not a code task, but a deployment checklist item.

---

### Pitfall 4: Duplicated Google Maps interop code across components

**What goes wrong:**
The codebase already has two nearly identical implementations of circle drawing: `google_map.cljs/draw-circles!` and `eligibility_form.cljs/draw-network-circles!`. The detail page will need circles + markers + possibly info windows. Without extracting shared utilities, a third copy emerges with subtle differences (different colors, missing cleanup, inconsistent error handling).

**Why it happens:**
Each map component was built independently. The interop code is small enough that copy-paste feels faster than extracting a shared module. But each copy handles lifecycle cleanup differently, and bugs fixed in one are not fixed in the other.

**How to avoid:**
- Before building the detail page map, extract a `app.components.maps-interop` or `app.utils.google-maps` namespace with shared functions: `draw-circle!`, `draw-marker!`, `clear-overlays!`, `fit-bounds!`.
- The existing `load-google-maps-script!` in `google_map.cljs` is already reusable -- move it to the shared namespace.
- Each page-level component still uses Form-3 for lifecycle, but delegates overlay management to shared functions.

**Warning signs:**
- Three or more files with `js/google.maps.Circle.` calls
- Circle colors/opacity inconsistent between components
- Memory leak from overlays not being cleaned up on unmount (`.setMap nil` missing)

**Phase to address:**
Phase 1 (Refactoring) -- extract shared code before building the detail page map, not after.

---

### Pitfall 5: Computing aggregate stats client-side from raw production data

**What goes wrong:**
The detail page needs: total installed capacity (sum of kWc), energy mix (% solar/wind/hydro), production count, consumer count. If these are computed in the frontend from raw lists, every visitor downloads the full production list, and any calculation error (wrong filter, missing null check) shows incorrect stats. Worse, if the network has many productions, the payload becomes unnecessarily large.

**Why it happens:**
It feels simpler to just return all productions and let the frontend `reduce` over them. No new backend code needed. But this conflates a "detail/summary" read model with a "list" read model.

**How to avoid:**
- Create a dedicated backend endpoint or extend `GET /api/v1/networks/:id` to return pre-computed stats: `{:total-capacity-kwc 150.0 :energy-mix {:solar 60 :wind 30 :hydro 10} :production-count 5 :consumer-count 42}`.
- Compute stats in `application/network_scenarios.clj` where you have access to both `ProductionRepo` and `ConsumptionRepo`.
- The frontend simply displays the numbers. No aggregation logic in ClojureScript.

**Warning signs:**
- Frontend code with `(reduce + (map :production/installed-power ...))`
- Frontend importing both production and consumption subscriptions on a public page
- Stats showing "0" or "NaN" because of nil values in incomplete productions

**Phase to address:**
Phase 1 (Backend API) -- design the response shape before building the frontend.

---

### Pitfall 6: Route handling -- network detail page without proper URL structure

**What goes wrong:**
The detail page needs a URL like `/networks/:id`. But the existing routing in the app does not have parameterized public routes. If the route is added carelessly, issues arise: browser refresh on `/networks/some-uuid` returns a 404 (SPA routing not configured for that path), or the network ID is not extracted from the route params, or navigating back from the detail page loses the eligibility result state.

**Why it happens:**
The current routes are simple (`:page/home`, `:page/login`, etc.) without path parameters. Adding a parameterized route requires changes to the Reitit router config, the route-change event handler, and potentially the backend's catch-all route for SPA fallback.

**How to avoid:**
- Add the route with proper parameter coercion in the Reitit frontend config: `["/networks/:id" {:name :page/network-detail :parameters {:path {:id string?}}}]`.
- Ensure the backend serves `index.html` for all frontend routes (SPA fallback). Check that the backend's static file handler or catch-all route covers `/networks/*`.
- Store the network ID in `app-db` via a route-change event, then dispatch an API fetch for the network detail. Do not rely on the eligibility result being in `app-db` -- the user might arrive via a direct URL.

**Warning signs:**
- Refreshing the detail page shows a blank page or 404
- The network data is undefined after a direct URL visit (only works when navigating from eligibility)
- Back button from detail page clears eligibility state

**Phase to address:**
Phase 1 (Routing) -- set up the route and data fetching before building UI components.

---

### Pitfall 7: Marker clustering and map bounds not handled for variable production counts

**What goes wrong:**
A network might have 2 productions or 20. With a fixed zoom level and center (copied from the existing map component that centers on France at zoom 6), the detail page map either shows markers too far apart or completely overlapping. The map does not auto-fit to show all productions within the network boundary.

**Why it happens:**
The existing `network-map` component hard-codes center at France's center (`lat 46.6, lng 1.88`) with zoom 6. This works for showing all networks on a national map but is wrong for a single network detail view where the map should be centered on that network and zoomed to fit its radius.

**How to avoid:**
- Center the map on the network's `center-lat`/`center-lng` and calculate zoom from `radius-km`. A 1km radius needs ~zoom 15, a 5km radius ~zoom 12. Use `map.fitBounds()` with the circle's bounds for automatic zoom.
- For networks with many productions, consider using Google Maps marker clustering (the `@googlemaps/markerclusterer` library). But for the typical scale of local energy networks (5-20 productions), simple markers with slight offset are sufficient.
- Add an info window on marker click showing production name and type, rather than cramming all info into the marker icon.

**Warning signs:**
- All markers stacked on the same pixel for small-radius networks
- Map shows entire France when only one network zone should be visible
- User has to manually zoom in 8 levels to see the actual network area

**Phase to address:**
Phase 2 (Map polish) -- basic centering in Phase 1, `fitBounds` refinement in Phase 2.

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Hard-code network stats in the frontend | Skip backend work, ship faster | Stats become stale, incorrect, or inconsistent with real data | Never -- even for MVP, compute server-side |
| Copy-paste map interop from eligibility form | Ship detail page faster | Three divergent map implementations to maintain | Only if refactoring is planned for the same sprint |
| Return all production fields on the public endpoint | No new serialization code | Leaks IBAN, user IDs, PRM to unauthenticated visitors | Never -- security issue |
| Skip `fitBounds` and hard-code zoom | Faster to implement | Broken UX for networks of different sizes | Acceptable for first prototype if all test networks are similar size |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Google Maps JS API | Loading the script multiple times when navigating between pages with maps | Use the existing `load-google-maps-script!` which checks for existing script tag -- but move it to a shared namespace |
| Google Maps JS API | Not waiting for API load before creating map objects | The existing callback pattern handles this, but the detail page must also guard against `js/google` being undefined if the script fails to load (network error, blocked by ad blocker) |
| Google Maps JS API | Using deprecated `google.maps.Marker` (deprecated since 2024) | Use `google.maps.marker.AdvancedMarkerElement` instead. Note: requires a Map ID configured in Google Cloud Console. The existing code uses `Marker` in `eligibility_form.cljs` -- this will trigger deprecation warnings |
| XTDB queries | Writing a query that scans all productions to filter by network-id | Add a proper XTDB query with `:where` clause filtering by `:production/network-id` rather than `find-all` + `filterv` |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Re-rendering the entire detail page when any subscription changes | Map flickers, stats section rebuilds unnecessarily | Split into independent sub-components: `[network-map ...]`, `[network-stats ...]`, `[network-description ...]`, each with their own subscriptions | Noticeable with >5 subscriptions on the page |
| Creating new Google Maps overlay objects on every component update | Memory leak, map slows down after several navigation cycles | Track overlays in atoms, clear before redraw (the existing pattern does this but must be maintained) | After 10+ navigation cycles between pages |
| Fetching network + productions + consumers in three sequential API calls | Visible loading cascade (map appears, then stats pop in one by one) | Single endpoint returning all detail page data, or parallel dispatches with a combined loading state | Noticeable on slow connections (3G) |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Public network detail endpoint returns production user IDs | User enumeration, privacy violation (who participates in which network) | Return only: energy type, installed power, label/name. No user-id, no IBAN, no PRM |
| Network ID in URL is a guessable UUID | Allows enumeration of all networks including private/pending ones | Backend must filter: only return detail for `:public` lifecycle networks. Return 404 for private networks |
| Consumer count endpoint leaks individual consumer data | Privacy violation (GDPR) | Return only the count (integer), never any consumer identifiers or details. This is already in scope ("anonyme, juste le compteur") |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Map loads after page content, causing layout shift | Content jumps down when map container appears, disorienting the user | Give the map container a fixed aspect-ratio and min-height (already done in existing code: `aspect-ratio: 4/3, min-height: 300px`). Show a skeleton/placeholder with the same dimensions |
| No loading state while fetching network details | User sees empty page or partial data, thinks page is broken | Show skeleton placeholders for stats, a "loading map" overlay on the map container |
| Energy mix percentages shown without context | "60% solar" means nothing to a non-technical user | Add icons per energy type, use a visual bar chart or donut, and add a one-liner explanation ("Ce reseau est alimente principalement par l'energie solaire") |
| CTA button ("Rejoindre") not visible without scrolling | User reads the description but never sees the action button | Place the CTA both above the fold (near network name) and at the bottom of the page. Make it sticky on mobile |

## "Looks Done But Isn't" Checklist

- [ ] **Map component:** Overlays are cleaned up on unmount -- verify no memory leak by navigating away and back 10 times, check browser memory usage
- [ ] **Public endpoint:** Returns 404 for non-public networks -- verify by requesting a `:private` network ID
- [ ] **Stats computation:** Handles networks with zero productions -- verify the page does not show "NaN%" or crash
- [ ] **Stats computation:** Only counts `:active` productions in stats, not `:pending` or `:abandoned` -- verify by having mixed-lifecycle productions in test data
- [ ] **Direct URL access:** `/networks/:id` works on browser refresh -- verify the SPA fallback route serves index.html
- [ ] **Mobile responsiveness:** Map is usable on mobile (touch zoom, not cut off) -- verify on 375px viewport
- [ ] **Google Maps script:** Page still renders (without map) when Google Maps API is blocked (ad blocker, network error) -- verify with network throttling

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Data leak on public endpoint | MEDIUM | Add field whitelist to serialization function, audit existing API responses, deploy fix |
| Duplicated map code across 3 components | LOW | Extract shared namespace, update imports. No behavior change, just refactoring |
| Stats computed client-side | MEDIUM | Add backend endpoint, update frontend to use it. Must coordinate backend+frontend deploy |
| Wrong map zoom/center | LOW | Switch to `fitBounds()`, test with networks of different radii |
| Route 404 on refresh | LOW | Add catch-all route to backend, or configure hosting platform (Railway) for SPA routing |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Maps lifecycle mismatch | Phase 1 (Map component) | Map survives 10 navigation cycles without grey-out or memory leak |
| No find-by-network-id | Phase 1 (Backend API) | Endpoint exists and returns filtered, safe data |
| API key exposed | Phase 1 (Infrastructure checklist) | Key has HTTP referrer restrictions in Google Cloud Console |
| Duplicated interop code | Phase 1 (Refactoring before build) | Single `maps-interop` namespace, old components updated |
| Client-side stats | Phase 1 (Backend API) | Stats returned by API, no aggregation in ClojureScript |
| Route handling | Phase 1 (Routing setup) | Direct URL to `/networks/:id` works after browser refresh |
| Map bounds/zoom | Phase 2 (Map polish) | Map auto-zooms to network boundary for networks of different radii |

## Sources

- Codebase analysis: `frontend/src/app/components/google_map.cljs`, `eligibility_form.cljs` (existing map patterns)
- Codebase analysis: `backend/src/domain/production.clj`, `network.clj` (entity schemas, repo protocols)
- Codebase analysis: `backend/src/infrastructure/rest_api/network_handler.clj` (existing API surface)
- Google Maps JavaScript API deprecation of `Marker` class (2024): training data, MEDIUM confidence
- Re-frame component lifecycle patterns: training data verified against codebase patterns, HIGH confidence

---
*Pitfalls research for: Network detail page with Google Maps + stats*
*Researched: 2026-03-16*
