(ns app.pages.network-detail
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [app.utils.google-maps :as google-maps]
            [reitit.frontend.easy :as rfee]))

;; ── Helpers ─────────────────────────────────────────────────────────────────

(def ^:private energy-type-labels
  {:solar   "Solaire"
   :wind    "Eolien"
   :hydro   "Hydro"
   :biomass "Biomasse"})

(defn- energy-type-label [k]
  (get energy-type-labels k "Autre"))

(def ^:private energy-mix-colors
  {:solar   "#f5aa46"
   :wind    "#64917d"
   :hydro   "#4a90d9"
   :biomass "#8b6914"})

(defn- energy-mix-color [k]
  (get energy-mix-colors k "#999999"))

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
      [:strong (str (:total-capacity-kwc stats) " kWc")]
      "."]]))

(defn- network-stats []
  (let [stats @(rf/subscribe [:network-detail/stats])]
    [:section.nd-stats
     [:div.nd-stat-card
      [:span.nd-stat-value (:total-capacity-kwc stats)]
      [:span.nd-stat-label "Capacite installee (kWc)"]]
     [:div.nd-stat-card
      [:span.nd-stat-value (:production-count stats)]
      [:span.nd-stat-label "Sites de production"]]
     [:div.nd-stat-card
      [:span.nd-stat-value (:consumer-count stats)]
      [:span.nd-stat-label "Consommateurs"]]]))

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
  (let [network @(rf/subscribe [:network-detail/network])
        map-el  (atom nil)
        circle  (atom nil)]
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
               (.fitBounds gmap (.getBounds c)))))))

      :component-will-unmount
      (fn [_this]
        (when @circle
          (.setMap @circle nil)))

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
           [:span.nd-production-power (str (:production/installed-power prod) " kWc")]
           [:span.nd-production-address (:production/producer-address prod)]])])]))

(defn- join-cta []
  [:section.nd-cta
   [:h2 "Rejoignez ce reseau"]
   [:p "Testez votre eligibilite et commencez a beneficier de l'energie locale."]
   [:a.btn.btn--accent.nd-cta-btn {:href (rfee/href :page/home)} "Tester mon eligibilite"]])

;; ── Main page ───────────────────────────────────────────────────────────────

(defn network-detail-page []
  (let [loading? @(rf/subscribe [:network-detail/loading?])
        data     @(rf/subscribe [:network-detail/data])
        error    @(rf/subscribe [:network-detail/error])]
    (cond
      loading?
      [:div.nd-page.container [:p "Chargement..."]]

      error
      [:div.nd-page.container [:p "Erreur de chargement du reseau."]]

      data
      [:div.nd-page.container
       [network-hero]
       [network-stats]
       [energy-mix-section]
       [network-map-section]
       [production-list]
       [join-cta]]

      :else
      [:div.nd-page.container [:p "Aucune donnee."]])))
