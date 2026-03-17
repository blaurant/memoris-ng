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

;; ── Sub-components ──────────────────────────────────────────────────────────

(defn- network-hero []
  (let [network @(rf/subscribe [:network-detail/network])
        stats   @(rf/subscribe [:network-detail/stats])
        name    (:network/name network)
        n       (:production-count stats)]
    [:section.nd-hero
     [:h1.nd-title name]
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
