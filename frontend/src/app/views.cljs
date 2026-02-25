(ns app.views
  (:require [app.pages.home :as home]))

(defn- navbar []
  [:nav.navbar
   [:a.navbar__logo {:href "/"} "Wattprox"]
   [:span.navbar__tagline "Énergie locale partagée"]])

(defn main-panel []
  [:<>
   [navbar]
   [home/home-page]
   [:footer.footer "© 2026 Wattprox — Énergie locale partagée"]])
