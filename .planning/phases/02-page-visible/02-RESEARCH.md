# Phase 2: Page Visible - Research

**Researched:** 2026-03-16
**Domain:** Frontend ClojureScript/Re-frame page with Google Maps, stats display, production list, CTA
**Confidence:** HIGH

## Summary

Phase 2 transforms the placeholder network detail page (created in Phase 1) into a fully functional page displaying a Google Maps card with network circle, statistics (capacity, energy mix), a production list, generated description text, and a CTA button. All infrastructure is already in place: the API endpoint `GET /api/v1/networks/:id/detail` returns aggregated data, the route `/reseau/:id` is wired, Re-frame events/subs are operational, and the placeholder page renders data from `app-db`.

The work is purely frontend: replacing the placeholder `network-detail-page` component with real UI components. No new libraries are needed. The Google Maps interop code must be extracted from the duplicated implementations in `google_map.cljs` and `eligibility_form.cljs` into a shared namespace before building the detail page map. The map component must use Form-3 (`r/create-class`) as decided in Phase 1.

**Primary recommendation:** Extract shared Google Maps utilities first, then build the page as independent sub-components (map, stats, production list, description, CTA) each with their own subscriptions to avoid unnecessary re-renders.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| MAP-01 | Carte interactive affichant la zone du reseau (cercle centre sur les coordonnees) | Form-3 map component centered on network coords, circle via `google.maps.Circle`, `fitBounds` for auto-zoom. Existing pattern in `google_map.cljs`. |
| MAP-02 | Marqueurs des sites de production positionnes sur la carte | Productions have `producer-address` (string) but no lat/lng. Use Option B from architecture research: list productions below map, no markers on map for v1. |
| STAT-01 | Capacite totale installee du reseau affichee en kWc | Data already in API response as `:total-capacity-kwc`. Pure display component. |
| STAT-02 | Mix energetique affiche en % par type d'energie | Data already in API response as `:energy-mix` map. CSS-only percentage bars, no chart library needed. |
| CONT-01 | Liste des productions avec nom/raison sociale, type d'energie et localisation | Productions in API response with `:production/energy-type`, `:production/installed-power`, `:production/producer-address`. Note: no dedicated display name exists -- use energy-type + address as fallback. |
| CONT-02 | Description generee du reseau (style "Rejoignez l'Operation d'ACC a [ville]") | Generate from network name. Pure frontend string interpolation. |
| CONT-03 | Bouton CTA vers le parcours d'inscription / test d'eligibilite | Link to `:page/signup` or scroll to `#eligibility` on home page. Follow existing `rfee/href` pattern. |
</phase_requirements>

## Standard Stack

### Core (No Changes)
| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| ClojureScript | 1.12.x | Frontend language | Existing |
| Reagent | 1.2.0 | React wrapper | Existing |
| Re-frame | 1.3.0 | State management | Existing |
| shadow-cljs | 2.28.14 | Build toolchain | Existing |
| Google Maps JS API | v3 (latest) | Map rendering | Existing, loaded via script tag |

### No New Dependencies

Zero new ClojureScript or npm dependencies needed. Everything is achievable with existing stack + CSS.

## Architecture Patterns

### Recommended Project Structure

```
frontend/src/app/
  utils/
    google_maps.cljs        # NEW: extracted shared maps interop
  components/
    google_map.cljs         # MODIFIED: delegate to utils/google_maps
    eligibility_form.cljs   # MODIFIED: delegate to utils/google_maps
  network_detail/
    events.cljs             # EXISTS (Phase 1)
    subs.cljs               # EXISTS (Phase 1) — add derived subs
  pages/
    network_detail.cljs     # EXISTS (placeholder) — replace with full page
```

### Pattern 1: Form-3 Map Component with Single Network Focus

**What:** A Form-3 (`r/create-class`) component that renders a Google Map centered on a single network, draws the network circle, and uses `fitBounds` for automatic zoom.

**When to use:** For the network detail map (MAP-01).

**Key decision (from STATE.md):** Use Form-3 (`r/create-class`) for the map component -- never conditionally render the map div.

