(ns app.routes
  (:require [re-frame.core :as rf]
            [reitit.frontend :as rfe]))

(def router
  (rfe/router
   [["/" {:name :page/home}]]))

(defn- url->page
  "Resolves the current browser path to a page name."
  []
  (let [path (.-pathname js/location)
        match (rfe/match-by-path router path)]
    (or (-> match :data :name) :page/home)))

(defn init!
  "Reads the current URL, dispatches the matching page."
  []
  (rf/dispatch-sync [:router/navigated (url->page)]))
