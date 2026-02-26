(ns app.views
  (:require [app.config :as config]
            [app.pages.home :as home]))

(defn- env-banner []
  (when (not= config/APP_ENV "prod")
    [:div {:style {:background "#ff9800" :color "#000" :text-align "center"
                   :padding "4px 0" :font-size "0.75rem" :font-weight "bold"
                   :letter-spacing "0.05em" :text-transform "uppercase"
                   :position "sticky" :top 0 :z-index 9999}}
     config/APP_ENV]))

(defn- navbar []
  [:nav.navbar
   [:a.navbar__logo {:href "/"} "⚡ ProxyWatt"]
   [:span.navbar__tagline "Énergie locale partagée"]])

(defn main-panel []
  [:<>
   [env-banner]
   [navbar]
   [home/home-page]
   [:footer.footer "© 2026 ProxyWatt — Énergie locale partagée"]])
