(ns app.subs
  (:require [re-frame.core :as rf]))

;; ── Hello ─────────────────────────────────────────────────────────────────────

(rf/reg-sub :hello/message
  (fn [db _] (:hello/message db)))

(rf/reg-sub :hello/loading?
  (fn [db _] (:hello/loading? db)))

;; ── Networks ─────────────────────────────────────────────────────────────────

(rf/reg-sub :networks/list
  (fn [db _] (:networks/list db)))

(rf/reg-sub :networks/loading?
  (fn [db _] (:networks/loading? db)))

;; ── Router ───────────────────────────────────────────────────────────────────

(rf/reg-sub :router/current-page
  (fn [db _] (:router/current-page db)))

;; ── Eligibility ───────────────────────────────────────────────────────────────

(rf/reg-sub :eligibility/result
  (fn [db _] (:eligibility/result db)))

(rf/reg-sub :eligibility/loading?
  (fn [db _] (:eligibility/loading? db)))

(rf/reg-sub :eligibility/address
  (fn [db _] (:eligibility/address db)))

(rf/reg-sub :eligibility/lat
  (fn [db _] (:eligibility/lat db)))

(rf/reg-sub :eligibility/lng
  (fn [db _] (:eligibility/lng db)))
