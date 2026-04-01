(ns app.pages.reset-password
  (:require [app.components.password-input :refer [password-input]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- password-valid? [pw]
  (and (>= (count pw) 8)
       (re-find #"[^a-zA-Z0-9]" pw)))

(defn reset-password-page []
  (let [password (r/atom "")
        confirm  (r/atom "")
        token    (-> js/location .-search (js/URLSearchParams.) (.get "token"))]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            error    @(rf/subscribe [:auth/error])
            success? @(rf/subscribe [:auth/reset-password-success?])
            pw       @password
            pw-ok?   (password-valid? pw)
            match?   (and pw-ok? (= pw @confirm))]
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
            [password-input
             {:placeholder "Nouveau mot de passe"
              :value       pw
              :on-change   #(reset! password (.. % -target -value))}]
            (when (and (seq pw) (not pw-ok?))
              [:div {:style {:font-size "0.8rem" :color "#d32f2f" :margin-top "-0.25rem"}}
               "8 caractères minimum dont 1 caractère spécial"])
            [password-input
             {:placeholder "Confirmer le mot de passe"
              :value       @confirm
              :on-change   #(reset! confirm (.. % -target -value))}]
            (when (and (seq pw) (seq @confirm) (not= pw @confirm))
              [:div.auth-error "Les mots de passe ne correspondent pas"])
            [:button.btn.btn--green
             {:disabled (or loading? (not match?))
              :on-click #(rf/dispatch [:auth/reset-password token pw])}
             (if loading? "Enregistrement..." "Enregistrer le nouveau mot de passe")]])]))))
