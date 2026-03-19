(ns app.pages.network-detail
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.utils.google-maps :as google-maps]
            [reitit.frontend.easy :as rfee]))

;; ── Helpers ─────────────────────────────────────────────────────────────────

(defn- name-or-str [k]
  (if (keyword? k) (name k) (str k)))

(def ^:private energy-type-labels
  {"solar"         "Solaire"
   "wind"          "Eolien"
   "hydro"         "Hydro"
   "biomass"       "Biomasse"
   "cogeneration"  "Cogeneration"})

(defn- energy-type-label [k]
  (get energy-type-labels (name-or-str k) "Autre"))

(def ^:private energy-mix-colors
  {"solar"         "#f5aa46"
   "wind"          "#64917d"
   "hydro"         "#4a90d9"
   "biomass"       "#8b6914"
   "cogeneration"  "#a0522d"})

(defn- energy-mix-color [k]
  (get energy-mix-colors (name-or-str k) "#999999"))

;; ── Energy icons ───────────────────────────────────────────────────────────

(defn- energy-icon [energy-type]
  (let [etype (name-or-str energy-type)]
    (case etype
      "solar"
      [:svg {:width "20" :height "20" :viewBox "0 0 24 24" :fill "none"
             :stroke "#f5aa46" :stroke-width "2"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:circle {:cx "12" :cy "12" :r "5"}]
       [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "3"}]
       [:line {:x1 "12" :y1 "21" :x2 "12" :y2 "23"}]
       [:line {:x1 "4.22" :y1 "4.22" :x2 "5.64" :y2 "5.64"}]
       [:line {:x1 "18.36" :y1 "18.36" :x2 "19.78" :y2 "19.78"}]
       [:line {:x1 "1" :y1 "12" :x2 "3" :y2 "12"}]
       [:line {:x1 "21" :y1 "12" :x2 "23" :y2 "12"}]
       [:line {:x1 "4.22" :y1 "19.78" :x2 "5.64" :y2 "18.36"}]
       [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]]
      "wind"
      [:svg {:width "20" :height "20" :viewBox "0 0 24 24" :fill "none"
             :stroke "#64917d" :stroke-width "2"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:path {:d "M9.59 4.59A2 2 0 1 1 11 8H2"}]
       [:path {:d "M12.59 19.41A2 2 0 1 0 14 16H2"}]
       [:path {:d "M17.73 7.73A2.5 2.5 0 1 1 19.5 12H2"}]]
      "hydro"
      [:svg {:width "20" :height "20" :viewBox "0 0 24 24" :fill "none"
             :stroke "#4a90d9" :stroke-width "2"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:path {:d "M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"}]]
      "biomass"
      [:svg {:width "20" :height "20" :viewBox "0 0 24 24" :fill "none"
             :stroke "#8b6914" :stroke-width "2"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:path {:d "M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66"}]
       [:path {:d "M20.59 3.41A21 21 0 0 0 3 21c5-4 8-6 11-7s6-2 8-4a3 3 0 0 0-1.41-5.59z"}]]
      "cogeneration"
      [:svg {:width "20" :height "20" :viewBox "0 0 24 24" :fill "none"
             :stroke "#a0522d" :stroke-width "2"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:path {:d "M13 2L3 14h9l-1 8 10-12h-9l1-8z"}]]
      ;; default
      [:svg {:width "20" :height "20" :viewBox "0 0 24 24" :fill "none"
             :stroke "#999" :stroke-width "2"
             :stroke-linecap "round" :stroke-linejoin "round"}
       [:circle {:cx "12" :cy "12" :r "10"}]])))

;; ── Sub-components ──────────────────────────────────────────────────────────

