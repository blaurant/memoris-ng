(ns app.pages.admin
  (:require [re-frame.core :as rf]))

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

;; ── Networks table ────────────────────────────────────────────────────────────

(defn networks-tab []
  (let [networks @(rf/subscribe [:admin/networks])
        loading? @(rf/subscribe [:admin/networks-loading?])]
    [:div
     [:h2.admin__tab-title "Réseaux"]
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
            [:td (:network/radius-km n)]])]])]))
