(ns app.components.production-onboarding-form
  (:require [app.productions.contract :as contract]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def steps
  [{:key :installation-info  :label "Installation" :number 1}
   {:key :payment-info       :label "Paiement"     :number 2}
   {:key :contract-signature :label "Contrat"       :number 3}])

(def ^:private step-order
  {:installation-info  0
   :payment-info       1
   :contract-signature 2})

(defn- step-status [step-key current-step]
  (let [idx     (step-order step-key)
        current (step-order current-step)]
    (cond
      (< idx current) :completed
      (= idx current) :active
      :else           :pending)))

(defn- stepper [current-step]
  [:div.stepper
   (doall
     (for [{:keys [key label number]} steps]
       (let [status (step-status key current-step)]
         ^{:key key}
         [:div.stepper__item
          {:class (str "stepper__item--" (name status))}
          [:div.stepper__circle
           (if (= status :completed)
             [:svg {:width "14" :height "14" :viewBox "0 0 24 24"
                    :fill "none" :stroke "currentColor" :stroke-width "3"
                    :stroke-linecap "round" :stroke-linejoin "round"}
              [:polyline {:points "20 6 9 17 4 12"}]]
             (str number))]
          [:div.stepper__label label]])))])

(def ^:private energy-types
  [{:value :solar         :label "Solaire"}
   {:value :wind          :label "Eolien"}
   {:value :hydro         :label "Hydraulique"}
   {:value :biomass       :label "Biomasse"}
   {:value :cogeneration  :label "Cogeneration"}])

(defn- step1-form [production-id]
  (let [pdl-prm     (r/atom "")
        power       (r/atom "")
        energy-type (r/atom "")
        linky-meter (r/atom "")]
    (fn []
      [:div.onboarding__form
       [:label "Numero PDL/PRM"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Ex: 12345678901234"
         :value       @pdl-prm
         :on-change   #(reset! pdl-prm (.. % -target -value))}]
       [:label "Puissance installee (kWc)"]
       [:input.onboarding__input
        {:type        "number"
         :step        "any"
         :placeholder "Ex: 9"
         :value       @power
         :on-change   #(reset! power (.. % -target -value))}]
       [:label "Type d'energie"]
       [:select.onboarding__select
        {:value     @energy-type
         :on-change #(reset! energy-type (.. % -target -value))}
        [:option {:value ""} "Choisir un type"]
        (doall
          (for [{:keys [value label]} energy-types]
            ^{:key value}
            [:option {:value (name value)} label]))]
       [:label "Numero de compteur Linky"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Ex: 09123456789012"
         :value       @linky-meter
         :on-change   #(reset! linky-meter (.. % -target -value))}]
       [:button.btn.btn--green.btn--small
        {:disabled (or (empty? @pdl-prm)
                       (empty? @power)
                       (empty? @energy-type)
                       (empty? @linky-meter))
         :on-click #(rf/dispatch [:productions/submit-step1
                                   production-id @pdl-prm @power
                                   (keyword @energy-type) @linky-meter])}
        "Suivant"]])))

(defn- step2-form [production-id]
  (let [iban (r/atom "")]
    (fn []
      [:div.onboarding__form
       [:label "IBAN"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Ex: FR76 3000 6000 0112 3456 7890 189"
         :value       @iban
         :on-change   #(reset! iban (.. % -target -value))}]
       [:button.btn.btn--green.btn--small
        {:disabled (empty? @iban)
         :on-click #(rf/dispatch [:productions/submit-step2
                                   production-id @iban])}
        "Suivant"]])))

(defn- contract-icon []
  [:svg {:width "24" :height "24" :viewBox "0 0 24 24"
         :fill "none" :stroke "currentColor" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

(defn- step3-form [production-id production]
  (let [show-contract? (r/atom false)]
    (fn [production-id _production]
      (let [signed? (some? (:production/adhesion-signed-at production))]
        [:div.onboarding__form
         [:div.contract-row
          [:div.contract-row__info
           [contract-icon]
           [:span.contract-row__label "Adhesion a l'association"]]
          (if signed?
            [:span.contract-row__signed "Signe\u0301 \u2713"]
            [:button.btn.btn--small.btn--outline
             {:on-click #(reset! show-contract? true)}
             "A signer"])]
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
              [:pre {:style {:white-space      "pre-wrap"
                             :font-size        "0.85rem"
                             :line-height      "1.5"
                             :background-color "var(--color-green-pale)"
                             :padding          "1rem"
                             :border-radius    "var(--radius)"
                             :max-height       "400px"
                             :overflow-y       "auto"}}
               contract/adhesion-contract-text]]
             [:div.modal__actions
              [:button.btn.btn--small.btn--outline
               {:on-click #(reset! show-contract? false)}
               "Annuler"]
              [:button.btn.btn--small.btn--green
               {:on-click (fn []
                            (reset! show-contract? false)
                            (rf/dispatch [:productions/submit-step3 production-id]))}
               "Signer le contrat"]]]])]))))

(defn production-onboarding-form [production]
  (let [lifecycle (keyword (:production/lifecycle production))
        pid       (:production/id production)]
    [:div.consumption-block.consumption-block--onboarding
     [:div.consumption-block__header "Nouvelle Production"]
     [stepper lifecycle]
     (case lifecycle
       :installation-info  [step1-form pid]
       :payment-info       [step2-form pid]
       :contract-signature [step3-form pid production]
       nil)]))
