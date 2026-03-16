# Phase 1: Infrastructure - Research

**Researched:** 2026-03-16
**Domain:** Backend API endpoint + frontend route/state wiring (Clojure/ClojureScript)
**Confidence:** HIGH

## Summary

Phase 1 establishes the end-to-end plumbing for the network detail page: a public backend endpoint `GET /api/v1/networks/:id/detail` that aggregates network data, active productions, and stats; a frontend route `/reseau/:id` with Re-frame events/subs for loading state; and the missing `find-by-network-id` method on `ProductionRepo`. No new dependencies are needed. All work follows existing codebase patterns closely.

The critical security concern is data filtering: the public endpoint must never expose sensitive fields (IBAN, PRM, user-id). A `serialize-public-production` whitelist function is mandatory. The architecture research already provides detailed code patterns for every component.

**Primary recommendation:** Follow the existing 3-layer DDD pattern exactly. Add `find-by-network-id` to ProductionRepo, create `get-network-detail` scenario, wire the handler, then add the frontend route with route-driven data loading.

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| INFR-01 | Endpoint public `GET /api/v1/networks/:id/detail` retournant reseau + productions + stats agregees | Backend scenario aggregation pattern, handler wiring, serialization whitelist |
| INFR-02 | Route frontend `/reseau/:id` accessible apres verification d'eligibilite | Reitit parameterized route, router/navigated event extension, views.cljs dispatch |
| INFR-03 | Methode `find-by-network-id` sur ProductionRepo | Protocol extension, XTDB query with :where clause, in-memory filter implementation |
</phase_requirements>

## Standard Stack

### Core (No Changes)
| Library | Version | Purpose | Status |
|---------|---------|---------|--------|
| Clojure | 1.12 | Backend language | Existing |
| ClojureScript | 1.12.x | Frontend language | Existing |
| XTDB v1 | (RocksDB) | Database | Existing |
| Reagent | 1.2.0 | React wrapper | Existing |
| Re-frame | 1.3.0 | State management | Existing |
| Reitit | 0.7.2 | Routing (both ends) | Existing |
| day8.re-frame/http-fx | 0.2.4 | HTTP effects | Existing |
| Malli | (current) | Schema validation | Existing |
| Integrant | (current) | Component lifecycle | Existing |

### New Dependencies
**None.** Zero new libraries required. This phase is pure pattern application within the existing stack.

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Single aggregated endpoint | Three separate API calls | 3x HTTP requests, race conditions, loading complexity -- rejected |
| Backend stats computation | Frontend reduce over productions | Leaks all production data, wrong for public endpoint -- rejected |

## Architecture Patterns

### Recommended Project Structure (New Files Only)
```
backend/src/
  application/network_scenarios.clj      # ADD: get-network-detail function
  domain/production.clj                  # ADD: find-by-network-id to protocol
  infrastructure/xtdb/xtdb_production_repo.clj    # ADD: find-by-network-id impl
  infrastructure/in_memory_repo/mem_production_repo.clj  # ADD: find-by-network-id impl
  infrastructure/rest_api/network_handler.clj     # ADD: get-network-detail-handler + route
  infrastructure/rest_api/handler.clj             # MODIFY: pass production-repo + consumption-repo to network_handler/routes

frontend/src/app/
  routes.cljs                            # ADD: /reseau/:id route
  events.cljs                            # MODIFY: router/navigated to store path-params + dispatch fetch
  subs.cljs                              # ADD: network-detail subs
  db.cljs                                # ADD: network-detail state keys
  views.cljs                             # ADD: case for :page/network-detail
  pages/network_detail.cljs              # NEW: placeholder page component
  network_detail/events.cljs             # NEW: fetch/success/failure events
  network_detail/subs.cljs               # NEW: subscriptions

backend/test/
  application/network_scenarios_test.clj  # NEW: get-network-detail scenarios
  domain/production_test.clj              # ADD: find-by-network-id tests (if protocol test exists)
```

### Pattern 1: Scenario-Level Aggregation
**What:** `get-network-detail` in `application/network_scenarios.clj` orchestrates three repos (network, production, consumption) and computes derived stats (total kWc, energy mix).
**When to use:** Always when a page needs data from multiple domain entities.
**Example:**
```clojure
;; Source: architecture research + existing codebase pattern
(defn get-network-detail
  "Aggregate a public network with its productions and consumer count."
  [network-repo production-repo consumption-repo network-id]
  (let [network (network/find-by-id network-repo network-id)]
    (when-not network
      (throw (ex-info "Network not found" {:network-id network-id})))
    (when (not= :public (:network/lifecycle network))
      (throw (ex-info "Network not found" {:network-id network-id})))
    (let [productions    (production/find-by-network-id production-repo network-id)
          active-prods   (filterv #(= :active (:production/lifecycle %)) productions)
          consumer-count (consumption/count-by-network-id consumption-repo network-id)
          total-kwc      (reduce + 0 (map :production/installed-power active-prods))
          energy-counts  (frequencies (map :production/energy-type active-prods))
          energy-mix     (when (seq active-prods)
                           (into {} (map (fn [[k v]] [k (double (/ (* 100 v) (count active-prods)))])
                                        energy-counts)))]
      {:network        (serialize-public-network network)
       :productions    (mapv serialize-public-production active-prods)
       :consumer-count consumer-count
       :total-capacity-kwc total-kwc
       :energy-mix     (or energy-mix {})})))
```

