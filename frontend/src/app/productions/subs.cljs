(ns app.productions.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :productions/list
  (fn [db _] (:productions/list db)))

(rf/reg-sub :productions/loading?
  (fn [db _] (:productions/loading? db)))

(rf/reg-sub :productions/dashboard
  (fn [db [_ production-id]]
    (get-in db [:productions/dashboards production-id])))

(rf/reg-sub :productions/dashboard-loading?
  (fn [db [_ production-id]]
    (get-in db [:productions/dashboard-loading production-id])))
