(ns app.pages.consumptions
  (:require [app.components.onboarding-form :as onboarding]
            [app.consumptions.utils :as conso-utils]
            [app.utils.google-maps :as google-maps]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfee]))

(def ^:private onboarding-states
  #{"consumer-information" "linky-reference" "billing-address" "contract-signature"})

(defn- onboarding? [consumption]
  (contains? onboarding-states (:consumption/lifecycle consumption)))

(def ^:private energy-type-labels
  {"solar" "Solaire" "wind" "Eolien" "hydro" "Hydraulique"
   "biomass" "Biomasse" "cogeneration" "Cogénération"})

(def ^:private month-labels
  {1 "Jan" 2 "Fév" 3 "Mar" 4 "Avr" 5 "Mai" 6 "Juin"
   7 "Juil" 8 "Août" 9 "Sep" 10 "Oct" 11 "Nov" 12 "Déc"})

(defn- monthly-bar-chart [monthly-history]
  (let [sorted  (->> monthly-history
                     (sort-by (juxt :year :month))
                     (take-last 6)
                     vec)
        max-kwh (apply max 1 (map :kwh sorted))
        n       (count sorted)
        w       280
        h       130
        bar-w   30
        gap     (if (> n 1) (/ (- w (* n bar-w)) (dec n)) 0)
        chart-h 90
        label-y (+ chart-h 15)]
    (when (seq sorted)
      [:svg {:viewBox (str "0 0 " w " " h) :width "100%"
             :style {:max-width "320px" :display "block" :margin "0 auto"}}
       (doall
         (for [[idx entry] (map-indexed vector sorted)]
           (let [x      (* idx (+ bar-w gap))
                 bar-h  (* chart-h (/ (:kwh entry) max-kwh))
                 y      (- chart-h bar-h)
                 cx     (+ x (/ bar-w 2))]
             ^{:key idx}
             [:g
              ;; Bar
              [:rect {:x x :y y :width bar-w :height bar-h
                      :rx 3 :fill "#43a047"}]
              ;; kWh value above bar
              [:text {:x cx :y (- y 4) :text-anchor "middle"
                      :font-size "9" :fill "#333" :font-weight "600"}
               (str (.toFixed (:kwh entry) 0))]
              ;; Month label below
              [:text {:x cx :y label-y :text-anchor "middle"
                      :font-size "9" :fill "#888"}
               (get month-labels (:month entry) "?")]])))])))


;; ── Dashboard map ───────────────────────────────────────────────────────────

(defn- dashboard-map [network producers consumer-address]
  (let [map-el  (atom nil)
        circle  (atom nil)
        markers (atom [])]
    (r/create-class
     {:display-name "conso-dashboard-map"

      :component-did-mount
      (fn [_]
        (when (and @map-el network)
          (google-maps/load-google-maps-script!
           (fn []
             (let [lat (:network/center-lat network)
                   lng (:network/center-lng network)
                   gm  (js/google.maps.Map.
                         @map-el
                         #js {:center #js {:lat lat :lng lng}
                              :zoom 13 :mapTypeId "roadmap"})
                   c   (google-maps/draw-circle!
                         gm {:center-lat lat :center-lng lng
                             :radius-km (:network/radius-km network)})]
               (reset! circle c)
               (.fitBounds gm (.getBounds c))
               ;; Producers (blue pins)
               (let [blue-pin "https://maps.google.com/mapfiles/ms/icons/blue-dot.png"]
                 (doseq [p producers]
                   (when (:producer-address p)
                     (google-maps/geocode-and-mark!
                       gm markers (:producer-address p)
                       (str (get energy-type-labels (:energy-type p) "") " " (:installed-power p) " kWh")
                       nil blue-pin))))
               ;; Consumer (red dot)
               (when consumer-address
                 (let [red-dot #js {:path   js/google.maps.SymbolPath.CIRCLE
                                    :scale  6
                                    :fillColor "#d32f2f"
                                    :fillOpacity 1
                                    :strokeColor "#b71c1c"
                                    :strokeWeight 1}]
                   (google-maps/geocode-and-mark!
                     gm markers consumer-address "Ma consommation" nil red-dot))))))))

      :component-will-unmount
      (fn [_]
        (when @circle (.setMap ^js @circle nil))
        (google-maps/clear-overlays! markers))

      :reagent-render
      (fn [_network _producers _consumer-address]
        [:div.prod-dash__map
         {:ref (fn [el] (when el (reset! map-el el)))}])})))

;; ── Status badge ────────────────────────────────────────────────────────────

(defn- status-badge [lifecycle]
  (let [label (case lifecycle
                "active" "Actif"
                "pending" "En attente"
                lifecycle)
        color (case lifecycle
                "active" "#2e7d32"
                "pending" "#e65100"
                "#757575")]
    [:span.prod-dash__badge {:style {:background color}} label]))


