(ns app.pages.portal
  (:require [app.pages.consumptions :as consumptions]
            [re-frame.core :as rf]))

(defn- sidebar []
  (let [active @(rf/subscribe [:portal/active-section])]
    [:nav.sidebar
     [:ul.sidebar__list
      [:li.sidebar__item
       {:class    (when (= :dashboard active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :dashboard])}
       "Tableau de bord"]
      [:li.sidebar__item
       {:class    (when (= :consumptions active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :consumptions])}
       "Consommations"]]]))

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
        :consumptions [consumptions/consumptions-page]
        [dashboard-section])]]))
