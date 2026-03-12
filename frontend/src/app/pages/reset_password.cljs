(ns app.pages.reset-password
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn reset-password-page []
  (let [password (r/atom "")
        confirm  (r/atom "")
        token    (-> js/location .-search (js/URLSearchParams.) (.get "token"))]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            error    @(rf/subscribe [:auth/error])
            success? @(rf/subscribe [:auth/reset-password-success?])
            match?   (and (seq @password) (= @password @confirm))]
        [:div.login-container
         [:h1 "Nouveau mot de passe"]
         (cond
           success?
           [:div
            [:p {:style {:color "var(--color-green)" :font-weight "600" :margin-bottom "1rem"}}
             "Votre mot de passe a été modifié avec succès."]
            [:p "Vous allez être redirigé vers la page de connexion..."]]

           (nil? token)
           [:p.auth-error "Lien de réinitialisation invalide."]

           :else
           [:div.email-auth-form
            (when error
              [:div.auth-error error])
            [:input.onboarding__input
             {:type        "password"
              :placeholder "Nouveau mot de passe (8 caractères minimum)"
              :value       @password
              :on-change   #(reset! password (.. % -target -value))}]
            [:input.onboarding__input
             {:type        "password"
              :placeholder "Confirmer le mot de passe"
              :value       @confirm
              :on-change   #(reset! confirm (.. % -target -value))}]
            (when (and (seq @password) (seq @confirm) (not= @password @confirm))
              [:div.auth-error "Les mots de passe ne correspondent pas"])
            [:button.btn.btn--green
             {:disabled (or loading? (not match?) (< (count @password) 8))
              :on-click #(rf/dispatch [:auth/reset-password token @password])}
             (if loading? "Enregistrement..." "Enregistrer le nouveau mot de passe")]])]))))