;; ── Editable field ──────────────────────────────────────────────────────────

(defn- editable-field [value on-save]
  (let [editing? (r/atom false)
        draft    (r/atom value)]
    (fn [value _on-save]
      (if @editing?
        [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
         [:input.onboarding__input
          {:type "text" :value @draft :auto-focus true
           :on-change #(reset! draft (.. % -target -value))
           :on-key-down (fn [e]
                          (when (= "Enter" (.-key e)) (on-save @draft) (reset! editing? false))
                          (when (= "Escape" (.-key e)) (reset! draft value) (reset! editing? false)))
           :style {:margin "0" :padding "0.3rem 0.5rem" :font-size "0.9rem"}}]
         [:button.btn.btn--green.btn--small
          {:on-click #(do (on-save @draft) (reset! editing? false))
           :style {:padding "0.3rem 0.6rem"}} "OK"]
         [:button.btn.btn--small
          {:on-click #(do (reset! draft value) (reset! editing? false))
           :style {:padding "0.3rem 0.6rem"}} "Annuler"]]
        [:div {:style {:display "flex" :align-items "center" :gap "0.4rem"}}
         [:span value]
         [:button {:on-click #(do (reset! draft value) (reset! editing? true))
                   :style {:background "transparent" :border "none" :cursor "pointer"
                           :color "var(--color-muted)" :padding "0" :display "flex"}}
          [:svg {:width "14" :height "14" :viewBox "0 0 24 24" :fill "none"
                 :stroke "currentColor" :stroke-width "2"
                 :stroke-linecap "round" :stroke-linejoin "round"}
           [:path {:d "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"}]
           [:path {:d "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"}]]]]))))

;; ── Consumption menu ───────────────────────────────────────────────────────