(defn- network-hero []
  (let [network     @(rf/subscribe [:network-detail/network])
        productions @(rf/subscribe [:network-detail/productions])
        stats       @(rf/subscribe [:network-detail/stats])
        name        (:network/name network)
        n           (:production-count stats)
        energy-types (map #(name-or-str (:production/energy-type %)) productions)]
    [:section.nd-hero
     [:div {:style {:display "flex" :align-items "center" :gap "0.5rem" :flex-wrap "wrap"}}
      [:h1.nd-title {:style {:margin "0"}} name]
      (when (seq energy-types)
        [:div {:style {:display "flex" :gap "0.3rem" :align-items "center"}}
         (doall
           (for [[idx etype] (map-indexed vector energy-types)]
             ^{:key idx}
             [:span {:style {:display "flex"} :title (energy-type-label etype)}
              [energy-icon etype]]))])]
     [:p.nd-description
      "Rejoignez l'operation d'autoconsommation collective "
      [:strong name]
      ". Ce reseau reunit " n " "
      (if (> n 1) "sites" "site")
      " de production pour une capacite totale de "
      [:strong (str (:total-capacity-kwc stats) " kWh")]
      "."]]))

(defn- network-stats []
  (let [stats   @(rf/subscribe [:network-detail/stats])
        network @(rf/subscribe [:network-detail/network])
        price   (:network/price-per-kwh network)]
    [:section.nd-stats
     [:div.nd-stat-card
      [:span.nd-stat-value (:total-capacity-kwc stats)]
      [:span.nd-stat-label "Capacite installee (kWh)"]]
     [:div.nd-stat-card
      [:span.nd-stat-value (:production-count stats)]
      [:span.nd-stat-label "Sites de production"]]
     [:div.nd-stat-card
      [:span.nd-stat-value (:consumer-count stats)]
      [:span.nd-stat-label "Consommateurs"]]
     (when price
       [:div.nd-stat-card
        [:span.nd-stat-value (str price " €")]
        [:span.nd-stat-label "Prix HT/kWh"]])]))

(defn- energy-mix-bar [energy-mix]
  (if (empty? energy-mix)
    [:p.nd-empty "Aucune production active"]
    (let [sorted (sort-by val > energy-mix)]
      [:div
       [:div.nd-mix-bar
        (for [[etype pct] sorted]
          ^{:key etype}
          [:div.nd-mix-segment {:style {:width      (str pct "%")
                                        :background (energy-mix-color etype)}}])]
       [:div.nd-mix-legend
        (for [[etype pct] sorted]
          ^{:key etype}
          [:span.nd-mix-legend-item
           [:span.nd-mix-legend-dot {:style {:background (energy-mix-color etype)}}]
           (str (energy-type-label etype) " " pct "%")])]])))

(defn- energy-mix-section []
  (let [stats @(rf/subscribe [:network-detail/stats])]
    [:section.nd-energy-mix
     [:h2 "Mix energetique"]
     [energy-mix-bar (:energy-mix stats)]]))

(defn- network-map-section []
  (let [network     @(rf/subscribe [:network-detail/network])
        productions @(rf/subscribe [:network-detail/productions])
        map-el      (atom nil)
        circle      (atom nil)
        markers     (atom [])]
    (r/create-class
     {:display-name "network-map-section"

      :component-did-mount
      (fn [_this]
        (when @map-el
          (google-maps/load-google-maps-script!
           (fn []
             (let [lat    (:network/center-lat network)
                   lng    (:network/center-lng network)
                   gmap   (js/google.maps.Map.
                           @map-el
                           #js {:center    #js {:lat lat :lng lng}
                                :zoom      13
                                :mapTypeId "roadmap"})
                   c      (google-maps/draw-circle!
                           gmap
                           {:center-lat lat
                            :center-lng lng
                            :radius-km  (:network/radius-km network)})]
               (reset! circle c)
               (.fitBounds gmap (.getBounds c))
               ;; Geocode and place markers for each production
               (doseq [prod productions]
                 (when-let [addr (:production/producer-address prod)]
                   (let [label (str (energy-type-label (:production/energy-type prod))
                                    " - " (:production/installed-power prod) " kWh")]
                     (google-maps/geocode-and-mark! gmap markers addr label nil)))))))))

      :component-will-unmount
      (fn [_this]
        (when @circle (.setMap @circle nil))
        (google-maps/clear-overlays! markers))

      :reagent-render
      (fn []
        [:section.nd-map-section
         [:h2 "Zone du reseau"]
         [:div.nd-map-container {:ref   (fn [el] (when el (reset! map-el el)))
                                 :style {:width        "100%"
                                         :aspect-ratio "4/3"
                                         :min-height   "300px"
                                         :max-height   "500px"}}]])})))

(defn- production-list []
  (let [productions      @(rf/subscribe [:network-detail/productions])
        has-productions? @(rf/subscribe [:network-detail/has-productions?])]
    [:section.nd-productions
     [:h2 "Sites de production"]
     (if-not has-productions?
       [:p.nd-empty "Aucun site de production pour le moment."]
       [:div.nd-production-grid
        (for [prod productions]
          ^{:key (:production/id prod)}
          [:div.nd-production-card
           [:span.nd-production-type (energy-type-label (:production/energy-type prod))]
           [:span.nd-production-power (str (:production/installed-power prod) " kWh")]
           [:span.nd-production-address (:production/producer-address prod)]])])]))

(defn- network-description []
  (let [network @(rf/subscribe [:network-detail/network])
        desc    (:network/description network)]
    (when (seq desc)
      [:section.nd-description
       [:h2 "A propos du reseau"]
       [:p {:style {:white-space "pre-line"}} desc]])))

(defn- join-cta []
  [:section.nd-cta
   [:h2 "Rejoignez ce reseau"]
   [:p "Testez votre eligibilite et commencez a beneficier de l'energie locale."]
   [:a.btn.btn--accent.nd-cta-btn {:href (rfee/href :page/home)} "Tester mon eligibilite"]])

;; ── Admin banner ───────────────────────────────────────────────────────────

(defn- admin-status-banner []
  (let [network   @(rf/subscribe [:network-detail/network])
        lifecycle (:network/lifecycle network)
        public?   (= "public" lifecycle)]
    [:div {:style {:display "flex" :align-items "center" :gap "1rem"
                   :padding "0.75rem 1rem" :margin-bottom "1.5rem"
                   :background (if public? "#e8f5e9" "#fff3e0")
                   :border-radius "var(--radius)" :border (str "1px solid " (if public? "#2e7d32" "#e65100"))}}
     [:span {:style {:font-weight "600" :color (if public? "#2e7d32" "#e65100")}}
      (str "Statut : " (if public? "Public" "Prive"))]
     [:button.btn.btn--small
      {:on-click #(rf/dispatch [:network-detail/toggle-visibility (:network/id network)])
       :style {:margin-left "auto"}}
      (if public? "Rendre prive" "Rendre public")]]))

;; ── Main page ───────────────────────────────────────────────────────────────

(defn network-detail-page []
  (let [loading? @(rf/subscribe [:network-detail/loading?])
        data     @(rf/subscribe [:network-detail/data])
        error    @(rf/subscribe [:network-detail/error])
        admin?   @(rf/subscribe [:auth/admin?])]
    (cond
      loading?
      [:div.nd-page.container [:p "Chargement..."]]

      error
      [:div.nd-page.container
       [:p (if admin?
             "Erreur de chargement du reseau."
             "Le reseau n'est pas visible pour le moment.")]]

      data
      [:div.nd-page.container
       (when admin? [admin-status-banner])
       [network-hero]
       [network-stats]
       [:div.nd-grid
        [:div.nd-grid__left
         [energy-mix-section]
         [network-description]]
        [:div.nd-grid__right
         [network-map-section]]]
       [production-list]
       [join-cta]]

      :else
      [:div.nd-page.container [:p "Aucune donnee."]])))
