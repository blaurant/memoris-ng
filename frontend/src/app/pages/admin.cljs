(ns app.pages.admin
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

;; ── Users table ───────────────────────────────────────────────────────────────

(defn- users-tab []
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

;; ── Networks table ────────────────────────────────────────────────────────────

(defn- networks-tab []
  (let [networks @(rf/subscribe [:admin/networks])
        loading? @(rf/subscribe [:admin/networks-loading?])]
    [:div
     [:h2.admin__tab-title "Reseaux"]
     (cond
       loading?
       [:p.loading "Chargement..."]

       (empty? networks)
       [:p.admin__empty "Aucun reseau."]

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
            [:td (:network/radius-km n)]])]])]))

;; ── Admin page ────────────────────────────────────────────────────────────────

(defn admin-page []
  (r/create-class
    {:component-did-mount
     (fn [_]
       (rf/dispatch [:admin/fetch-users])
       (rf/dispatch [:admin/fetch-networks]))

     :reagent-render
     (fn []
       (let [active-tab @(rf/subscribe [:admin/active-tab])]
         [:div.portal
          [:aside.portal__sidebar
           [:nav.sidebar
            [:ul.sidebar__list
             [:li {:class    (str "sidebar__item"
                                  (when (= :users active-tab) " sidebar__item--active"))
                   :on-click #(rf/dispatch [:admin/set-tab :users])}
              "Utilisateurs"]
             [:li {:class    (str "sidebar__item"
                                  (when (= :networks active-tab) " sidebar__item--active"))
                   :on-click #(rf/dispatch [:admin/set-tab :networks])}
              "Reseaux"]]]]
          [:main.portal__content
           [:h1.dashboard__title "Administration"]
           (case active-tab
             :networks [networks-tab]
             [users-tab])]]))}))
