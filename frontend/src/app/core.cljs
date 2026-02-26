(ns app.core
  (:require [app.auth.events]
            [app.auth.subs]
            [app.events]
            [app.routes :as routes]
            [app.subs]
            [app.views :as views]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]
            [reitit.frontend :as rfe]))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (rdom/render [views/main-panel]
               (.getElementById js/document "app")))

(defn on-reload []
  (mount-root))

(defn init []
  (rf/dispatch-sync [:app/initialize])
  (rf/dispatch-sync [:auth/restore-session])
  ;; Resolve initial route synchronously before first render
  (let [match (rfe/match-by-path routes/router (.-pathname js/location))
        page  (or (-> match :data :name) :page/home)]
    (rf/dispatch-sync [:router/navigated page]))
  (routes/init!)
  (mount-root))
