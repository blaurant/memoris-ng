(ns app.pages.verify-email
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn verify-email-page []
  (let [dispatched? (r/atom false)]
    (fn []
      (let [status @(rf/subscribe [:auth/verification-status])
            error  @(rf/subscribe [:auth/error])
            params (js/URLSearchParams. (.-search js/location))
            token  (.get params "token")]
        (when (and token (not @dispatched?))
          (reset! dispatched? true)
          (rf/dispatch [:auth/verify-email token]))
        [:div.login-container
         [:h1 "Vérification de votre email"]
         (case status
           :loading [:p.loading "Vérification en cours..."]
           :success [:<>
                     [:p "Votre adresse email a été vérifiée avec succès."]
                     [:a.btn.btn--green {:href "/login"} "Se connecter"]]
           :error   [:<>
                     [:p.auth-error (or error "Le lien de vérification est invalide ou a expiré.")]
                     [:a.btn.btn--small {:href "/login"} "Retour à la connexion"]]
           [:<>
            (if token
              [:p.loading "Vérification en cours..."]
              [:p.auth-error "Aucun token de vérification trouvé."])])]))))
