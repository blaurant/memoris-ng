# Architecture Patterns

**Domain:** Network detail page with map, stats, and production list
**Researched:** 2026-03-16

## Recommended Architecture

The network detail page fits cleanly into the existing 3-layer DDD architecture. It requires one new backend endpoint, one new frontend page, and a new Re-frame module -- no new domain entities or repos.

### High-Level Data Flow

```
[User clicks "Voir le reseau" after eligibility check]
        |
        v
[Frontend: route /reseau/:id triggers :network-detail/fetch]
        |
        v
[Re-frame event-fx -> HTTP GET /api/v1/networks/:id/detail]
        |
        v
[Backend: network_handler -> network_scenarios -> repos]
        |  Aggregates:
        |  - network (from network-repo)
        |  - productions for this network (from production-repo)
        |  - consumer count (from consumption-repo)
        v
[Backend returns aggregated JSON response]
        |
        v
[Re-frame: stores in app-db under :network-detail/*]
        |
        v
[Frontend page: renders map + stats + production list + CTA]
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| `domain/network.clj` | Network entity, schema, lifecycle. **No changes needed.** | -- |
| `domain/production.clj` | Production entity, schema. **No changes needed.** | -- |
| `domain/consumption.clj` | Consumption entity. Already has `count-by-network-id` in repo protocol. **No changes needed.** | -- |
| `application/network_scenarios.clj` | **New function:** `get-network-detail` aggregating network + productions + consumer count. | `domain.network/NetworkRepo`, `domain.production/ProductionRepo`, `domain.consumption/ConsumptionRepo` |
| `infrastructure/rest_api/network_handler.clj` | **New handler:** `GET /api/v1/networks/:id/detail`. Serializes aggregated data. Public (no auth). | `application.network-scenarios` |
| `infrastructure/xtdb/xtdb_production_repo.clj` | **New method:** `find-by-network-id` query. | XTDB node |
| `frontend: app/pages/network_detail.cljs` | **New file.** Page component: map, stats cards, production list, CTA button. | Re-frame subs |
| `frontend: app/network_detail/events.cljs` | **New file.** Fetch event, success/failure handlers. | HTTP API |
| `frontend: app/network_detail/subs.cljs` | **New file.** Subscriptions for network-detail state. | app-db |
| `frontend: app/routes.cljs` | **Add route:** `["/reseau/:id" {:name :page/network-detail}]` | Reitit |
| `frontend: app/views.cljs` | **Add case** in `current-page` for `:page/network-detail` | network-detail page |

## Detailed Architecture

### Backend: New Scenario Function

The aggregation belongs in `application/network_scenarios.clj` because it orchestrates multiple repos (network, production, consumption) -- a classic use-case.

```clojure
;; in application/network_scenarios.clj

(defn get-network-detail
  "Aggregate a public network with its productions and consumer count.
   Returns a map with :network, :productions, :consumer-count, :total-capacity-kwc,
   and :energy-mix."
  [network-repo production-repo consumption-repo network-id]
  (let [network (network/find-by-id network-repo network-id)]
    (when-not network
      (throw (ex-info "Network not found" {:network-id network-id})))
    (when (not= :public (:network/lifecycle network))
      (throw (ex-info "Network is not public" {:network-id network-id})))
    (let [productions    (production/find-by-network-id production-repo network-id)
          active-prods   (filterv #(= :active (:production/lifecycle %)) productions)
          consumer-count (consumption/count-by-network-id consumption-repo network-id)
          total-kwc      (reduce + 0 (map :production/installed-power active-prods))
          energy-counts  (frequencies (map :production/energy-type active-prods))
          energy-mix     (into {} (map (fn [[k v]] [k (double (/ (* 100 v) (count active-prods)))])
                                      energy-counts))]
      {:network        network
       :productions    active-prods
       :consumer-count consumer-count
       :total-capacity-kwc total-kwc
       :energy-mix     energy-mix})))
```

**Key design decisions:**
- Only active productions are exposed (not onboarding/pending ones)
- Consumer count comes from the existing `count-by-network-id` protocol method
- Energy mix and total capacity are computed in the scenario (derived data, not stored)
- Guard: only public networks are accessible

### Backend: New Repo Method

The `ProductionRepo` protocol needs a `find-by-network-id` method. This is the only domain protocol change.

```clojure
;; Add to domain/production.clj protocol:
(find-by-network-id [repo network-id] "Find all productions linked to a network.")
```

XTDB implementation:

```clojure
;; in xtdb_production_repo.clj
(find-by-network-id [_ network-id]
  (let [results (xt/q (xt/db node)
                      '{:find  [(pull e [*])]
                        :where [[e :production/network-id nid]]
                        :in    [nid]}
                      network-id)]
    (mapv (fn [[doc]] (doc->production doc)) results)))
