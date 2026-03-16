# Technology Stack

**Project:** Network Detail Page (Page Reseau Detaillee)
**Researched:** 2026-03-16

## Existing Stack (No Changes)

The network detail page is a feature addition to an existing brownfield application. The core stack is fixed and does not change.

| Technology | Current Version | Purpose |
|------------|----------------|---------|
| ClojureScript | 1.12.x | Frontend language |
| shadow-cljs | 2.28.14 | Build toolchain |
| Reagent | 1.2.0 | React wrapper (hiccup syntax) |
| Re-frame | 1.3.0 | State management |
| Reitit | 0.7.2 | Client-side routing |
| day8.re-frame/http-fx | 0.2.4 | HTTP effects for re-frame |
| React | 18.3.1 | UI runtime |
| Clojure (backend) | 1.12 | Backend language |
| XTDB v1 | — | Database |

## Recommended Additions for This Milestone

### No new library dependencies needed

This milestone requires **zero new ClojureScript or npm dependencies**. Everything needed is already available through the existing stack and the Google Maps JavaScript API (already loaded via script tag).

**Confidence: HIGH** -- verified against codebase analysis.

### Google Maps API Usage Updates

| Concern | Current State | Recommendation | Why |
|---------|---------------|----------------|-----|
| Marker class | Uses `google.maps.Marker` (deprecated Feb 2024) | Migrate to `google.maps.marker.AdvancedMarkerElement` | Legacy Marker is deprecated; no discontinuation date yet, but no new features or non-regression bugfixes. AdvancedMarkerElement is more performant and accessible. |
| Map ID | Not set (plain `google.maps.Map` init) | Add `mapId: 'DEMO_MAP_ID'` for dev, real Map ID for prod | Required for AdvancedMarkerElement. DEMO_MAP_ID works without Cloud Console setup for development. Production needs a Map ID created in Google Cloud Console. |
| Circle class | Uses `google.maps.Circle` | Keep using `google.maps.Circle` | NOT deprecated. The Drawing *Library* (DrawingManager) is deprecated, but the Circle overlay class is actively maintained. |
| Script loading | Custom `load-google-maps-script!` with callback | Keep as-is, add `&libraries=marker` to the script URL | Needed to load the marker library for AdvancedMarkerElement. |

**Confidence: HIGH** -- verified against Google Maps deprecation docs and release notes.