**Example:**
```clojure
(defn network-detail-map
  "Form-3: Google Map showing a single network circle."
  [_network _productions]
  (let [map-el   (atom nil)
        map-inst (atom nil)
        circle   (atom nil)]
    (r/create-class
     {:display-name "network-detail-map"

      :component-did-mount
      (fn [this]
        (google-maps/load-google-maps-script!
         (fn []
           (let [[_ network _] (r/argv this)
                 center #js {:lat (:network/center-lat network)
                             :lng (:network/center-lng network)}
                 gmap (js/google.maps.Map.
                        @map-el
                        #js {:center center :zoom 13 :mapTypeId "roadmap"})]
             (reset! map-inst gmap)
             ;; Draw network circle
             (reset! circle
                     (google-maps/draw-circle! gmap
                       {:center-lat (:network/center-lat network)
                        :center-lng (:network/center-lng network)
                        :radius-km  (:network/radius-km network)}))
             ;; Fit bounds to circle
             (when @circle
               (.fitBounds gmap (.getBounds @circle)))))))

      :component-will-unmount
      (fn [_]
        (when @circle (.setMap @circle nil)))

      :reagent-render
      (fn [_ _]
        [:div.nd-map-container
         {:ref   (fn [el] (when el (reset! map-el el)))
          :style {:width "100%" :aspect-ratio "4/3"
                  :min-height "300px" :max-height "500px"}}])})))
```

### Pattern 2: Derived Subscriptions for Display Data

**What:** Create derived Re-frame subscriptions that extract and format display data from the raw API response.

**When to use:** Stats, production list, network info -- each component subscribes to its own derived sub.

**Example:**
```clojure
;; In network_detail/subs.cljs
(rf/reg-sub :network-detail/network
  :<- [:network-detail/data]
  (fn [data _] (:network data)))

(rf/reg-sub :network-detail/productions
  :<- [:network-detail/data]
  (fn [data _] (:productions data)))

(rf/reg-sub :network-detail/stats
  :<- [:network-detail/data]
  (fn [data _]
    (when data
      {:total-capacity-kwc (:total-capacity-kwc data)
       :energy-mix         (:energy-mix data)
       :consumer-count     (:consumer-count data)
       :production-count   (count (:productions data))})))
```

### Pattern 3: Independent Sub-Components

**What:** Each section of the page is its own Form-1 function subscribing only to what it needs.

**Why:** Avoids re-rendering the entire page (especially the map) when unrelated data changes.

**Structure:**
```clojure
(defn network-detail-page []
  (let [loading? @(rf/subscribe [:network-detail/loading?])
        error    @(rf/subscribe [:network-detail/error])
        data     @(rf/subscribe [:network-detail/data])]
    (cond
      loading? [:div.nd-page [:p "Chargement..."]]
      error    [:div.nd-page [:p "Erreur de chargement."]]
      data     [:div.nd-page
                [network-hero]       ;; description + CTA
                [network-stats]      ;; stats cards
                [network-map-section] ;; map with circle
                [production-list]    ;; production cards
                [join-cta]]          ;; bottom CTA
      :else    [:div.nd-page [:p "Aucune donnee."]])))
```

### Anti-Patterns to Avoid

- **Conditional map div:** Never wrap the map container div in `when`/`if`. The div must always be rendered for Google Maps to function correctly.
- **Storing Google Maps objects in app-db:** Google Maps instances, circles, and markers are mutable JS objects. Store them in local atoms only.
- **Client-side geocoding for production markers:** Productions have string addresses, not coordinates. Do NOT geocode N addresses on the client. List productions below the map instead.
- **Single monolithic component:** Do NOT put all subscriptions in one top-level `let` block. Split into sub-components so each re-renders independently.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Energy mix visualization | Canvas/SVG chart | CSS flexbox with colored segments | Simple percentages, no interactivity needed. CSS is sufficient and adds zero bundle size. |
| Map zoom calculation | Manual zoom-level-from-radius math | `map.fitBounds(circle.getBounds())` | Google Maps API handles this correctly for any radius. |
| Production display names | Complex join logic with user data | Concatenation of energy-type + producer-address | No dedicated label field exists. Keep it simple for v1. |
| Map script loading | New script loader | Existing `load-google-maps-script!` | Already handles deduplication, polling, and error cases. |

## Common Pitfalls

### Pitfall 1: Map Grey Rectangle After Navigation
**What goes wrong:** Navigating away from the detail page and back shows a grey map container.
**Why it happens:** The map instance is destroyed on unmount but the script is already loaded. On re-mount, the component needs to re-create the map instance.
**How to avoid:** Always create a new map instance in `component-did-mount`. The existing `load-google-maps-script!` handles the "already loaded" case by calling the callback immediately.
**Warning signs:** Grey rectangle, no console error.

