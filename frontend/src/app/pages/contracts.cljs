(ns app.pages.contracts
  (:require [app.consumptions.contract :as conso-contract]
            [app.productions.contract :as prod-contract]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private lifecycle-labels
  {"active"              "Actif"
   "pending"             "En attente"
   "suspended"           "Suspendu"
   "abandoned"           "Abandonné"
   "contract-signature"  "Signature"
   "consumer-information" "En cours"
   "linky-reference"     "En cours"
   "billing-address"     "En cours"
   "producer-information" "En cours"
   "installation-info"   "En cours"
   "payment-info"        "En cours"})

(defn- status-class [lifecycle]
  (case lifecycle
    "active"    "contracts__status--active"
    "pending"   "contracts__status--pending"
    "suspended" "contracts__status--suspended"
    "abandoned" "contracts__status--abandoned"
    "contracts__status--onboarding"))

(defn- doc-icon []
  [:svg {:width "20" :height "20" :viewBox "0 0 24 24"
         :fill "none" :stroke "currentColor" :stroke-width "2"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

(defn- format-date
  "Formats an ISO date string like '2026-03-18T10:00:00Z' to 'dd/mm/yyyy'."
  [iso-str]
  (when iso-str
    (let [d (js/Date. iso-str)]
      (when-not (js/isNaN (.getTime d))
        (let [day   (.padStart (str (.getDate d)) 2 "0")
              month (.padStart (str (inc (.getMonth d))) 2 "0")
              year  (.getFullYear d)]
          (str day "/" month "/" year))))))

(def ^:private energy-type-labels
  {"solar"        "Solaire"
   "wind"         "Eolien"
   "hydro"        "Hydraulique"
   "biomass"      "Biomasse"
   "cogeneration" "Cogeneration"})

(defn- contract-modal [title text on-close]
  [:div.modal-overlay {:on-click (fn [e]
                                    (when (= (.-target e) (.-currentTarget e))
                                      (on-close)))}
   [:div.modal
    [:div.modal__header
     [:span title]
     [:button.btn.btn--small
      {:on-click on-close
       :style {:background "transparent" :color "var(--color-muted)"
               :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:pre {:style {:white-space      "pre-wrap"
                    :font-size        "0.85rem"
                    :line-height      "1.5"
                    :background-color "var(--color-green-pale)"
                    :padding          "1rem"
                    :border-radius    "var(--radius)"
                    :max-height       "400px"
                    :overflow-y       "auto"}}
      text]]
    [:div.modal__actions
     [:button.btn.btn--small.btn--green
      {:on-click on-close}
      "Fermer"]]]])

(defn- contract-row [{:keys [icon label sublabel status lifecycle signed-at on-click download-event]}]
  (let [click-handler (or (when download-event
                            #(rf/dispatch download-event))
                          on-click)]
    [:div.contracts__row {:on-click click-handler
                          :style {:cursor (when click-handler "pointer")}}
     [:div.contracts__row-left
      icon
      [:div
       [:span.contracts__row-label label]
       (when sublabel
         [:span.contracts__row-type sublabel])]]
     [:div.contracts__row-right
      [:span {:class (str "contracts__status " (status-class lifecycle))}
       (if-let [date (format-date signed-at)]
         (str "Signé le " date)
         status)]]]))

;; ── Adhesion section ────────────────────────────────────────────────────────

(defn- adhesion-section []
  (let [open-modal? (r/atom false)]
    (fn []
      (let [user             @(rf/subscribe [:auth/user])
            adhesion-signed? (some? (:adhesion-signed-at user))]
        (when adhesion-signed?
          [:<>
           [:h3.contracts__section-title "Adhésion"]
           [contract-row
            {:icon           [doc-icon]
             :label          "Adhésion Elink-co"
             :sublabel       "Association"
             :status         "Signé"
             :lifecycle      "active"
             :signed-at      (:adhesion-signed-at user)
             :on-click       #(reset! open-modal? true)
             :download-event [:auth/download-adhesion]}]
           (when @open-modal?
             [contract-modal "Adhésion Elink-co"
              conso-contract/contract-text
              #(reset! open-modal? false)])])))))

;; ── Productions section ─────────────────────────────────────────────────────

(defn- productions-section []
  (let [open-modal? (r/atom nil)]
    (fn []
      (let [productions @(rf/subscribe [:productions/list])]
        (when (seq productions)
          [:<>
           [:h3.contracts__section-title "Productions"]
           (doall
             (for [p productions]
               (let [lifecycle (:production/lifecycle p)
                     energy    (get energy-type-labels (:production/energy-type p) (:production/energy-type p))
                     power     (:production/installed-power p)
                     pid       (:production/id p)]
                 ^{:key (str "prod-" pid)}
                 [contract-row
                  {:icon      [doc-icon]
                   :label     (str energy " — " power " kWh")
                   :sublabel  "Production"
                   :status    (get lifecycle-labels lifecycle lifecycle)
                   :lifecycle lifecycle
                   :signed-at (:production/adhesion-signed-at p)
                   :on-click  (when (= "active" lifecycle)
                                #(reset! open-modal? pid))}])))
           (when @open-modal?
             [contract-modal "Contrat de Producteur"
              prod-contract/adhesion-contract-text
              #(reset! open-modal? nil)])])))))

;; ── Consumptions section ────────────────────────────────────────────────────

(defn- consumptions-section []
  (fn []
    (let [consumptions @(rf/subscribe [:consumptions/list])]
      (when (seq consumptions)
        [:<>
         [:h3.contracts__section-title "Consommations"]
         (doall
           (for [c consumptions]
             (let [network-id   (:consumption/network-id c)
                   network-name @(rf/subscribe [:consumptions/network-name network-id])
                   cid          (:consumption/id c)
                   producer-signed? (some? (:consumption/producer-contract-signed-at c))
                   sepa-signed?     (some? (:consumption/sepa-mandate-signed-at c))]
               ^{:key (str "conso-" cid)}
               [:<>
                (when producer-signed?
                  [contract-row
                   {:icon           [doc-icon]
                    :label          (str "Contrat Producteur — " (or network-name "Réseau"))
                    :sublabel       "Consommation"
                    :status         "Signé"
                    :lifecycle      "active"
                    :signed-at      (:consumption/producer-contract-signed-at c)
                    :download-event [:consumptions/download-contract cid :producer]}])
                (when sepa-signed?
                  [contract-row
                   {:icon           [doc-icon]
                    :label          (str "Mandat SEPA — " (or network-name "Réseau"))
                    :sublabel       "Consommation"
                    :status         "Signé"
                    :lifecycle      "active"
                    :signed-at      (:consumption/sepa-mandate-signed-at c)
                    :download-event [:consumptions/download-contract cid :sepa]}])])))]))))

;; ── Main page ───────────────────────────────────────────────────────────────

(defn contracts-page []
  (r/create-class
    {:component-did-mount
     (fn [_]
       (rf/dispatch [:consumptions/fetch])
       (rf/dispatch [:productions/fetch])
       (rf/dispatch [:networks/fetch]))

     :reagent-render
     (fn []
       (let [consumptions     @(rf/subscribe [:consumptions/list])
             productions      @(rf/subscribe [:productions/list])
             user             @(rf/subscribe [:auth/user])
             adhesion-signed? (some? (:adhesion-signed-at user))
             conso-load?      @(rf/subscribe [:consumptions/loading?])
             prod-load?       @(rf/subscribe [:productions/loading?])
             loading?         (or conso-load? prod-load?)
             has-anything?    (or adhesion-signed? (seq productions) (seq consumptions))]
         [:div.contracts
          [:div.consumptions__header
           [:h2 "Mes contrats"]]
          (cond
            loading?
            [:p.loading "Chargement..."]

            (not has-anything?)
            [:div.consumptions__empty
             [:p "Aucun contrat pour le moment."]
             [:div {:style {:display "flex" :gap "1rem" :margin-top "1rem"}}
              [:button.btn.btn--green
               {:on-click #(rf/dispatch [:portal/set-section :consumptions])}
               "Ajouter une consommation"]
              [:button.btn.btn--green
               {:on-click #(rf/dispatch [:portal/set-section :productions])}
               "Ajouter une production"]]]

            :else
            [:div.contracts__list
             [adhesion-section]
             [productions-section]
             [consumptions-section]])]))}))