**Source:** [Google Maps Deprecations](https://developers.google.com/maps/deprecations), [Advanced Markers Migration](https://developers.google.com/maps/documentation/javascript/advanced-markers/migration)

### Configuration Addition

| Item | Value | Purpose |
|------|-------|---------|
| `GOOGLE_MAPS_MAP_ID` | Closure define in shadow-cljs.edn | Map ID for AdvancedMarkerElement. `DEMO_MAP_ID` in dev, real ID in prod. |

This is a string config value, not a library. Add it alongside the existing `GOOGLE_MAPS_API_KEY` closure define.

## Version Upgrade Opportunities (Optional, Not Required)

The project currently uses older versions of Reagent and Re-frame. These upgrades are **not required** for the network detail page but are worth noting:

| Library | Current | Latest | Breaking Changes | Recommendation |
|---------|---------|--------|------------------|----------------|
| Reagent | 1.2.0 | 2.0.1 | Major version bump, likely breaking | DO NOT upgrade during this milestone. Evaluate separately. |
| Re-frame | 1.3.0 | 1.4.4 | Minor bump, should be compatible | Safe to upgrade but not necessary for this feature. |

**Confidence: MEDIUM** -- versions verified on Clojars, but breaking change assessment for Reagent 2.0 based on version number inference only.

**Source:** [Reagent on Clojars](https://clojars.org/reagent), [Re-frame on Clojars](https://clojars.org/re-frame)

## What This Feature Actually Needs (Implementation Patterns, Not Libraries)

The "stack" for this feature is patterns within the existing stack, not new dependencies:

### 1. New Backend API Endpoint

**What:** `GET /api/v1/networks/:id/detail` returning network + productions + stats.

**Why a dedicated endpoint:** The current `GET /api/v1/networks` returns all networks without productions. The detail page needs:
- Network data (name, center, radius, lifecycle)
- Productions for that network (filtered to active ones, with energy type, installed power)
- Aggregated stats (consumer count, total kWc, energy mix percentages)

**Pattern:** Follow existing `network_handler.clj` pattern. Add a new handler function and route. Compute aggregations in the application layer (`application/network_scenarios.clj`), not in the handler.

**Confidence: HIGH** -- direct codebase analysis.

### 2. New Frontend Route

**What:** `/reseau/:id` route in Reitit, mapped to a `network-detail` page component.

**Pattern:** Follow existing route definitions in `routes.cljs`. The route is public (no auth guard).

### 3. Google Maps Component for Detail Page

**What:** A Form-3 Reagent component (like the existing `network-map` and `eligibility-mini-map`) showing:
- Network circle (reuse existing `draw-circles!` pattern)
- Production markers (use AdvancedMarkerElement with custom pin colors by energy type)
- Zoom fitted to the network bounds

**Pattern:** The existing `google_map.cljs` already demonstrates the correct Form-3 lifecycle pattern for Google Maps. Reuse this pattern but with:
- Single network focus (not all networks)
- AdvancedMarkerElement instead of legacy Marker
- `map.fitBounds()` on the circle's bounds for auto-zoom

### 4. Stats Display Components

**What:** Pure Reagent Form-1 components rendering stats from re-frame subscriptions. No charting library needed -- the requirements are simple counters and percentages, not charts.

- Consumer count badge
- Production count badge
- Total installed capacity (kWc)
- Energy mix as percentage bars (CSS-only, no chart library)

**Pattern:** Simple `defn` components subscribing to derived subs. The energy mix bar is a CSS flexbox with colored segments proportional to percentages.

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Map library | Google Maps JS API (existing) | Leaflet + OpenStreetMap | Already integrated, API key configured, eligibility form uses it. Switching adds complexity for zero benefit. |
| Chart library | CSS-only percentage bars | Chart.js, Vega-Lite, Recharts | The requirements show simple percentage breakdowns, not time-series or interactive charts. A chart library is massive overkill. |
| Marker library | AdvancedMarkerElement (native) | @googlemaps/markerclusterer | Only a handful of productions per network. Clustering is unnecessary. |
| Data fetching | re-frame http-fx (existing) | re-frame-fetch, ajax directly | http-fx already works, is configured, used everywhere. No reason to change. |
| New Reagent wrappers | Direct JS interop | react-google-maps (npm) | Adding a React wrapper lib creates an interop layer (React component -> Reagent adapter). Direct `js/google.maps` interop is simpler and already proven in this codebase. |

## Installation

No new dependencies to install. The only changes are:

```clojure
;; shadow-cljs.edn — add GOOGLE_MAPS_MAP_ID closure define
;; alongside existing GOOGLE_MAPS_API_KEY
app.config/GOOGLE_MAPS_MAP_ID #shadow/env ["GOOGLE_MAPS_MAP_ID" "DEMO_MAP_ID"]
```

```clojure
;; google_map.cljs — add libraries=marker to script URL
(str "https://maps.googleapis.com/maps/api/js?key="
     config/GOOGLE_MAPS_API_KEY
     "&libraries=marker"
     "&callback=initGoogleMap")
```

## Anti-Recommendations (What NOT to Add)

| Do NOT Add | Why |
|------------|-----|
| Leaflet / MapLibre | Google Maps is already integrated and working. Dual map libraries is a maintenance burden. |
| Chart.js / D3 / Recharts | The stats are simple counters and percentage bars. CSS handles this trivially. Adding a charting dependency for a colored bar is engineering theater. |
| Reagent 2.0 upgrade | Major version bump during a feature milestone risks regressions unrelated to the feature. Upgrade in a dedicated maintenance milestone. |
| cljs-ajax (replacing http-fx) | http-fx works, is integrated, is stable. Swapping HTTP libraries mid-project has no upside. |
| Server-side rendering (SSR) | The page is accessed after eligibility check (SPA navigation). SSR adds massive complexity for a page that isn't crawled independently. |
| Google Maps React wrapper (npm) | Adds npm dependency + React-Reagent interop overhead. Direct JS interop is already the pattern in this codebase and works cleanly. |

## Sources

- [Google Maps Deprecations](https://developers.google.com/maps/deprecations) -- Marker deprecation timeline
- [Advanced Markers Migration Guide](https://developers.google.com/maps/documentation/javascript/advanced-markers/migration) -- Migration steps
- [Advanced Markers Start Guide](https://developers.google.com/maps/documentation/javascript/advanced-markers/start) -- mapId requirements
- [Re-frame: Using Stateful JS Components](https://github.com/Day8/re-frame/blob/master/docs/Using-Stateful-JS-Components.md) -- Container/inner component pattern
- [Reagent on Clojars](https://clojars.org/reagent) -- Version 2.0.1 (latest)
- [Re-frame on Clojars](https://clojars.org/re-frame) -- Version 1.4.4 (latest)
- [Google Maps Circle example](https://developers.google.com/maps/documentation/javascript/examples/circle-simple) -- Circle class still supported
- Codebase: `frontend/src/app/components/google_map.cljs` -- Existing Form-3 pattern
- Codebase: `frontend/src/app/components/eligibility_form.cljs` -- Existing Maps + Re-frame integration
- Codebase: `backend/src/infrastructure/rest_api/network_handler.clj` -- Existing API pattern
