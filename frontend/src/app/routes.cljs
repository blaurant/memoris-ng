(ns app.routes
  (:require [re-frame.core :as rf]
            [reitit.frontend :as rfe]
            [reitit.frontend.easy :as rfee]))

(def router
  (rfe/router
   [["/"              {:name :page/home}]
    ["/login"          {:name :page/login}]
    ["/signup"         {:name :page/signup}]
    ["/portal"         {:name :page/portal}]
    ["/verify-email"   {:name :page/verify-email}]
    ["/check-email"       {:name :page/check-email}]
    ["/forgot-password"   {:name :page/forgot-password}]
    ["/reset-password"    {:name :page/reset-password}]
    ["/reseau/:id"        {:name :page/network-detail}]
    ["/faq"               {:name :page/faq}]
    ["/qui-sommes-nous"   {:name :page/about}]
    ["/comment-ca-marche" {:name :page/how-it-works}]]))

(defn init!
  "Starts reitit HTML5 history listener. Dispatches :router/navigated on each change."
  []
  (rfee/start!
    router
    (fn [match _]
      (let [page   (or (-> match :data :name) :page/home)
            params (-> match :parameters :path)]
        (rf/dispatch [:router/navigated page params])))
    {:use-fragment false}))
