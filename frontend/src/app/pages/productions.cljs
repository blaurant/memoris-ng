(ns app.pages.productions
  (:require [app.components.production-block :as block]
            [app.components.production-onboarding-form :as onboarding]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private onboarding-states
  #{"producer-information" "installation-info" "payment-info" "contract-signature"})

(defn- onboarding? [production]
  (contains? onboarding-states (:production/lifecycle production)))

(defn productions-page []
  (r/create-class
    {:component-did-mount
     (fn [_]
       (rf/dispatch [:productions/fetch])
       (rf/dispatch [:networks/fetch]))

     :reagent-render
     (fn []
       (let [productions @(rf/subscribe [:productions/list])
             loading?    @(rf/subscribe [:productions/loading?])]
         [:div.consumptions
          [:div.consumptions__header
           [:h2 "Mes productions"]
           (when (seq productions)
             [:button.btn.btn--green.btn--small
              {:on-click #(rf/dispatch [:productions/create])}
              [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
                     :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
                     :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"
                     :style {:vertical-align "middle" :margin-right "4px"}}
               [:circle {:cx "12" :cy "12" :r "10"}]
               [:line {:x1 "12" :y1 "8" :x2 "12" :y2 "16"}]
               [:line {:x1 "8" :y1 "12" :x2 "16" :y2 "12"}]]
              "Produire"])]
          (cond
            loading?
            [:p.loading "Chargement..."]

            (empty? productions)
            [:div.consumptions__empty
             [:p "Aucune production pour le moment."]
             [:button.btn.btn--green
              {:on-click #(rf/dispatch [:productions/create])
               :style {:margin-top "1rem"}}
              "Ajouter une production"]]

            :else
            [:div.consumptions__list
             (doall
               (for [p productions]
                 ^{:key (:production/id p)}
                 [:div
                  (if (onboarding? p)
                    [onboarding/production-onboarding-form p]
                    [block/production-block p])]))])]))}))