### Pattern 2: Public Serialization Whitelist
**What:** Dedicated functions that `select-keys` only safe fields before sending to the client.
**When to use:** Any public endpoint exposing domain entities.
**Example:**
```clojure
(defn- serialize-public-production [p]
  (select-keys p [:production/id :production/energy-type
                  :production/installed-power :production/producer-address]))

(defn- serialize-public-network [n]
  (select-keys n [:network/id :network/name :network/center-lat
                  :network/center-lng :network/radius-km :network/lifecycle]))
```

### Pattern 3: Route-Driven Data Loading (Frontend)
**What:** The `:router/navigated` event must be extended to handle parameterized routes. Currently it receives only the page name keyword. For `/reseau/:id`, it also needs the route match data (path params).
**When to use:** Any page that needs server data based on URL parameters.
**Critical finding:** The current `routes.cljs` callback passes only the page name:
```clojure
;; CURRENT — only page keyword, no params
(fn [match _]
  (let [page (or (-> match :data :name) :page/home)]
    (rf/dispatch [:router/navigated page])))
```
This must be extended to also pass path-params:
```clojure
;; UPDATED — pass match data for parameterized routes
(fn [match _]
  (let [page   (or (-> match :data :name) :page/home)
        params (-> match :parameters :path)]
    (rf/dispatch [:router/navigated page params])))
```
And the `:router/navigated` event handler must dispatch the fetch:
```clojure
(rf/reg-event-fx :router/navigated
  (fn [{:keys [db]} [_ page-name path-params]]
    (cond-> {:db (cond-> (assoc db :router/current-page page-name)
                   (not= page-name :page/signup) (dissoc :eligibility/join-network))}
      (= page-name :page/network-detail)
      (assoc :dispatch [:network-detail/fetch (:id path-params)]))))
```

### Pattern 4: Handler Wiring (Dependency Injection)
**What:** The `network_handler/routes` function currently takes `[network-repo ec-repo]`. It must be extended to accept `production-repo` and `consumption-repo` for the detail endpoint.
**When to use:** Adding endpoints that need additional repos.
**Critical wiring in `handler.clj`:**
```clojure
;; CURRENT
(network-handler/routes network-repo ec-repo)
;; UPDATED
(network-handler/routes network-repo ec-repo production-repo consumption-repo)
```

### Anti-Patterns to Avoid
- **Frontend aggregation:** Never fetch network, productions, and consumption count in separate API calls from the frontend.
- **Domain cross-references:** Never import `domain.production` from `domain.network`. Aggregation belongs in the application layer.
- **Oversharing data:** Never return full production entities (with IBAN, PRM, user-id) on the public endpoint. Use `serialize-public-production` whitelist.
- **Client-side stats:** Never compute `reduce + installed-power` in ClojureScript. Stats are computed in the backend scenario.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP effects | Custom XHR wrapper | `day8.re-frame/http-fx` (existing) | Already integrated and working everywhere |
| Route matching | Manual URL parsing | Reitit frontend router (existing) | Handles params, history, etc. |
| JSON serialization | Manual response building | Muuntaja middleware (existing) | Already configured in handler.clj |
| Component lifecycle | Manual init/halt | Integrant (existing) | Already manages all repos |

## Common Pitfalls

### Pitfall 1: Sensitive Data Leak on Public Endpoint
**What goes wrong:** Returning full production entities exposes IBAN, PRM, user-id to unauthenticated visitors.
**Why it happens:** The scenario returns raw domain entities without filtering.
**How to avoid:** `serialize-public-production` with explicit `select-keys` whitelist. Test by inspecting the raw JSON response.
**Warning signs:** API response contains `:production/iban`, `:production/user-id`, or `:production/pdl-prm`.

### Pitfall 2: Non-Public Network Accessible
**What goes wrong:** The detail endpoint returns data for private or pending-validation networks.
**Why it happens:** Missing lifecycle guard in the scenario function.
**How to avoid:** Check `(:network/lifecycle network)` is `:public`. Return 404 (not 403) for non-public networks to avoid information leakage.
**Warning signs:** Requesting a private network ID returns 200 with data.

