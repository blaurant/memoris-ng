(ns app.pages.portal
  (:require [re-frame.core :as rf]))

(defn portal-page []
  (let [user-name @(rf/subscribe [:auth/user-name])
        user      @(rf/subscribe [:auth/user])
        role      @(rf/subscribe [:auth/user-role])]
    [:div.portal-container
     [:h1 "Bienvenue dans l'espace client"]
     [:h2 user-name]
     [:div.portal-info
      [:p [:strong "Email : "] (:email user)]
      [:p [:strong "Rôle : "] role]]
     [:button.btn.btn--green.btn--small
      {:on-click #(rf/dispatch [:auth/logout])}
      "Déconnexion"]]))
