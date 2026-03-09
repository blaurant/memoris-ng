(ns app.pages.portal
  (:require [app.pages.admin :as admin]
            [app.pages.consumptions :as consumptions]
            [re-frame.core :as rf]))

(defn- sidebar []
  (let [active @(rf/subscribe [:portal/active-section])
        admin? @(rf/subscribe [:auth/admin?])]
    [:nav.sidebar
     [:ul.sidebar__list
      [:li.sidebar__item
       {:class    (when (= :dashboard active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :dashboard])}
       "Tableau de bord"]
      [:li.sidebar__item
       {:class    (when (= :consumptions active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :consumptions])}
       "Consommations"]
      (when admin?
        [:<>
         [:li.sidebar__item.sidebar__item--separator]
         [:li.sidebar__item
          {:class    (when (= :admin-users active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-users])
                          (rf/dispatch [:admin/fetch-users]))}
          "Utilisateurs"]
         [:li.sidebar__item
          {:class    (when (= :admin-networks active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-networks])
                          (rf/dispatch [:admin/fetch-networks]))}
          "Réseaux"]])]]))

(defn- dashboard-section []
  (let [user-name @(rf/subscribe [:auth/user-name])
        user      @(rf/subscribe [:auth/user])
        role      @(rf/subscribe [:auth/user-role])]
    [:div.dashboard
     [:h1 "Bienvenue dans l'espace client"]
     [:h2 user-name]
     [:div.portal-info
      [:p [:strong "Email : "] (:email user)]
      [:p [:strong "Rôle : "] role]]
     [:button.btn.btn--green.btn--small
      {:on-click #(rf/dispatch [:auth/logout])}
      "Déconnexion"]]))

(defn portal-page []
  (let [active @(rf/subscribe [:portal/active-section])]
    [:div.portal
     [:div.portal__sidebar
      [sidebar]]
     [:div.portal__content
      (case active
        :consumptions  [consumptions/consumptions-page]
        :admin-users   [admin/users-tab]
        :admin-networks [admin/networks-tab]
        [dashboard-section])]]))
