(ns app.core
  (:require [app.events]
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
  (routes/init!)
  (mount-root))
