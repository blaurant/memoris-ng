(ns app.pages.consumptions
  (:require [app.components.consumption-block :as block]
            [app.components.onboarding-form :as onboarding]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private onboarding-states
  #{"consumer-information" "linky-reference" "billing-address" "contract-signature"})

(defn- onboarding? [consumption]
  (contains? onboarding-states (:consumption/lifecycle consumption)))

(defn consumptions-page []
  (r/create-class
    {:component-did-mount
     (fn [_]
       (rf/dispatch [:consumptions/fetch])
       (rf/dispatch [:networks/fetch]))

     :reagent-render
     (fn []
       (let [consumptions @(rf/subscribe [:consumptions/list])
             loading?     @(rf/subscribe [:consumptions/loading?])]
         [:div.consumptions
          [:div.consumptions__header
           [:h2 "Mes consommations"]
           [:button.btn.btn--green.btn--small
            {:on-click #(rf/dispatch [:consumptions/create])}
            "+ Consommer"]]
          (cond
            loading?
            [:p.loading "Chargement..."]

            (empty? consumptions)
            [:p.consumptions__empty "Aucune consommation pour le moment."]

            :else
            [:div.consumptions__list
             (doall
               (for [c consumptions]
                 ^{:key (:consumption/id c)}
                 [:div
                  (if (onboarding? c)
                    [onboarding/onboarding-form c]
                    [block/consumption-block c])]))])]))}))
