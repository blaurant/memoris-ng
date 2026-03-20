(ns app.consumptions.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :consumptions/list
  (fn [db _] (:consumptions/list db)))

(rf/reg-sub :consumptions/loading?
  (fn [db _] (:consumptions/loading? db)))

(rf/reg-sub :portal/active-section
  (fn [db _] (:portal/active-section db)))

(rf/reg-sub :consumptions/network-name
  :<- [:networks/list]
  (fn [networks [_ network-id]]
    (let [net (some #(when (= (:network/id %) network-id) %) networks)]
      (or (:network/name net) network-id))))

(rf/reg-sub :consumptions/dashboard
  (fn [db [_ consumption-id]]
    (get-in db [:consumptions/dashboards consumption-id])))

(rf/reg-sub :consumptions/dashboard-loading?
  (fn [db [_ consumption-id]]
    (get-in db [:consumptions/dashboard-loading consumption-id])))
