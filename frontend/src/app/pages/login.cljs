(ns app.pages.login
  (:require [app.components.auth-buttons :as auth-buttons]))

(defn login-page []
  [:div.login-container
   [:h1 "Connexion"]
   [:p "Connectez-vous pour accéder à votre espace client."]
   [auth-buttons/sign-in-buttons]])