### Pitfall 2: Map Container with Zero Height
**What goes wrong:** The map div has no content (Google Maps injects asynchronously), so it collapses to 0px height, and the map never appears.
**How to avoid:** Give the map container explicit dimensions via CSS (`min-height: 300px`, `aspect-ratio: 4/3`). Already established in existing components.

### Pitfall 3: Energy Mix Division by Zero
**What goes wrong:** Network with zero active productions returns `{}` for energy-mix and `0` for total-capacity-kwc. The UI shows "NaN%" or crashes.
**How to avoid:** The backend already handles this (returns `{}` for energy-mix, `0` for capacity). The frontend must guard: if energy-mix is empty, show "Aucune production active" instead of rendering empty bars.

### Pitfall 4: Stale Data When Navigating Between Networks
**What goes wrong:** User views network A, navigates to network B, briefly sees network A data.
**How to avoid:** Already handled in Phase 1 -- `router/navigated` dissocs `:network-detail/data` and `:network-detail/error` before dispatching the new fetch.

### Pitfall 5: CTA Link Target Ambiguity
**What goes wrong:** The CTA button links to signup but the user is not yet eligible (arrived via direct URL).
**How to avoid:** Two CTA options: (1) if user arrived after eligibility check and is eligible, link directly to signup with network context; (2) if user arrived via direct URL, link to home page eligibility section `/#eligibility`.

## Code Examples

### API Response Shape (from backend `get-network-detail`)

```json
{
  "network": {
    "network/id": "uuid",
    "network/name": "Reseau Solaire de Lyon",
    "network/center-lat": 45.764,
    "network/center-lng": 4.8357,
    "network/radius-km": 2.0,
    "network/lifecycle": "public"
  },
  "productions": [
    {
      "production/id": "uuid",
      "production/energy-type": "solar",
      "production/installed-power": 36.0,
      "production/producer-address": "12 rue Example, Lyon"
    }
  ],
  "consumer-count": 42,
  "total-capacity-kwc": 156.0,
  "energy-mix": {"solar": 75.0, "wind": 25.0}
}
```

Note: Keywords are transformed by `ajax/json-response-format {:keywords? true}` -- nested keys become `:network/name` etc.

### Shared Google Maps Utility (extraction target)

```clojure
;; app/utils/google_maps.cljs
(ns app.utils.google-maps
  (:require [app.config :as config]))

;; Move load-google-maps-script! here from google_map.cljs
;; Move draw-circle! helper here (extracted from draw-circles!/draw-network-circles!)

(defn draw-circle!
  "Draws a single circle on the map. Returns the Circle instance."
  [gmap {:keys [center-lat center-lng radius-km
                stroke-color stroke-weight fill-color fill-opacity]}]
  (js/google.maps.Circle.
   #js {:map         gmap
        :center      #js {:lat center-lat :lng center-lng}
        :radius      (* radius-km 1000)
        :strokeColor  (or stroke-color "#2e7d32")
        :strokeWeight (or stroke-weight 2)
        :fillColor    (or fill-color "#4caf50")
        :fillOpacity  (or fill-opacity 0.2)}))

(defn clear-overlays!
  "Removes all overlays from the map by calling .setMap nil."
  [overlays-atom]
  (doseq [o @overlays-atom] (.setMap o nil))
  (reset! overlays-atom []))
```

### Energy Mix CSS Bar

```clojure
;; Pure CSS energy mix display -- no chart library
(defn energy-mix-bar [energy-mix]
  (let [colors {:solar "#f5aa46" :wind "#64917d" :hydro "#4a90d9"
                :biomass "#8b6914" :other "#999999"}]
    [:div.nd-mix-bar
     (for [[energy-type pct] (sort-by val > energy-mix)]
       ^{:key energy-type}
       [:div.nd-mix-segment
        {:style {:width (str pct "%")
                 :background (get colors energy-type "#999")}}])]))
```

```css
.nd-mix-bar {
  display: flex;
  height: 24px;
  border-radius: var(--radius);
  overflow: hidden;
  background: var(--color-border);
}
.nd-mix-segment {
  transition: width 0.3s ease;
}
```

### Generated Description Text

```clojure
(defn- network-description [network stats]
  (let [name (:network/name network)
        prod-count (:production-count stats)
        capacity (:total-capacity-kwc stats)]
    [:p.nd-description
     "Rejoignez l'operation d'autoconsommation collective "
     [:strong name]
     ". Ce reseau reunit " prod-count " site"
     (when (> prod-count 1) "s")
     " de production pour une capacite totale de "
     [:strong (str capacity " kWc")] "."]))
```

