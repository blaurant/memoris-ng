(ns app.core
  (:require [app.auth.events]
            [app.auth.subs]
            [app.events]
            [app.routes :as routes]
            [app.subs]
            [app.views :as views]
            [re-frame.core :as rf]
            [reagent.dom :as rdom]))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (rdom/render [views/main-panel]
               (.getElementById js/document "app")))

(defn on-reload []
  (mount-root))

(defn init []
  (rf/dispatch-sync [:app/initialize])
  (rf/dispatch-sync [:auth/restore-session])
  (routes/init!)
  (mount-root))