```

### Backend: New REST Endpoint

```clojure
;; Add to network_handler.clj

(defn get-network-detail-handler
  "GET /api/v1/networks/:id/detail — returns aggregated network detail."
  [network-repo production-repo consumption-repo]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))]
        {:status 200
         :body   (scenarios/get-network-detail
                   network-repo production-repo consumption-repo network-id)})
      (catch clojure.lang.ExceptionInfo e
        {:status (if (.contains (.getMessage e) "not found") 404 403)
         :body   {:error (.getMessage e)}}))))
```

**Route:** `["/api/v1/networks/:id/detail" {:get handler}]` -- public, no auth middleware.

**Note:** The handler needs `production-repo` and `consumption-repo` injected. The `routes` function signature in `network_handler.clj` must be extended. This also means `handler.clj` (the main router) must pass these repos through.

### Backend: Serialization

The response JSON shape:

```json
{
  "network": {
    "network/id": "uuid-string",
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
      "production/producer-address": "12 rue Example"
    }
  ],
  "consumer-count": 42,
  "total-capacity-kwc": 156.0,
  "energy-mix": {"solar": 75.0, "wind": 25.0}
}
```

Productions should be stripped of sensitive fields (user-id, IBAN, PRM, linky-meter) before serialization. Use a `serialize-public-production` function that only exposes: id, energy-type, installed-power, producer-address.

### Frontend: Re-frame State

```clojure
;; Add to app/db.cljs default-db:
:network-detail/data     nil      ;; the aggregated response
:network-detail/loading? false
:network-detail/error    nil
```

### Frontend: Events

```clojure
;; app/network_detail/events.cljs

(rf/reg-event-fx :network-detail/fetch
  (fn [{:keys [db]} [_ network-id]]
    {:db         (-> db
                     (assoc :network-detail/loading? true)
                     (assoc :network-detail/error nil))
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/networks/" network-id "/detail")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:network-detail/fetch-ok]
                  :on-failure      [:network-detail/fetch-err]}}))
```

### Frontend: Route with Data Loading

The route entry must trigger the fetch on navigation:

```clojure
;; In app/routes.cljs
["/reseau/:id" {:name :page/network-detail}]
```

The `:router/navigated` event handler should dispatch `:network-detail/fetch` when the page is `:page/network-detail`. Alternatively, dispatch from the page component's mount. The existing pattern in the codebase uses event dispatch from the router callback -- follow that pattern.

### Frontend: Page Component Structure

```
network-detail-page
  |-- network-hero-section (name, description text, CTA)
  |-- network-stats-cards (consumer-count, production-count, total-kwc, energy-mix)
  |-- network-map (Google Maps with circle + production markers)
  |-- production-list (table/cards of productions)
  |-- join-cta-section (button -> signup/eligibility)
```

**Map component:** Reuse `app.components.google-map/load-google-maps-script!` for loading. Create a new map component specific to the detail page that:
1. Centers on the network center
2. Draws the network circle (reuse `draw-circles!` pattern)
3. Adds markers for each production location (geocoded from producer-address)

**Important:** Production addresses are strings, not lat/lng. Two options:
- **Option A (recommended):** Geocode production addresses on the backend at save time. Store lat/lng in the production entity. This avoids client-side geocoding N addresses.
- **Option B (simpler, MVP):** Skip production markers on the map. Show only the network circle. List productions below as cards without map pins. This avoids adding fields to the production entity.

**Recommendation:** Go with Option B for the first implementation. Production markers can be added later when lat/lng is stored on productions.

### Frontend: Navigation Entry Point

After eligibility check returns `eligible? true`, the result already contains the network data including its ID. Add a link/button in the eligibility form result:

```clojure
;; In eligibility_form.cljs, after "Bonne nouvelle"
[:a.btn.btn--outline
 {:href (rfee/href :page/network-detail {:id (str (:network/id network))})}
 "Decouvrir le reseau"]
