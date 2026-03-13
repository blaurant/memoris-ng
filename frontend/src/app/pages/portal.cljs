(ns app.pages.portal
  (:require [app.pages.admin :as admin]
            [app.pages.consumptions :as consumptions]
            [app.pages.productions :as productions]
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
      [:li.sidebar__item
       {:class    (when (= :productions active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :productions])}
       "Productions"]
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
          "Réseaux"]
         [:li.sidebar__item
          {:class    (when (= :admin-eligibility active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-eligibility])
                          (rf/dispatch [:admin/fetch-eligibility-checks]))}
          "Visiteurs"]
         [:li.sidebar__item
          {:class    (when (= :admin-productions active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-productions])
                          (rf/dispatch [:admin/fetch-productions]))}
          "Productions"]
         [:li.sidebar__item
          {:class    (when (= :admin-alert active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-alert])
                          (rf/dispatch [:admin/fetch-alert]))}
          "Alerte"]])]]))

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
        :consumptions      [consumptions/consumptions-page]
        :productions       [productions/productions-page]
        :admin-users       [admin/users-tab]
        :admin-networks    [admin/networks-tab]
        :admin-eligibility [admin/eligibility-checks-tab]
        :admin-productions [admin/productions-tab]
        :admin-alert       [admin/alert-tab]
        [dashboard-section])]]))