### Pitfall 3: Route Refresh Returns 404
**What goes wrong:** Direct navigation to `/reseau/some-uuid` returns a blank page or server 404.
**Why it happens:** The backend does not serve `index.html` for unknown frontend routes. SPA fallback may not cover `/reseau/*`.
**How to avoid:** Verify the backend's static file handler or catch-all route serves index.html for all frontend paths. Check Reitit default handler configuration.
**Warning signs:** Refreshing the page on `/reseau/:id` shows a blank page.

### Pitfall 4: Router Navigated Loses Path Params
**What goes wrong:** The network detail page loads but has no network ID -- the fetch event fires with `nil`.
**Why it happens:** Current `routes.cljs` callback only passes page name, not path params. The `:router/navigated` event ignores the match data.
**How to avoid:** Extend both the callback and the event handler to pass/store path params (see Pattern 3 above).
**Warning signs:** `:network-detail/fetch` dispatched with `nil` network-id.

### Pitfall 5: Stats NaN/Crash on Zero Productions
**What goes wrong:** A public network with zero active productions causes division by zero in energy-mix calculation, or `NaN` in total-capacity.
**Why it happens:** `(/ (* 100 v) (count active-prods))` when `active-prods` is empty.
**How to avoid:** Guard with `(when (seq active-prods) ...)`, return empty map for energy-mix and 0 for total-capacity.
**Warning signs:** Energy mix shows `NaN%` or page crashes.

### Pitfall 6: Network Handler Routes Signature Change
**What goes wrong:** Adding production-repo and consumption-repo params to `network_handler/routes` without updating the call site in `handler.clj`.
**Why it happens:** The function signature changes but the caller is in a different file.
**How to avoid:** Update `handler.clj` `build-router` at the same time. Both production-repo and consumption-repo are already available in the `ig/init-key :http/handler` destructuring.
**Warning signs:** Compilation error or wrong arity exception at startup.

## Code Examples

### XTDB Query for find-by-network-id
```clojure
;; Source: existing find-by-user-id pattern in xtdb_production_repo.clj
(find-by-network-id [_ network-id]
  (let [results (xt/q (xt/db node)
                      '{:find  [(pull e [*])]
                        :where [[e :production/network-id nid]]
                        :in    [nid]}
                      network-id)]
    (mapv (fn [[doc]] (doc->production doc)) results)))
```

### In-Memory find-by-network-id
```clojure
;; Source: existing find-by-user-id pattern in mem_production_repo.clj
(find-by-network-id [_ network-id]
  (vec (keep (fn [p]
               (when (= network-id (:production/network-id p))
                 (production/build-production p)))
             (vals @store))))
```

### Handler with Error Handling
```clojure
;; Source: existing handler patterns
(defn get-network-detail-handler
  "GET /api/v1/networks/:id/detail — aggregated network detail."
  [network-repo production-repo consumption-repo]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))]
        {:status 200
         :body   (scenarios/get-network-detail
                   network-repo production-repo consumption-repo network-id)})
      (catch clojure.lang.ExceptionInfo _e
        {:status 404
         :body   {:error "Network not found"}}))))
```

### Frontend Events (network_detail/events.cljs)
```clojure
;; Source: existing events.cljs patterns
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

(rf/reg-event-db :network-detail/fetch-ok
  (fn [db [_ response]]
    (-> db
        (assoc :network-detail/data response)
        (assoc :network-detail/loading? false))))

(rf/reg-event-db :network-detail/fetch-err
  (fn [db [_ error]]
    (-> db
        (assoc :network-detail/error error)
        (assoc :network-detail/loading? false))))
```

### Frontend Subscriptions (network_detail/subs.cljs)
```clojure
(rf/reg-sub :network-detail/data
  (fn [db _] (:network-detail/data db)))

(rf/reg-sub :network-detail/loading?
  (fn [db _] (:network-detail/loading? db)))

(rf/reg-sub :network-detail/error
  (fn [db _] (:network-detail/error db)))
```

