(ns app.views
  (:require [app.config :as config]
            [app.pages.home :as home]
            [app.pages.login :as login]
            [app.pages.portal :as portal]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfee]))

(defn- env-banner []
  (when (not= config/APP_ENV "prod")
    [:div {:style {:background "#ff9800" :color "#000" :text-align "center"
                   :padding "4px 0" :font-size "0.75rem" :font-weight "bold"
                   :letter-spacing "0.05em" :text-transform "uppercase"
                   :position "sticky" :top 0 :z-index 9999}}
     config/APP_ENV]))

(defn- navbar []
  (let [logged-in? @(rf/subscribe [:auth/logged-in?])
        user-name  @(rf/subscribe [:auth/user-name])]
    [:nav.navbar
     [:a.navbar__logo {:href "/"} "⚡ ProxyWatt"]
     [:span.navbar__tagline "Énergie locale partagée"]
     [:div.navbar__auth
      (if logged-in?
        [:div.navbar__user
         [:span user-name]
         [:button.btn.btn--small
          {:on-click #(rf/dispatch [:auth/logout])}
          "Déconnexion"]]
        [:a.btn.btn--green.btn--small
         {:href (rfee/href :page/login)}
         "Se connecter"])]]))

(defn- current-page []
  (let [page       @(rf/subscribe [:router/current-page])
        logged-in? @(rf/subscribe [:auth/logged-in?])]
    (case page
      :page/login  (if logged-in?
                     (do (rf/dispatch [:router/navigate :page/portal]) nil)
                     [login/login-page])
      :page/portal (if logged-in?
                     [portal/portal-page]
                     (do (rf/dispatch [:router/navigate :page/home]) nil))
      [home/home-page])))

(defn main-panel []
  [:<>
   [env-banner]
   [navbar]
   [current-page]
   [:footer.footer "© 2026 ProxyWatt — Énergie locale partagée"]])
