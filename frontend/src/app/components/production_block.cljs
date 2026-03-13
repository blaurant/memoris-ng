(ns app.components.production-block
  (:require [app.productions.contract :as contract]
            [reagent.core :as r]))

(def ^:private energy-type-labels
  {"solar"        "Solaire"
   "wind"         "Eolien"
   "hydro"        "Hydraulique"
   "biomass"      "Biomasse"
   "cogeneration" "Cogeneration"})

(defn production-block [production]
  (let [show-contract? (r/atom false)]
    (fn [production]
      (let [energy-type (:production/energy-type production)
            lifecycle   (:production/lifecycle production)]
        [:div.consumption-block
         [:div.consumption-block__header
          [:span (str (get energy-type-labels energy-type energy-type) " — " (:production/installed-power production) " kWc")]
          [:span.consumption-block__status lifecycle]]
         [:div.consumption-block__details
          [:div.consumption-block__field
           [:span.consumption-block__label "PDL/PRM"]
           [:span.consumption-block__value (:production/pdl-prm production)]]
          [:div.consumption-block__field
           [:span.consumption-block__label "Compteur Linky"]
           [:span.consumption-block__value (:production/linky-meter production)]]
          (when (:production/iban production)
            [:div.consumption-block__field
             [:span.consumption-block__label "IBAN"]
             [:span.consumption-block__value (:production/iban production)]])
          (when (:production/adhesion-signed-at production)
            [:div.consumption-block__field
             [:span.consumption-block__contract-link
              {:on-click #(reset! show-contract? true)}
              [:svg {:width "16" :height "16" :viewBox "0 0 24 24"
                     :fill "none" :stroke "currentColor" :stroke-width "2"
                     :stroke-linecap "round" :stroke-linejoin "round"
                     :style {:vertical-align "middle" :margin-right "0.3rem"}}
               [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
               [:polyline {:points "14 2 14 8 20 8"}]
               [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
               [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
               [:polyline {:points "10 9 9 9 8 9"}]]
              "Adhesion a l'association"]])]
         (when @show-contract?
           [:div.modal-overlay {:on-click (fn [e]
                                             (when (= (.-target e) (.-currentTarget e))
                                               (reset! show-contract? false)))}
            [:div.modal
             [:div.modal__header
              [:span "Adhesion a l'association"]
              [:button.btn.btn--small
               {:on-click #(reset! show-contract? false)
                :style {:background "transparent" :color "var(--color-muted)"
                        :border "none" :font-size "1.2rem" :padding "0"}}
               "\u00D7"]]
             [:div.modal__body
              [:pre {:style {:white-space "pre-wrap"
                             :font-size   "0.85rem"
                             :line-height "1.5"}}
               contract/adhesion-contract-text]]
             [:div.modal__actions
              [:button.btn.btn--small.btn--green
               {:on-click #(reset! show-contract? false)}
               "Fermer"]]]])]))))
