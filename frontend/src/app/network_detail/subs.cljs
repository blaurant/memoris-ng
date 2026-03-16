(ns app.network-detail.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :network-detail/data
  (fn [db _] (:network-detail/data db)))

(rf/reg-sub :network-detail/loading?
  (fn [db _] (:network-detail/loading? db)))

(rf/reg-sub :network-detail/error
  (fn [db _] (:network-detail/error db)))
