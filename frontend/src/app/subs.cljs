(ns app.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :hello/message
  (fn [db _] (:hello/message db)))

(rf/reg-sub :hello/loading?
  (fn [db _] (:hello/loading? db)))