### Placeholder Page Component
```clojure
;; Phase 1: empty but functional page
(defn network-detail-page []
  (let [loading? @(rf/subscribe [:network-detail/loading?])
        data     @(rf/subscribe [:network-detail/data])
        error    @(rf/subscribe [:network-detail/error])]
    [:div.network-detail
     (cond
       loading? [:p "Chargement..."]
       error    [:p "Erreur de chargement."]
       data     [:div
                 [:h1 (:network/name (:network data))]
                 [:p (str "Productions: " (count (:productions data)))]
                 [:p (str "Capacite: " (:total-capacity-kwc data) " kWc")]]
       :else    [:p "Aucune donnee."])]))
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Separate event files per module | Module-scoped event files (`network_detail/events.cljs`) | Existing pattern in `consumptions/events.cljs` | Follow existing convention |
| `reg-event-db` for router | `reg-event-fx` for router (when dispatch needed) | This phase | Enables route-driven data loading |

## Open Questions

1. **Production display name**
   - What we know: Productions have no dedicated display name field. They have `:production/producer-address` and `:production/energy-type`.
   - What's unclear: What to show as "nom/raison sociale" for each production on the detail page.
   - Recommendation: For Phase 1, serialize only energy-type + installed-power + producer-address. The display name question is a Phase 2 concern (CONT-01).

2. **`:public` lifecycle confirmation**
   - What we know: `network.clj` has `:public` in the lifecycle enum. The `list-networks` scenario already filters by `:public`.
   - What's unclear: STATE.md flags "confirmer que :public est le bon lifecycle state pour visible aux prospects".
   - Recommendation: HIGH confidence that `:public` is correct -- it's already used in the eligibility check flow. Use `:public` as the guard.

3. **SPA fallback route**
   - What we know: The app uses HTML5 history mode (`{:use-fragment false}`).
   - What's unclear: Whether the backend's default handler serves index.html for `/reseau/:id` paths.
   - Recommendation: Test during implementation. If 404 on refresh, add a catch-all route to the Ring handler.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Kaocha (via `clj -M:test`) |
| Config file | `backend/tests.edn` (or deps.edn :test alias) |
| Quick run command | `cd backend && clj -M:test --focus-meta :phase-1` |
| Full suite command | `cd backend && clj -M:test` |
| BDD helper | `bdd.test-gwt` (GIVEN/WHEN/THEN macro) |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INFR-01 | Endpoint returns network + productions + stats for public network | functional (GWT) | `cd backend && clj -M:test --focus application.network-scenarios-test` | Exists but needs new scenarios |
| INFR-01 | Endpoint returns 404 for non-public or missing network | functional (GWT) | Same as above | Wave 0 |
| INFR-01 | Response contains no sensitive fields | functional (GWT) | Same as above | Wave 0 |
| INFR-03 | find-by-network-id returns productions for a network | unit | `cd backend && clj -M:test --focus domain.production-test` | Exists but needs new tests |
| INFR-02 | Frontend route exists and triggers fetch | manual | Browser: navigate to `/reseau/:id`, check network tab | Manual-only (no frontend test infra) |
| INFR-02 | Re-frame state populated after fetch | manual | Browser: inspect Re-frame app-db via devtools | Manual-only |

### Sampling Rate
- **Per task commit:** `cd backend && clj -M:test`
- **Per wave merge:** Full backend test suite
- **Phase gate:** All backend tests green + manual browser verification of INFR-02

### Wave 0 Gaps
- [ ] `backend/test/application/network_scenarios_test.clj` -- ADD scenarios for `get-network-detail` (public network, non-public 404, missing 404, no productions, sensitive field exclusion)
- [ ] No frontend test infrastructure exists -- INFR-02 verification is manual (browser + devtools)

## Sources

### Primary (HIGH confidence)
- Codebase: `backend/src/domain/production.clj` -- ProductionRepo protocol (lines 170-175), no `find-by-network-id`
- Codebase: `backend/src/domain/network.clj` -- Network schema, lifecycle states (`:private :public :pending-validation`)
- Codebase: `backend/src/domain/consumption.clj` -- ConsumptionRepo protocol with `count-by-network-id` (line 193)
- Codebase: `backend/src/application/network_scenarios.clj` -- existing scenario patterns
- Codebase: `backend/src/infrastructure/rest_api/network_handler.clj` -- existing handler pattern, routes signature
- Codebase: `backend/src/infrastructure/rest_api/handler.clj` -- build-router wiring, all repos available
- Codebase: `backend/src/infrastructure/xtdb/xtdb_production_repo.clj` -- XTDB query pattern for `find-by-user-id`
- Codebase: `frontend/src/app/routes.cljs` -- current router (no parameterized routes)
- Codebase: `frontend/src/app/events.cljs` -- `:router/navigated` receives only page name
- Codebase: `frontend/src/app/views.cljs` -- `current-page` case dispatch
- Codebase: `frontend/src/app/db.cljs` -- default-db structure
- `.planning/research/ARCHITECTURE.md` -- detailed architecture patterns
- `.planning/research/PITFALLS.md` -- comprehensive pitfall analysis
- `.planning/research/STACK.md` -- stack analysis confirming zero new dependencies

### Secondary (MEDIUM confidence)
- `.planning/STATE.md` -- project decisions and blockers

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- zero new dependencies, all existing patterns
- Architecture: HIGH -- detailed code patterns verified against actual codebase
- Pitfalls: HIGH -- verified against actual codebase structure and existing patterns
- Frontend routing: HIGH -- verified current `routes.cljs` only handles static routes, extension pattern is clear

**Research date:** 2026-03-16
**Valid until:** 2026-04-16 (stable codebase, no external dependency changes)
