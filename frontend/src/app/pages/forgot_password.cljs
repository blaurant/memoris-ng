(ns app.pages.forgot-password
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn forgot-password-page []
  (let [initial-email @(rf/subscribe [:auth/forgot-email])
        email         (r/atom (or initial-email ""))]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            sent?    @(rf/subscribe [:auth/forgot-password-sent?])]
        [:div.login-container
         [:h1 "Mot de passe oublié"]
         (if sent?
           [:div
            [:p {:style {:color "var(--color-green)" :font-weight "600" :margin-bottom "1rem"}}
             "Si cette adresse email est associée à un compte, vous recevrez un lien de réinitialisation."]
            [:p "Vous allez être redirigé vers la page de connexion..."]]
           [:div
            [:p "Entrez votre adresse email. Si un compte existe, vous recevrez un lien pour réinitialiser votre mot de passe."]
            [:div.email-auth-form
             [:input.onboarding__input
              {:type        "email"
               :placeholder "Adresse email"
               :value       @email
               :on-change   #(reset! email (.. % -target -value))}]
             [:button.btn.btn--green
              {:disabled (or loading? (empty? @email))
               :on-click #(rf/dispatch [:auth/forgot-password @email])}
              (if loading? "Envoi..." "Envoyer")]]])]))))