## State of the Art

| Aspect | Current State in Codebase | Needed for Phase 2 |
|--------|---------------------------|---------------------|
| Map component | Two near-identical implementations (google_map.cljs, eligibility_form.cljs) | Extract shared utils, build detail-specific component |
| Network detail page | Placeholder with raw data dump | Full page with map, stats, list, CTA |
| Re-frame subs | Basic layer 2 subs (direct db access) | Add derived layer 3 subs for display data |
| CSS | No `.nd-*` classes exist | Add network detail page styles to main.css |
| Google Maps Marker | Uses deprecated `google.maps.Marker` in eligibility form | Keep legacy Marker for now (no markers needed on detail map in v1) |

## Open Questions

1. **Production display name**
   - What we know: Productions have `:production/energy-type` and `:production/producer-address` but no dedicated display name/label field.
   - What's unclear: What should show in the production list as the "name"?
   - Recommendation: Use energy type label (e.g., "Solaire") as the primary identifier, with address as secondary info. This matches the API's public serialization which only exposes type, power, and address.

2. **CTA destination for direct URL visitors**
   - What we know: After eligibility check, user has network context. Via direct URL, they don't.
   - What's unclear: Should the CTA always go to signup, or to eligibility check first?
   - Recommendation: Link to home page `/#eligibility` for the general case. The eligibility flow already handles the journey to signup.

3. **MAP-02 interpretation (production markers)**
   - What we know: Productions have string addresses, not lat/lng coordinates. Client-side geocoding is an anti-pattern.
   - Recommendation: Satisfy MAP-02 by listing productions below the map with their addresses. The requirement says "or listed below the map if coordinates are not available" -- this is the case.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Kaocha (backend) / shadow-cljs test (frontend) |
| Config file | backend: `tests.edn` / frontend: `shadow-cljs.edn` |
| Quick run command | `cd backend && clj -M:test` |
| Full suite command | `cd backend && clj -M:test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| MAP-01 | Map renders with circle at network coords | manual-only | Visual verification in browser | N/A |
| MAP-02 | Productions listed below map | manual-only | Visual verification in browser | N/A |
| STAT-01 | Capacity displayed matches API data | manual-only | Compare browser display with API response | N/A |
| STAT-02 | Energy mix percentages match API data | manual-only | Compare browser display with API response | N/A |
| CONT-01 | Production list shows type, power, address | manual-only | Visual verification in browser | N/A |
| CONT-02 | Generated description includes network name | manual-only | Visual verification in browser | N/A |
| CONT-03 | CTA button navigates correctly | manual-only | Click test in browser | N/A |

### Sampling Rate
- **Per task commit:** Visual inspection in dev browser
- **Per wave merge:** Full backend test suite + visual verification
- **Phase gate:** All 7 requirements visually verified

### Wave 0 Gaps
None -- this phase is purely frontend UI components. Backend tests from Phase 1 already validate the API contract. Frontend components are visual and require manual verification. No new automated test infrastructure needed.

## Sources

### Primary (HIGH confidence)
- Codebase analysis: `frontend/src/app/components/google_map.cljs` -- existing Form-3 map pattern, draw-circles!
- Codebase analysis: `frontend/src/app/components/eligibility_form.cljs` -- existing Form-3 map + circles
- Codebase analysis: `backend/src/application/network_scenarios.clj` -- exact API response shape
- Codebase analysis: `frontend/src/app/network_detail/events.cljs` + `subs.cljs` -- existing Re-frame wiring
- Codebase analysis: `frontend/public/css/main.css` -- CSS variables, existing component patterns
- Phase 1 summaries: `01-01-SUMMARY.md`, `01-02-SUMMARY.md` -- completed infrastructure

### Secondary (MEDIUM confidence)
- Architecture research: `.planning/research/ARCHITECTURE.md` -- component boundaries, Option B for markers
- Pitfalls research: `.planning/research/PITFALLS.md` -- Form-3 lifecycle, overlay cleanup
- Stack research: `.planning/research/STACK.md` -- no new dependencies, AdvancedMarkerElement recommendation (deferred)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- no new dependencies, all existing code verified
- Architecture: HIGH -- existing patterns clear, API contract established, component boundaries well-defined
- Pitfalls: HIGH -- verified against actual codebase patterns and Phase 1 decisions

**Research date:** 2026-03-16
**Valid until:** 2026-04-16 (stable -- no external dependency changes expected)
