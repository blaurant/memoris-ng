(ns app.pages.login
  (:require [app.components.auth-buttons :as auth-buttons]
            [app.components.password-input :refer [password-input]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfee]))

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
         [password-input
          {:placeholder "Mot de passe"
           :value       @password
           :on-change   #(reset! password (.. % -target -value))}]
         [:button.btn.btn--green
          {:disabled (or loading? (empty? @email) (empty? @password))
           :on-click #(rf/dispatch [:auth/login-with-email @email @password])}
          (if loading? "Connexion..." "Se connecter")]
         [:a {:href     (rfee/href :page/forgot-password)
              :on-click #(rf/dispatch [:auth/set-forgot-email @email])
              :style    {:display "block" :text-align "right" :margin-top "8px"
                         :font-size "0.85rem" :color "var(--color-muted)"}}
          "Mot de passe oublié ?"]]))))

(defn- password-valid? [pw]
  (and (>= (count pw) 8)
       (re-find #"[^a-zA-Z0-9]" pw)))

(defn- rgpd-checkbox [rgpd-ok?]
  [:label.signup-rgpd
   [:input {:type "checkbox" :checked @rgpd-ok?
            :on-change #(swap! rgpd-ok? not)
            :style {:margin-top "3px" :accent-color "var(--color-green)"}}]
   [:span "J'accepte les "
    [:a {:href (rfee/href :page/legal)
         :target "_blank"
         :style {:color "var(--color-green)" :text-decoration "underline"}}
     "mentions l\u00e9gales"]
    " et la politique de confidentialit\u00e9."]])

(defn- email-register-form []
  (let [name*    (r/atom "")
        email    (r/atom "")
        password (r/atom "")
        confirm  (r/atom "")
        rgpd-ok? (r/atom false)]
    (fn []
      (let [loading?      @(rf/subscribe [:auth/loading?])
            error         @(rf/subscribe [:auth/error])
            pw            @password
            pw-ok?        (password-valid? pw)
            match?        (= pw @confirm)
            valid?        (and (seq @name*) (seq @email) pw-ok? (seq @confirm) match? @rgpd-ok?)]
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
         [password-input
          {:placeholder "Mot de passe"
           :value       pw
           :on-change   #(reset! password (.. % -target -value))}]
         (when (and (seq pw) (not pw-ok?))
           [:div {:style {:font-size "0.8rem" :color "#d32f2f" :margin-top "-0.25rem"}}
            "8 caract\u00e8res minimum dont 1 caract\u00e8re sp\u00e9cial"])
         [password-input
          {:placeholder "Confirmer le mot de passe"
           :value       @confirm
           :on-change   #(reset! confirm (.. % -target -value))}]
         (when (and (seq pw) (seq @confirm) (not match?))
           [:div.auth-error "Les mots de passe ne correspondent pas"])
         [rgpd-checkbox rgpd-ok?]
         [:button.btn.btn--green
          {:disabled (or loading? (not valid?))
           :on-click #(rf/dispatch [:auth/register @name* @email @password])}
          (if loading? "Inscription..." "S'inscrire")]]))))

(defn login-page [{:keys [signup?]}]
  (let [join-network   @(rf/subscribe [:eligibility/join-network])
        oauth-rgpd-ok? (r/atom false)]
    (fn [{:keys [signup?]}]
      [:div.login-container
       (if signup?
         [:<>
          [:h1 (if join-network
                 (str "Cr\u00e9er un compte pour rejoindre le r\u00e9seau " join-network)
                 "Cr\u00e9er un compte")]
          [:p (if join-network
                "Cr\u00e9ez votre compte gratuitement pour finaliser votre inscription au r\u00e9seau."
                "Cr\u00e9ez votre compte gratuitement sur Elink-co. Cela ne vous engage en rien.")]
          [:p {:style {:font-size "0.9rem" :color "var(--color-muted)" :margin-top "0.5rem"}}
           "Votre compte vous permettra ensuite d'adh\u00e9rer \u00e0 l'association, "
           "de cr\u00e9er un r\u00e9seau, de consommer ou de produire de l'\u00e9nergie locale."]

          ;; Two-column layout: form left, OAuth right
          [:div.signup-columns
           [:div.signup-columns__form
            [:h3 {:style {:margin-bottom "0.75rem" :font-size "1rem" :color "var(--color-green)"}}
             "Par email"]
            [email-register-form]]
           [:div.signup-columns__separator
            [:span "ou"]]
           [:div.signup-columns__oauth
            [:h3 {:style {:margin-bottom "0.75rem" :font-size "1rem" :color "var(--color-green)"}}
             "Inscription rapide"]
            [auth-buttons/sign-in-buttons
             {:disabled?     (not @oauth-rgpd-ok?)
              :rgpd-checkbox [rgpd-checkbox oauth-rgpd-ok?]}]]]]

         ;; Login
         [:<>
          [:h1 "Connexion"]
          [:p "Connectez-vous pour acc\u00e9der \u00e0 votre espace client."]
          [:div.signup-columns
           [:div.signup-columns__form
            [:h3 {:style {:margin-bottom "0.75rem" :font-size "1rem" :color "var(--color-green)"}}
             "Par email"]
            [email-login-form]]
           [:div.signup-columns__separator
            [:span "ou"]]
           [:div.signup-columns__oauth
            [:h3 {:style {:margin-bottom "0.75rem" :font-size "1rem" :color "var(--color-green)"}}
             "Connexion rapide"]
            [auth-buttons/sign-in-buttons]]]])])))
