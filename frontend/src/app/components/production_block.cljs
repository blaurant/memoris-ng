(ns app.components.production-block
  (:require [app.productions.contract :as contract]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private energy-type-labels
  {"solar"        "Solaire"
   "wind"         "Eolien"
   "hydro"        "Hydraulique"
   "biomass"      "Biomasse"
   "cogeneration" "Cogénération"})

(defn- energy-icon [energy-type]
  (case energy-type
    "solar"
    [:svg {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
           :stroke "currentColor" :stroke-width "2"
           :stroke-linecap "round" :stroke-linejoin "round"}
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
    [:svg {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
           :stroke "currentColor" :stroke-width "2"
           :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M9.59 4.59A2 2 0 1 1 11 8H2"}]
     [:path {:d "M12.59 19.41A2 2 0 1 0 14 16H2"}]
     [:path {:d "M17.73 7.73A2.5 2.5 0 1 1 19.5 12H2"}]]
    "hydro"
    [:svg {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
           :stroke "currentColor" :stroke-width "2"
           :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z"}]]
    "biomass"
    [:svg {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
           :stroke "currentColor" :stroke-width "2"
           :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66"}]
     [:path {:d "M20.59 3.41A21 21 0 0 0 3 21c5-4 8-6 11-7s6-2 8-4a3 3 0 0 0-1.41-5.59z"}]]
    "cogeneration"
    [:svg {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
           :stroke "currentColor" :stroke-width "2"
           :stroke-linecap "round" :stroke-linejoin "round"}
     [:path {:d "M13 2L3 14h9l-1 8 10-12h-9l1-8z"}]]
    ;; default
    [:svg {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
           :stroke "currentColor" :stroke-width "2"
           :stroke-linecap "round" :stroke-linejoin "round"}
     [:circle {:cx "12" :cy "12" :r "10"}]]))

(defn- confirm-delete-modal [production on-confirm on-cancel]
  [:div.modal-overlay {:on-click (fn [e]
                                    (when (= (.-target e) (.-currentTarget e))
                                      (on-cancel)))}
   [:div.modal {:on-click #(.stopPropagation %)}
    [:div.modal__header
     [:span "Supprimer la production"]
     [:button.btn.btn--small {:on-click on-cancel
                              :style {:background "transparent" :color "var(--color-muted)"
                                      :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:p {:style {:margin-bottom "0.5rem" :font-weight "600" :color "#d32f2f"}}
      "Cette action est irréversible."]
     [:p {:style {:margin-bottom "0.5rem"}}
      (str "Voulez-vous vraiment supprimer cette production "
           (get energy-type-labels (:production/energy-type production) "")
           " de " (:production/installed-power production) " kWc ?")]
     [:p {:style {:color "var(--color-muted)" :font-size "0.9rem"}}
      "Les administrateurs seront notifiés de cette suppression."]]
    [:div.modal__actions
     [:button.btn.btn--small {:on-click on-cancel} "Annuler"]
     [:button.btn.btn--small {:on-click on-confirm
                              :style {:background "#d32f2f" :color "#fff"}}
      "Supprimer"]]]])

(defn production-menu
  "Dropdown menu with delete action for a production. Reusable in dashboard."
  [production]
  (let [show-menu?      (r/atom false)
        confirm-delete? (r/atom false)]
    (fn [production]
      (let [pid (:production/id production)]
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
           [confirm-delete-modal production
            (fn []
              (rf/dispatch [:productions/delete pid])
              (reset! confirm-delete? false))
            #(reset! confirm-delete? false)])]))))

(defn production-block [production]
  (let [show-contract? (r/atom false)
        show-menu?     (r/atom false)
        confirm-delete? (r/atom false)]
    (fn [production]
      (let [energy-type (:production/energy-type production)
            lifecycle   (:production/lifecycle production)
            pid         (:production/id production)]
        [:div.consumption-block
         [:div.consumption-block__header
          [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
           [:span {:style {:color "var(--color-accent)" :display "flex"}} [energy-icon energy-type]]
           [:span (str (get energy-type-labels energy-type energy-type) " — " (:production/installed-power production) " kWc")]]
          [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
           [:span.consumption-block__status lifecycle]
           ;; Menu déroulant
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
                "Supprimer"]])]]]
         [:div.consumption-block__details
          (when (or (:production/network-name production) (:production/producer-address production))
            [:div.consumption-block__field
             [:span.consumption-block__label "Réseau"]
             [:div
              (when (:production/network-name production)
                [:span.consumption-block__value {:style {:display "block"}}
                 (:production/network-name production)])
              (when (:production/producer-address production)
                [:span {:style {:display "block" :font-size "0.85rem" :color "var(--color-muted)"}}
                 (:production/producer-address production)])]])
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
          (when (some? (:adhesion-signed-at @(rf/subscribe [:auth/user])))
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
              "Adhésion Elink-co"]])]
         (when @show-contract?
           [:div.modal-overlay {:on-click (fn [e]
                                             (when (= (.-target e) (.-currentTarget e))
                                               (reset! show-contract? false)))}
            [:div.modal
             [:div.modal__header
              [:span "Adhésion Elink-co"]
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
               "Fermer"]]]])
         (when @confirm-delete?
           [confirm-delete-modal production
            (fn []
              (rf/dispatch [:productions/delete pid])
              (reset! confirm-delete? false))
            #(reset! confirm-delete? false)])]))))
