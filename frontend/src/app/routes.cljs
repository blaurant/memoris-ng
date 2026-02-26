(ns app.routes
  (:require [re-frame.core :as rf]
            [reitit.frontend :as rfe]
            [reitit.frontend.easy :as rfee]))

(def router
  (rfe/router
   [["/"       {:name :page/home}]
    ["/login"  {:name :page/login}]
    ["/portal" {:name :page/portal}]]))

(defn init!
  "Starts reitit HTML5 history listener. Dispatches :router/navigated on each change."
  []
  (rfee/start!
    router
    (fn [match _]
      (let [page (or (-> match :data :name) :page/home)]
        (rf/dispatch [:router/navigated page])))
    {:use-fragment false}))
