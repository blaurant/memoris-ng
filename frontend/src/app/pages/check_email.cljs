(ns app.pages.check-email
  (:require [re-frame.core :as rf]))

(defn check-email-page []
  (let [email          @(rf/subscribe [:auth/register-email])
        resend-success? @(rf/subscribe [:auth/resend-success?])]
    [:div.login-container
     [:h1 "Vérifiez votre email"]
     [:p (str "Un email de vérification a été envoyé à "
              (or email "votre adresse")
              ". Vérifiez votre boîte de réception.")]
     [:p "Ce lien expire dans 24 heures."]
     (if resend-success?
       [:p.auth-success "Email renvoyé avec succès."]
       (when email
         [:button.btn.btn--small.btn--outline
          {:on-click #(rf/dispatch [:auth/resend-verification email])}
          "Renvoyer l'email de vérification"]))
     [:a.btn.btn--small {:href "/login"} "Retour à la connexion"]]))
