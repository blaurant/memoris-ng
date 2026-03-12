(ns app.pages.login
  (:require [app.components.auth-buttons :as auth-buttons]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- email-login-form []
  (let [email    (r/atom "")
        password (r/atom "")]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            error    @(rf/subscribe [:auth/error])]
        [:div.email-auth-form
         (when error
           [:div.auth-error error])
         [:input.onboarding__input
          {:type        "email"
           :placeholder "Adresse email"
           :value       @email
           :on-change   #(reset! email (.. % -target -value))}]
         [:input.onboarding__input
          {:type        "password"
           :placeholder "Mot de passe"
           :value       @password
           :on-change   #(reset! password (.. % -target -value))}]
         [:button.btn.btn--green
          {:disabled (or loading? (empty? @email) (empty? @password))
           :on-click #(rf/dispatch [:auth/login-with-email @email @password])}
          (if loading? "Connexion..." "Se connecter")]]))))

(defn- email-register-form []
  (let [name*    (r/atom "")
        email    (r/atom "")
        password (r/atom "")
        confirm  (r/atom "")]
    (fn []
      (let [loading?      @(rf/subscribe [:auth/loading?])
            error         @(rf/subscribe [:auth/error])
            passwords-ok? (and (seq @password) (= @password @confirm))
            valid?        (and (seq @name*) (seq @email) passwords-ok?)]
        [:div.email-auth-form
         (when error
           [:div.auth-error error])
         [:input.onboarding__input
          {:type        "text"
           :placeholder "Nom complet"
           :value       @name*
           :on-change   #(reset! name* (.. % -target -value))}]
         [:input.onboarding__input
          {:type        "email"
           :placeholder "Adresse email"
           :value       @email
           :on-change   #(reset! email (.. % -target -value))}]
         [:input.onboarding__input
          {:type        "password"
           :placeholder "Mot de passe (8 caractères minimum)"
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
          {:disabled (or loading? (not valid?))
           :on-click #(rf/dispatch [:auth/register @name* @email @password])}
          (if loading? "Inscription..." "S'inscrire")]]))))

(defn login-page [{:keys [signup?]}]
  (let [join-network @(rf/subscribe [:eligibility/join-network])]
    [:div.login-container
     (if signup?
       [:<>
        [:h1 (if join-network
               (str "Pour adhérer et rejoindre le réseau " join-network)
               "Inscription")]
        [:p (if join-network
              "Créez votre compte pour finaliser votre adhésion."
              "Créez votre compte pour accéder à votre espace client.")]
        [email-register-form]
        [:div.auth-separator
         [:span "ou"]]]
       [:<>
        [:h1 "Connexion"]
        [:p "Connectez-vous pour accéder à votre espace client."]
        [email-login-form]
        [:div.auth-separator
         [:span "ou"]]])
     [auth-buttons/sign-in-buttons]]))
