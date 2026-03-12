(ns app.components.consumption-block
  (:require [app.consumptions.contract :as contract]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private contract-links
  [{:label      "Contrat Elinkco"
    :text       contract/contract-text
    :signed-key :consumption/contract-signed-at}
   {:label      "Contrat Producteur"
    :text       contract/producer-contract-text
    :signed-key :consumption/producer-contract-signed-at}
   {:label      "Mandat SEPA"
    :text       contract/sepa-mandate-text
    :signed-key :consumption/sepa-mandate-signed-at}])

(defn consumption-block [consumption]
  (let [open-contract (r/atom nil)]
    (fn [consumption]
      (let [network-id   (:consumption/network-id consumption)
            network-name @(rf/subscribe [:consumptions/network-name network-id])
            lifecycle    (:consumption/lifecycle consumption)]
        [:div.consumption-block
         [:div.consumption-block__header
          [:span (or network-name "Réseau")]
          [:span.consumption-block__status lifecycle]]
         [:div.consumption-block__details
          (when-let [date (:consumption/contract-start-date consumption)]
            [:div.consumption-block__field
             [:span.consumption-block__label "Date contrat"]
             [:span.consumption-block__value date]])
          (when-let [price (:consumption/price-per-kwh consumption)]
            [:div.consumption-block__field
             [:span.consumption-block__label "Prix kWh"]
             [:span.consumption-block__value (str price " EUR/kWh")]])
          (when-let [kwh (:consumption/last-monthly-kwh consumption)]
            [:div.consumption-block__field
             [:span.consumption-block__label "Dernière conso"]
             [:span.consumption-block__value (str kwh " kWh")]])
          (doall
            (for [{:keys [label signed-key]} contract-links]
              (when (some? (get consumption signed-key))
                ^{:key label}
                [:div.consumption-block__field
                 [:span.consumption-block__contract-link
                  {:on-click #(reset! open-contract label)}
                  [:svg {:width "16" :height "16" :viewBox "0 0 24 24"
                         :fill "none" :stroke "currentColor" :stroke-width "2"
                         :stroke-linecap "round" :stroke-linejoin "round"
                         :style {:vertical-align "middle" :margin-right "0.3rem"}}
                   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
                   [:polyline {:points "14 2 14 8 20 8"}]
                   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
                   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
                   [:polyline {:points "10 9 9 9 8 9"}]]
                  label]])))]
         (when-let [lbl @open-contract]
           (let [{:keys [text]} (first (filter #(= lbl (:label %)) contract-links))]
             [:div.modal-overlay {:on-click (fn [e]
                                              (when (= (.-target e) (.-currentTarget e))
                                                (reset! open-contract nil)))}
              [:div.modal
               [:div.modal__header
                [:span lbl]
                [:button.btn.btn--small
                 {:on-click #(reset! open-contract nil)
                  :style {:background "transparent" :color "var(--color-muted)"
                          :border "none" :font-size "1.2rem" :padding "0"}}
                 "\u00D7"]]
               [:div.modal__body
                [:pre {:style {:white-space "pre-wrap"
                               :font-size   "0.85rem"
                               :line-height "1.5"}}
                 text]]
               [:div.modal__actions
                [:button.btn.btn--small.btn--green
                 {:on-click #(reset! open-contract nil)}
                 "Fermer"]]]]))]))))