(defn- confirm-delete-modal [consumption on-confirm on-cancel]
  [:div.modal-overlay {:on-click (fn [e]
                                    (when (= (.-target e) (.-currentTarget e))
                                      (on-cancel)))}
   [:div.modal {:on-click #(.stopPropagation %)}
    [:div.modal__header
     [:span "Supprimer la consommation"]
     [:button.btn.btn--small {:on-click on-cancel
                              :style {:background "transparent" :color "var(--color-muted)"
                                      :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:p {:style {:margin-bottom "0.5rem" :font-weight "600" :color "#d32f2f"}}
      "Cette action est irréversible."]
     [:p {:style {:margin-bottom "0.5rem"}}
      "Voulez-vous vraiment supprimer cette consommation ?"]]
    [:div.modal__actions
     [:button.btn.btn--small {:on-click on-cancel} "Annuler"]
     [:button.btn.btn--small {:on-click on-confirm
                              :style {:background "#d32f2f" :color "#fff"}}
      "Supprimer"]]]])

(defn- consumption-menu [consumption]
  (let [show-menu?      (r/atom false)
        confirm-delete? (r/atom false)]
    (fn [consumption]
      (let [cid (:consumption/id consumption)]
        [:<>
         [:div {:style {:position "relative"}}
          [:button {:on-click #(swap! show-menu? not)
                    :style {:background "transparent" :border "none" :cursor "pointer"
                            :color "var(--color-muted)" :font-size "1.3rem"
                            :padding "0 0.25rem" :line-height "1"}}
           "\u22EE"]
          (when @show-menu?
            [:div.dropdown-menu
             {:on-mouse-leave #(reset! show-menu? false)}
             [:button.dropdown-menu__item
              {:on-click (fn []
                           (reset! show-menu? false)
                           (reset! confirm-delete? true))}
              "Supprimer"]])]
         (when @confirm-delete?
           [confirm-delete-modal consumption
            (fn []
              (rf/dispatch [:consumptions/delete cid])
              (reset! confirm-delete? false))
            #(reset! confirm-delete? false)])]))))

;; ── Dashboard view ──────────────────────────────────────────────────────────

(defn- consumption-dashboard [consumption]
  (let [cid       (:consumption/id consumption)
        expanded? (r/atom false)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (rf/dispatch [:consumptions/fetch-dashboard cid]))

      :reagent-render
      (fn [consumption]
        (let [cid        (:consumption/id consumption)
              dashboard  @(rf/subscribe [:consumptions/dashboard cid])
              loading?   @(rf/subscribe [:consumptions/dashboard-loading? cid])
              network    (:network dashboard)
              producers  (:producers dashboard)
              lifecycle  (:consumption/lifecycle consumption)
              user       @(rf/subscribe [:auth/user])
              adhesion?  (some? (:adhesion-signed-at user))]
          [:div.prod-dash
           ;; Header
           [:div.prod-dash__header
            [:div
             [:h2.prod-dash__title {:style {:display "flex" :align-items "center" :flex-wrap "nowrap" :gap "0.3rem"}}
              [:span "Ma consommation"]
              (when (:network/name network)
                [:<>
                 [:span "pour"]
                 [:a.prod-dash__network
                  {:href (rfee/href :page/network-detail {:id (:network/id network)})
                   :style {:white-space "nowrap"}}
                  (:network/name network)]])]]
            [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
             [status-badge lifecycle]
             [consumption-menu consumption]]]

           ;; Address
           (when (:consumption/consumer-address consumption)
             [:div.prod-dash__address
              [:span {:style {:display "inline-block" :width "12px" :height "12px"
                              :border-radius "50%" :background "#d32f2f"
                              :margin-right "0.4rem" :flex-shrink "0"}}]
              [editable-field (:consumption/consumer-address consumption)
               #(rf/dispatch [:consumptions/update-address cid %])]])

           ;; Stats + Map grid
           [:div.prod-dash__grid
            [:div.prod-dash__stats
             (when-let [kwh (conso-utils/latest-monthly-kwh consumption)]
               [:div.prod-dash__stat-card
                [:span.prod-dash__stat-value (str (.toFixed kwh 1) " kWh")]
                [:span.prod-dash__stat-label "Consommation du mois"]])
             [:div.prod-dash__stat-card
              {:style {:display "flex" :flex-direction "row" :justify-content "space-around" :gap "1rem"}}
              [:div {:style {:text-align "center"}}
               [:span.prod-dash__stat-value (count producers)]
               [:span.prod-dash__stat-label
                (str "Producteur" (when (> (count producers) 1) "s") " sur le r\u00e9seau")]]
              (when (:network/price-per-kwh network)
                [:div {:style {:text-align "center"}}
                 [:span.prod-dash__stat-value (str (:network/price-per-kwh network) " \u20ac")]
                 [:span.prod-dash__stat-label "Prix HT/kWh"]])]
             ;; Bar chart — last 6 months
             (if (seq (:consumption/monthly-history consumption))
               [:div.prod-dash__stat-card
                [:span.prod-dash__stat-label {:style {:margin-bottom "0.5rem"}}
                 "Consommation des 6 derniers mois"]
                [monthly-bar-chart (:consumption/monthly-history consumption)]]
               ;; Draft placeholder when no history
               [:div.prod-dash__finance-preview
                [:div.prod-dash__finance-badge "Bientôt disponible"]
                [:div.prod-dash__stat-card
                 [:span.prod-dash__stat-value "— kWh"]
                 [:span.prod-dash__stat-label "Consommation des 6 derniers mois"]]])]

            ;; Map
            (when network
              [dashboard-map network producers
               (:consumption/consumer-address consumption)])]

           ;; Toggle bar
           [:div.prod-dash__toggle
            {:on-click #(swap! expanded? not)}
            [:span.prod-dash__toggle-arrow
             {:class (when @expanded? "prod-dash__toggle-arrow--open")}
             "›"]
            [:span.prod-dash__toggle-label
             (if @expanded? "Masquer les détails" "Voir les détails")]
            [:span.prod-dash__toggle-line]]

           ;; Collapsible details
           [:div.prod-dash__collapsible
            {:class (when-not @expanded? "prod-dash__collapsible--closed")}

            ;; Technical details
            [:div.prod-dash__section
             [:h3 "Détails techniques"]
             [:div.prod-dash__details
              (when (:consumption/linky-reference consumption)
                [:div.prod-dash__field
                 [:span.prod-dash__field-label "Compteur Linky"]
                 [:span (:consumption/linky-reference consumption)]])]]

            ;; Banking info (draft)
            [:div.prod-dash__section
             [:h3 "Informations bancaires"]
             (when (:consumption/billing-address consumption)
               [:div.prod-dash__details {:style {:margin-bottom "0.75rem"}}
                [:div.prod-dash__field
                 [:span.prod-dash__field-label "Adresse de facturation"]
                 [editable-field (:consumption/billing-address consumption)
                  #(rf/dispatch [:consumptions/update-billing-address cid %])]]])
             (when (:consumption/iban consumption)
               [:div.prod-dash__details
                [:div.prod-dash__field
                 [:span.prod-dash__field-label "IBAN"]
                 [:span (let [iban (:consumption/iban consumption)]
                          (if (> (count iban) 8)
                            (str (subs iban 0 4) " •••• •••• " (subs iban (- (count iban) 4)))
                            iban))]]
                (when (:consumption/bic consumption)
                  [:div.prod-dash__field
                   [:span.prod-dash__field-label "BIC"]
                   [:span (:consumption/bic consumption)]])
                [:div.prod-dash__field
                 [:span.prod-dash__field-label "Mandat SEPA"]
                 [:span (if (:consumption/sepa-mandate-signed-at consumption)
                          "Signé"
                          "Non signé")]]])]

            ;; Contracts
            (when (or (:consumption/producer-contract-signed-at consumption)
                      (:consumption/sepa-mandate-signed-at consumption)
                      adhesion?)
              [:div.prod-dash__section
               [:h3 "Contrats"]
               [:div.prod-dash__details
                (when adhesion?
                  [:div.prod-dash__field
                   [:span.prod-dash__field-label "Adhésion Elink-co"]
                   [:span.consumption-block__contract-link
                    {:on-click #(rf/dispatch [:auth/download-adhesion])}
                    [:svg {:width "16" :height "16" :viewBox "0 0 24 24"
                           :fill "none" :stroke "currentColor" :stroke-width "2"
                           :stroke-linecap "round" :stroke-linejoin "round"
                           :style {:vertical-align "middle" :margin-right "0.3rem"}}
                     [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
                     [:polyline {:points "7 10 12 15 17 10"}]
                     [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
                    "Télécharger"]])
                (when (:consumption/producer-contract-signed-at consumption)
                  [:div.prod-dash__field
                   [:span.prod-dash__field-label "Contrat Producteur"]
                   [:span.consumption-block__contract-link
                    {:on-click #(rf/dispatch [:consumptions/download-contract cid :producer])}
                    [:svg {:width "16" :height "16" :viewBox "0 0 24 24"
                           :fill "none" :stroke "currentColor" :stroke-width "2"
                           :stroke-linecap "round" :stroke-linejoin "round"
                           :style {:vertical-align "middle" :margin-right "0.3rem"}}
                     [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
                     [:polyline {:points "7 10 12 15 17 10"}]
                     [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
                    "Télécharger"]])
                (when (:consumption/sepa-mandate-signed-at consumption)
                  [:div.prod-dash__field
                   [:span.prod-dash__field-label "Mandat SEPA"]
                   [:span.consumption-block__contract-link
                    {:on-click #(rf/dispatch [:consumptions/download-contract cid :sepa])}
                    [:svg {:width "16" :height "16" :viewBox "0 0 24 24"
                           :fill "none" :stroke "currentColor" :stroke-width "2"
                           :stroke-linecap "round" :stroke-linejoin "round"
                           :style {:vertical-align "middle" :margin-right "0.3rem"}}
                     [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
                     [:polyline {:points "7 10 12 15 17 10"}]
                     [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
                    "Télécharger"]])]])

            ;; Invoices (draft)
            [:div.prod-dash__section
             [:h3 "Factures"]
             [:div.prod-dash__finance-preview
              [:div.prod-dash__finance-badge "Bientôt disponible"]
              [:table.admin-table
               [:thead
                [:tr
                 [:th "Période"]
                 [:th "kWh"]
                 [:th "Montant"]
                 [:th "Statut"]]]
               [:tbody
                [:tr
                 [:td "Février 2026"]
                 [:td "187 kWh"]
                 [:td "24,31 €"]
                 [:td "Payée"]]
                [:tr
                 [:td "Janvier 2026"]
                 [:td "210 kWh"]
                 [:td "27,30 €"]
                 [:td "Payée"]]
                [:tr
                 [:td "Décembre 2025"]
                 [:td "195 kWh"]
                 [:td "25,35 €"]
                 [:td "Payée"]]]]]]]]))})))

;; ── Main page ───────────────────────────────────────────────────────────────

(defn consumptions-page []
  (r/create-class
    {:component-did-mount
     (fn [_]
       (rf/dispatch [:consumptions/fetch])
       (rf/dispatch [:networks/fetch]))

     :reagent-render
     (fn []
       (let [consumptions @(rf/subscribe [:consumptions/list])
             loading?     @(rf/subscribe [:consumptions/loading?])
             creating?    @(rf/subscribe [:consumptions/creating?])
             active-consos (filterv #(not (onboarding? %)) consumptions)
             onboarding-consos (filterv onboarding? consumptions)]
         [:div.consumptions
          (cond
            (or loading? creating?)
            [:p.loading "Chargement..."]

            (seq onboarding-consos)
            [:div
             (doall
               (for [c onboarding-consos]
                 ^{:key (:consumption/id c)}
                 [onboarding/onboarding-form c]))]

            (empty? consumptions)
            [:div
             [:p "Bienvenue ! Commencez votre première consommation."]
             [:button.btn.btn--primary
              {:on-click #(rf/dispatch [:consumptions/create])}
              "Démarrer"]]

            :else
            [:div
             (doall
               (for [c active-consos]
                 ^{:key (:consumption/id c)}
                 [:div {:style {:margin-bottom "2rem"}}
                  [consumption-dashboard c]]))
             [:div.prod-dash__add-site
              [:span "Vous souhaitez ajouter un autre point de consommation ?"]
              [:button.btn.btn--small.btn--outline
               {:on-click #(rf/dispatch [:consumptions/create])}
               "+ Ajouter"]]])]))}))