```

## Patterns to Follow

### Pattern 1: Scenario-Level Aggregation

**What:** Aggregate data from multiple repos in the application layer, not in handlers or domain.
**When:** A page needs data from multiple entities (network + productions + consumptions).
**Why:** The handler stays thin (HTTP concerns only). The domain stays pure (no cross-entity knowledge). The scenario orchestrates.

### Pattern 2: Public Serialization (Data Filtering)

**What:** Create dedicated serialization functions that strip sensitive fields before sending to the client.
**When:** Exposing entity data on public endpoints.
**Example:**
```clojure
(defn serialize-public-production [p]
  (select-keys p [:production/id :production/energy-type
                  :production/installed-power :production/producer-address]))
```

### Pattern 3: Route-Driven Data Loading

**What:** Dispatch data-fetch events when route changes, not in component mount.
**When:** Pages that need server data on load.
**Why:** Consistent with how the app already works. Avoids double-fetch on re-renders.

### Pattern 4: Namespaced Re-frame Module

**What:** Group events, subs, and page for network-detail in their own namespace.
**When:** Adding a new feature area.
**Example:** `app/network_detail/events.cljs`, `app/network_detail/subs.cljs`, `app/pages/network_detail.cljs`

## Anti-Patterns to Avoid

### Anti-Pattern 1: Frontend Aggregation

**What:** Fetching network, productions, and consumption count in separate API calls from the frontend, then combining.
**Why bad:** 3 HTTP requests instead of 1. Race conditions. Loading state complexity. The backend already has access to all repos.
**Instead:** Single aggregated endpoint `GET /api/v1/networks/:id/detail`.

### Anti-Pattern 2: Domain Cross-References

**What:** Having `domain/network.clj` import `domain/production.clj` to compute stats.
**Why bad:** Violates DDD -- entities should not know about each other's internals. Creates circular dependency risk.
**Instead:** Aggregation in the application layer scenario.

### Anti-Pattern 3: Geocoding in the Frontend for N Productions

**What:** Client-side geocoding each production address to place markers.
**Why bad:** Google Maps Geocoding API has rate limits. N productions = N async calls on page load. Slow, unreliable, expensive.
**Instead:** Skip markers initially (Option B) or store lat/lng at production creation time.

### Anti-Pattern 4: Oversharing Data on Public Endpoints

**What:** Returning full production entities (with IBAN, PRM, user-id) on a public page.
**Why bad:** Sensitive financial and personal data exposed.
**Instead:** Use `serialize-public-production` to whitelist safe fields.

## Build Order (Dependencies)

The components have the following dependency chain:

```
Phase 1 — Backend Foundation
  1a. Add find-by-network-id to ProductionRepo protocol + XTDB + in-memory implementations
  1b. Add get-network-detail scenario in network_scenarios.clj
  1c. Add GET /api/v1/networks/:id/detail handler + route
  1d. Wire new repos into handler.clj route builder

Phase 2 — Frontend Core
  2a. Add route /reseau/:id in routes.cljs
  2b. Add network-detail state to db.cljs
  2c. Create events.cljs and subs.cljs for network-detail
  2d. Add page dispatch in views.cljs

Phase 3 — Frontend Page
  3a. Create network_detail.cljs page with stats + production list
  3b. Add Google Maps component (network circle, centered on network)
  3c. Add navigation link from eligibility form result

Phase 4 — Content & Polish
  4a. Generate descriptive text ("Rejoignez l'operation...")
  4b. Style the page (responsive, consistent with existing design)
  4c. Add CTA button linking to signup flow
```

**Why this order:**
- Phase 1 can be fully TDD'd with no frontend dependencies
- Phase 2 is structural wiring (routing, state shape) -- needed before Phase 3
- Phase 3 is the visible page -- needs backend API and frontend wiring in place
- Phase 4 is content and styling -- independent of logic, can iterate freely

## Scalability Considerations

| Concern | At 10 networks | At 100 networks | At 1000 networks |
|---------|----------------|-----------------|-------------------|
| Detail page load | No concern | No concern | No concern (single network query) |
| Productions per network | No concern | May want pagination | Paginate production list |
| Consumer count query | No concern | No concern | Index on :consumption/network-id in XTDB |
| Map rendering | No concern | No concern | Cluster markers if >50 productions |

For the foreseeable scale of this application (local energy networks), none of these are blockers.

## Sources

- Existing codebase analysis (primary source -- HIGH confidence)
- DDD layering conventions from `.claude/CLAUDE_BACK.md`
- Re-frame patterns from `.claude/CLAUDE_FRONT.md`
- Existing patterns: `network_handler.clj`, `production_handler.clj`, `google_map.cljs`, `eligibility_form.cljs`
