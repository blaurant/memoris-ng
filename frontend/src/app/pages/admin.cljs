(ns app.pages.admin
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

;; ── Users table ───────────────────────────────────────────────────────────────

(defn users-tab []
  (let [users    @(rf/subscribe [:admin/users])
        loading? @(rf/subscribe [:admin/users-loading?])]
    [:div
     [:h2.admin__tab-title "Utilisateurs"]
     (cond
       loading?
       [:p.loading "Chargement..."]

       (empty? users)
       [:p.admin__empty "Aucun utilisateur."]

       :else
       [:table.admin-table
        [:thead
         [:tr
          [:th "Nom"]
          [:th "Email"]
          [:th "Fournisseur"]
          [:th "Role"]
          [:th "Statut"]]]
        [:tbody
         (for [u users]
           ^{:key (:user/id u)}
           [:tr
            [:td (:user/name u)]
            [:td (:user/email u)]
            [:td (:user/provider u)]
            [:td {:class (when (= "admin" (:user/role u)) "admin-table__role--admin")}
             (:user/role u)]
            [:td (:user/lifecycle u)]])]])]))

;; ── Create network modal ──────────────────────────────────────────────────────

(defn- create-network-modal [on-close]
  (let [form (r/atom {:name "" :center-lat "" :center-lng "" :radius-km "10"})]
    (fn [on-close]
      (let [{:keys [name center-lat center-lng radius-km]} @form
            valid? (and (seq name)
                        (seq center-lat)
                        (seq center-lng)
                        (seq radius-km))]
        [:div.modal-overlay {:on-click on-close}
         [:div.modal {:on-click #(.stopPropagation %)}
          [:div.modal__header
           [:span "Ajouter un réseau"]
           [:button.btn.btn--small {:on-click on-close} "X"]]
          [:div.modal__body
           [:div.onboarding__form
            [:label "Nom"]
            [:input.onboarding__input
             {:value       name
              :placeholder "Nom du réseau"
              :on-change   #(swap! form assoc :name (.-value (.-target %)))}]
            [:label "Latitude du centre"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       center-lat
              :placeholder "ex: 48.8566"
              :on-change   #(swap! form assoc :center-lat (.-value (.-target %)))}]
            [:label "Longitude du centre"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       center-lng
              :placeholder "ex: 2.3522"
              :on-change   #(swap! form assoc :center-lng (.-value (.-target %)))}]
            [:label "Rayon (km)"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       radius-km
              :placeholder "10"
              :on-change   #(swap! form assoc :radius-km (.-value (.-target %)))}]]]
          [:div.modal__actions
           [:button.btn.btn--small {:on-click on-close} "Annuler"]
           [:button.btn.btn--green.btn--small
            {:disabled (not valid?)
             :on-click (fn []
                         (rf/dispatch [:admin/create-network
                                       {:name       name
                                        :center-lat (js/parseFloat center-lat)
                                        :center-lng (js/parseFloat center-lng)
                                        :radius-km  (js/parseFloat radius-km)}])
                         (on-close))}
            "Créer"]]]]))))

;; ── Networks table ────────────────────────────────────────────────────────────

(defn networks-tab []
  (let [show-modal? (r/atom false)]
    (fn []
      (let [networks @(rf/subscribe [:admin/networks])
            loading? @(rf/subscribe [:admin/networks-loading?])]
        [:div
         [:div.consumptions__header
          [:h2.admin__tab-title "Réseaux"]
          [:button.btn.btn--green.btn--small
           {:on-click #(reset! show-modal? true)}
           "+ Ajouter un réseau"]]
         (cond
           loading?
           [:p.loading "Chargement..."]

           (empty? networks)
           [:p.admin__empty "Aucun réseau."]

           :else
           [:table.admin-table
            [:thead
             [:tr
              [:th "Nom"]
              [:th "Latitude"]
              [:th "Longitude"]
              [:th "Rayon (km)"]]]
            [:tbody
             (for [n networks]
               ^{:key (:network/id n)}
               [:tr
                [:td (:network/name n)]
                [:td (:network/center-lat n)]
                [:td (:network/center-lng n)]
                [:td (:network/radius-km n)]])]])
         (when @show-modal?
           [create-network-modal #(reset! show-modal? false)])]))))
