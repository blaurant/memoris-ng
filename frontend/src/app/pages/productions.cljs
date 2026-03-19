(ns app.pages.productions
  (:require [app.components.production-block :as block]
            [app.components.production-onboarding-form :as onboarding]
            [app.consumptions.contract :as contract]
            [app.utils.google-maps :as google-maps]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfee]))

(def ^:private onboarding-states
  #{"producer-information" "installation-info" "payment-info" "contract-signature"})

(defn- onboarding? [production]
  (contains? onboarding-states (:production/lifecycle production)))

;; ── Energy icons ────────────────────────────────────────────────────────────

(def ^:private energy-type-labels
  {"solar" "Solaire" "wind" "Eolien" "hydro" "Hydraulique"
   "biomass" "Biomasse" "cogeneration" "Cogénération"})

(defn- energy-icon [energy-type]
  (case energy-type
    "solar"
    [:svg {:width "28" :height "28" :viewBox "0 0 24 24" :fill "none"
           :stroke "#f5aa46" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
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
    [:svg {:width "28" :height "28" :viewBox "0 0 24 24" :fill "none"
           :stroke "#64917d" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M9.59 4.59A2 2 0 1 1 11 8H2"}]
     [:path {:d "M12.59 19.41A2 2 0 1 0 14 16H2"}]
     [:path {:d "M17.73 7.73A2.5 2.5 0 1 1 19.5 12H2"}]]
    "hydro"
    [:svg {:width "28" :height "28" :viewBox "0 0 24 24" :fill "none"
           :stroke "#4a90d9" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"}]]
    "biomass"
    [:svg {:width "28" :height "28" :viewBox "0 0 24 24" :fill "none"
           :stroke "#8b6914" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66"}]
     [:path {:d "M20.59 3.41A21 21 0 0 0 3 21c5-4 8-6 11-7s6-2 8-4a3 3 0 0 0-1.41-5.59z"}]]
    "cogeneration"
    [:svg {:width "28" :height "28" :viewBox "0 0 24 24" :fill "none"
           :stroke "#a0522d" :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M13 2L3 14h9l-1 8 10-12h-9l1-8z"}]]
    nil))

;; ── Update IBAN modal (2 steps: IBAN → SEPA signature) ──────────────────────

(defn- update-iban-modal [production-id current-iban on-close]
  (let [step     (r/atom :iban)   ;; :iban or :sepa
        new-iban (r/atom (or current-iban ""))]
    (fn [production-id _current-iban on-close]
      [:div.modal-overlay {:on-click (fn [e]
                                        (when (= (.-target e) (.-currentTarget e))
                                          (on-close)))}
       [:div.modal {:on-click #(.stopPropagation %)}
        [:div.modal__header
         [:span (if (= @step :iban)
                  "Modifier l'IBAN"
                  "Mandat de prélèvement SEPA")]
         [:button.btn.btn--small
          {:on-click on-close
           :style {:background "transparent" :color "var(--color-muted)"
                   :border "none" :font-size "1.2rem" :padding "0"}}
          "\u00D7"]]
        [:div.modal__body
         (if (= @step :iban)
           ;; Step 1: IBAN input
           [:div
            [:label {:style {:font-weight "600" :margin-bottom "0.5rem" :display "block"}}
             "Nouvel IBAN"]
            [:input.onboarding__input
             {:type        "text"
              :placeholder "Ex: FR76 3000 6000 0112 3456 7890 189"
              :value       @new-iban
              :on-change   #(reset! new-iban (.. % -target -value))}]]
           ;; Step 2: SEPA mandate
           [:pre {:style {:white-space      "pre-wrap"
                          :font-size        "0.85rem"
                          :line-height      "1.5"
                          :background-color "var(--color-green-pale)"
                          :padding          "1rem"
                          :border-radius    "var(--radius)"
                          :max-height       "400px"
                          :overflow-y       "auto"}}
            contract/sepa-mandate-text])]
        [:div.modal__actions
         (if (= @step :iban)
           ;; Step 1 actions
           [:<>
            [:button.btn.btn--small {:on-click on-close} "Annuler"]
            [:button.btn.btn--green.btn--small
             {:disabled (empty? @new-iban)
              :on-click #(reset! step :sepa)}
             "Suivant"]]
           ;; Step 2 actions
           [:<>
            [:button.btn.btn--small.btn--outline
             {:on-click #(reset! step :iban)}
             "Précédent"]
            [:button.btn.btn--green.btn--small
             {:on-click (fn []
                          (rf/dispatch [:productions/update-iban
                                        production-id @new-iban on-close]))}
             "Signer et valider"]])]]])))

;; ── Dashboard map ───────────────────────────────────────────────────────────

(defn- dashboard-map [network producers consumers]
  (let [map-el  (atom nil)
        circle  (atom nil)
        markers (atom [])]
    (r/create-class
     {:display-name "prod-dashboard-map"

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
               ;; Markers producteurs (bleu)
               (let [blue-pin "https://maps.google.com/mapfiles/ms/icons/blue-dot.png"]
                 (doseq [p producers]
                   (when (:producer-address p)
                     (google-maps/geocode-and-mark!
                       gm markers (:producer-address p)
                       (str (get energy-type-labels (:energy-type p) "") " " (:installed-power p) " kWc")
                       nil blue-pin))))
               ;; Markers consommateurs (rouge par défaut)
               (doseq [c consumers]
                 (when (:address c)
                   (google-maps/geocode-and-mark!
                     gm markers (:address c)
                     (str (:name c))
                     nil))))))))

      :component-will-unmount
      (fn [_]
        (when @circle (.setMap @circle nil))
        (google-maps/clear-overlays! markers))

      :reagent-render
      (fn [_network _producers _consumers]
        [:div.prod-dash__map
         {:ref (fn [el] (when el (reset! map-el el)))}])})))

;; ── Dashboard view ──────────────────────────────────────────────────────────

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

(defn- editable-field
  "Inline editable field. Displays value with a pencil icon, click to edit."
  [value on-save]
  (let [editing? (r/atom false)
        draft    (r/atom value)]
    (fn [value _on-save]
      (if @editing?
        [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
         [:input.onboarding__input
          {:type        "text"
           :value       @draft
           :auto-focus  true
           :on-change   #(reset! draft (.. % -target -value))
           :on-key-down (fn [e]
                          (when (= "Enter" (.-key e))
                            (on-save @draft)
                            (reset! editing? false))
                          (when (= "Escape" (.-key e))
                            (reset! draft value)
                            (reset! editing? false)))
           :style       {:margin "0" :padding "0.3rem 0.5rem" :font-size "0.9rem"}}]
         [:button.btn.btn--green.btn--small
          {:on-click (fn []
                       (on-save @draft)
                       (reset! editing? false))
           :style {:padding "0.3rem 0.6rem"}}
          "OK"]
         [:button.btn.btn--small
          {:on-click (fn []
                       (reset! draft value)
                       (reset! editing? false))
           :style {:padding "0.3rem 0.6rem"}}
          "Annuler"]]
        [:div {:style {:display "flex" :align-items "center" :gap "0.4rem"}}
         [:span value]
         [:button {:on-click (fn []
                               (reset! draft value)
                               (reset! editing? true))
                   :style {:background "transparent" :border "none" :cursor "pointer"
                           :color "var(--color-muted)" :padding "0" :display "flex"}}
          [:svg {:width "14" :height "14" :viewBox "0 0 24 24" :fill "none"
                 :stroke "currentColor" :stroke-width "2"
                 :stroke-linecap "round" :stroke-linejoin "round"}
           [:path {:d "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"}]
           [:path {:d "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"}]]]]))))

(defn- editable-iban-field
  "Inline editable IBAN field. On save, opens a SEPA mandate confirmation modal."
  [value production-id]
  (let [editing?    (r/atom false)
        draft       (r/atom value)
        confirming? (r/atom false)]
    (fn [value _production-id]
      [:<>
       (if @editing?
         [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
          [:input.onboarding__input
           {:type        "text"
            :value       @draft
            :auto-focus  true
            :on-change   #(reset! draft (.. % -target -value))
            :on-key-down (fn [e]
                           (when (= "Escape" (.-key e))
                             (reset! draft value)
                             (reset! editing? false)))
            :style       {:margin "0" :padding "0.3rem 0.5rem" :font-size "0.9rem"}}]
          [:button.btn.btn--green.btn--small
           {:disabled (empty? @draft)
            :on-click #(reset! confirming? true)
            :style {:padding "0.3rem 0.6rem"}}
           "OK"]
          [:button.btn.btn--small
           {:on-click (fn []
                        (reset! draft value)
                        (reset! editing? false))
            :style {:padding "0.3rem 0.6rem"}}
           "Annuler"]]
         [:div {:style {:display "flex" :align-items "center" :gap "0.4rem"}}
          [:span value]
          [:button {:on-click (fn []
                                (reset! draft value)
                                (reset! editing? true))
                    :style {:background "transparent" :border "none" :cursor "pointer"
                            :color "var(--color-muted)" :padding "0" :display "flex"}}
           [:svg {:width "14" :height "14" :viewBox "0 0 24 24" :fill "none"
                  :stroke "currentColor" :stroke-width "2"
                  :stroke-linecap "round" :stroke-linejoin "round"}
            [:path {:d "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"}]
            [:path {:d "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"}]]]])
       ;; SEPA confirmation modal
       (when @confirming?
         [:div.modal-overlay {:on-click (fn [e]
                                           (when (= (.-target e) (.-currentTarget e))
                                             (reset! confirming? false)))}
          [:div.modal {:on-click #(.stopPropagation %)}
           [:div.modal__header
            [:span "Renouvellement du mandat SEPA"]
            [:button.btn.btn--small
             {:on-click #(reset! confirming? false)
              :style {:background "transparent" :color "var(--color-muted)"
                      :border "none" :font-size "1.2rem" :padding "0"}}
             "\u00D7"]]
           [:div.modal__body
            [:p {:style {:margin-bottom "0.75rem"}}
             "Vos informations bancaires ont changé. Pour continuer à recevoir vos paiements, "
             "vous devez renouveler votre autorisation de prélèvement SEPA."]
            [:pre {:style {:white-space "pre-wrap" :font-size "0.85rem"
                           :line-height "1.5" :background-color "var(--color-green-pale)"
                           :padding "1rem" :border-radius "var(--radius)"
                           :max-height "300px" :overflow-y "auto"}}
             contract/sepa-mandate-text]]
           [:div.modal__actions
            [:button.btn.btn--small
             {:on-click (fn []
                          (reset! confirming? false))}
             "Annuler"]
            [:button.btn.btn--green.btn--small
             {:on-click (fn []
                          (rf/dispatch [:productions/update-iban production-id @draft nil])
                          (reset! confirming? false)
                          (reset! editing? false))}
             "Signer et valider"]]]])])))

(defn- production-dashboard [production]
  (let [pid (:production/id production)
        show-sepa? (r/atom false)]
    (r/create-class
     {:component-did-mount
      (fn [_]
        (rf/dispatch [:productions/fetch-dashboard pid]))

      :reagent-render
      (fn [production]
        (let [dashboard @(rf/subscribe [:productions/dashboard])
              loading?  @(rf/subscribe [:productions/dashboard-loading?])
              energy    (:production/energy-type production)
              network   (:network dashboard)
              producers (:producers dashboard)
              consumers (:consumers dashboard)]
          [:div.prod-dash
           ;; Header
           [:div.prod-dash__header
            [:div {:style {:display "flex" :align-items "center" :gap "0.75rem"}}
             [energy-icon energy]
             [:div
              [:h2.prod-dash__title
               (str (get energy-type-labels energy energy) " — "
                    (:production/installed-power production) " kWc")]
              (when (:network/name network)
                [:a.prod-dash__network
                 {:href (rfee/href :page/network-detail {:id (:network/id network)})}
                 (:network/name network)])]]
            [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
             [status-badge (:production/lifecycle production)]
             [block/production-menu production]]]

           ;; Address
           (when (:production/producer-address production)
             [:div.prod-dash__address
              [:img {:src   "https://maps.google.com/mapfiles/ms/icons/blue-dot.png"
                     :style {:width "18px" :height "18px" :vertical-align "middle"
                             :margin-right "0.4rem"}}]
              [editable-field (:production/producer-address production)
               #(rf/dispatch [:productions/update-address pid %])]])

           ;; Stats + Map grid
           [:div.prod-dash__grid
            ;; Left: stats
            [:div.prod-dash__stats
             (when-let [kwh (:production/last-monthly-kwh production)]
               [:div.prod-dash__stat-card
                [:span.prod-dash__stat-value (str (.toFixed kwh 1) " kWh")]
                [:span.prod-dash__stat-label "Production du mois"]])
             (when (:network/price-per-kwh network)
               [:div.prod-dash__stat-card
                [:span.prod-dash__stat-value (str (:network/price-per-kwh network) " €")]
                [:span.prod-dash__stat-label "Prix HT/kWh"]])
             [:div.prod-dash__stat-card
              [:span.prod-dash__stat-value (count consumers)]
              [:span.prod-dash__stat-label
               (str "Consommateur" (when (> (count consumers) 1) "s"))]]
             (when (seq producers)
               [:div.prod-dash__stat-card
                [:span.prod-dash__stat-value (inc (count producers))]
                [:span.prod-dash__stat-label "Producteurs sur le réseau"]])
             ;; Financial preview (fake data)
             [:div.prod-dash__finance-preview
              [:div.prod-dash__finance-badge "Bientôt disponible"]
              [:div.prod-dash__stat-card
               [:span.prod-dash__stat-value "523 €"]
               [:span.prod-dash__stat-label "Gains du dernier mois"]]
              [:div.prod-dash__stat-card
               [:span.prod-dash__stat-value "0,13 €/kWh"]
               [:span.prod-dash__stat-label "Prix de revente"]]]]

            ;; Right: map
            (when network
              [dashboard-map network
               (conj (vec producers)
                     {:energy-type (:production/energy-type production)
                      :installed-power (:production/installed-power production)
                      :producer-address (:production/producer-address production)})
               consumers])]

           ;; Technical details
           [:div.prod-dash__section
            [:h3 "Détails techniques"]
            [:div.prod-dash__details
             [:div.prod-dash__field
              [:span.prod-dash__field-label "PDL/PRM"]
              [editable-field (:production/pdl-prm production)
               #(rf/dispatch [:productions/update-pdl-prm pid %])]]
             [:div.prod-dash__field
              [:span.prod-dash__field-label "Compteur Linky"]
              [editable-field (:production/linky-meter production)
               #(rf/dispatch [:productions/update-linky-meter pid %])]]]]

           ;; Banking info
           (when (:production/iban production)
             [:div.prod-dash__section
              [:h3 "Informations bancaires"]
              [:div.prod-dash__details
               [:div.prod-dash__field
                [:span.prod-dash__field-label "IBAN"]
                [editable-iban-field (:production/iban production) pid]]
               [:div.prod-dash__field
                [:span.prod-dash__field-label "Mandat SEPA"]
                [:span.consumption-block__contract-link
                 {:on-click #(reset! show-sepa? true)}
                 [:svg {:width "16" :height "16" :viewBox "0 0 24 24"
                        :fill "none" :stroke "currentColor" :stroke-width "2"
                        :stroke-linecap "round" :stroke-linejoin "round"
                        :style {:vertical-align "middle" :margin-right "0.3rem"}}
                  [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
                  [:polyline {:points "14 2 14 8 20 8"}]
                  [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
                  [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]]
                 "Consulter le mandat"]]]])

           ;; SEPA mandate modal
           (when @show-sepa?
             [:div.modal-overlay {:on-click (fn [e]
                                               (when (= (.-target e) (.-currentTarget e))
                                                 (reset! show-sepa? false)))}
              [:div.modal
               [:div.modal__header
                [:span "Mandat de prélèvement SEPA"]
                [:button.btn.btn--small
                 {:on-click #(reset! show-sepa? false)
                  :style {:background "transparent" :color "var(--color-muted)"
                          :border "none" :font-size "1.2rem" :padding "0"}}
                 "\u00D7"]]
               [:div.modal__body
                [:pre {:style {:white-space "pre-wrap" :font-size "0.85rem"
                               :line-height "1.5" :background-color "var(--color-green-pale)"
                               :padding "1rem" :border-radius "var(--radius)"
                               :max-height "400px" :overflow-y "auto"}}
                 contract/sepa-mandate-text]]
               [:div.modal__actions
                [:button.btn.btn--small.btn--green
                 {:on-click #(reset! show-sepa? false)}
                 "Fermer"]]]])



           ;; Consumers list
           (when (and (not loading?) (seq consumers))
             [:div.prod-dash__section
              [:h3 (str "Consommateurs du réseau (" (count consumers) ")")]
              [:table.admin-table
               [:thead
                [:tr
                 [:th "Nom"]
                 [:th "Adresse"]
                 [:th "Dernière conso"]
                 [:th "Statut"]]]
               [:tbody
                (doall
                  (for [[idx c] (map-indexed vector consumers)]
                    ^{:key idx}
                    [:tr
                     [:td (:name c)]
                     [:td (or (:address c) "—")]
                     [:td (if-let [kwh (:last-monthly-kwh c)]
                            (str kwh " kWh")
                            "—")]
                     [:td (:lifecycle c)]]))]]])]))})))

;; ── Menu extraction from block (reuse dropdown) ────────────────────────────
;; The block module exposes the full block; we need just the menu for dashboard.
;; We add a public fn in production_block for the menu.

;; ── Main page ───────────────────────────────────────────────────────────────

(defn productions-page []
  (r/create-class
    {:component-did-mount
     (fn [_]
       (rf/dispatch [:productions/fetch])
       (rf/dispatch [:networks/fetch]))

     :reagent-render
     (fn []
       (let [productions @(rf/subscribe [:productions/list])
             loading?    @(rf/subscribe [:productions/loading?])
             active-prods (filterv #(not (onboarding? %)) productions)
             onboarding-prods (filterv onboarding? productions)
             primary (first active-prods)
             others  (rest active-prods)]
         [:div.consumptions
          (cond
            loading?
            [:p.loading "Chargement..."]

            ;; Onboarding in progress — show onboarding form
            (seq onboarding-prods)
            [:div
             (doall
               (for [p onboarding-prods]
                 ^{:key (:production/id p)}
                 [onboarding/production-onboarding-form p]))]

            ;; No productions at all
            (empty? productions)
            [:div.consumptions__empty
             [:p "Aucune production pour le moment."]
             [:button.btn.btn--green
              {:on-click #(rf/dispatch [:productions/create])
               :style {:margin-top "1rem"}}
              "Ajouter ma production"]]

            ;; Dashboard mode — all productions as full dashboards
            :else
            [:div
             (doall
               (for [p active-prods]
                 ^{:key (:production/id p)}
                 [:div {:style {:margin-bottom "2rem"}}
                  [production-dashboard p]]))
             ;; Discrete CTA for additional site
             [:div.prod-dash__add-site
              [:span "Vous avez un autre site de production ?"]
              [:button.btn.btn--small.btn--outline
               {:on-click #(rf/dispatch [:productions/create])}
               "+ Ajouter un site"]]])]))}))
