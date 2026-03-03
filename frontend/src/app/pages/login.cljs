(ns app.pages.login
  (:require [app.components.auth-buttons :as auth-buttons]))

(defn login-page [{:keys [signup?]}]
  [:div.login-container
   (if signup?
     [:<>
      [:h1 "Inscription"]
      [:p "Inscription pour accéder à votre espace client"]]
     [:<>
      [:h1 "Connexion"]
      [:p "Connectez-vous pour accéder à votre espace client."]])
   [auth-buttons/sign-in-buttons]])
