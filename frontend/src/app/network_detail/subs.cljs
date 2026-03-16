(ns app.network-detail.subs
  (:require [re-frame.core :as rf]))

;; ── Layer 2: direct db access ─────────────────────────────────────────────────

(rf/reg-sub :network-detail/data
  (fn [db _] (:network-detail/data db)))

(rf/reg-sub :network-detail/loading?
  (fn [db _] (:network-detail/loading? db)))

(rf/reg-sub :network-detail/error
  (fn [db _] (:network-detail/error db)))

;; ── Layer 3: derived subscriptions ────────────────────────────────────────────

(rf/reg-sub :network-detail/network
  :<- [:network-detail/data]
  (fn [data _]
    (:network data)))

(rf/reg-sub :network-detail/productions
  :<- [:network-detail/data]
  (fn [data _]
    (:productions data)))

(rf/reg-sub :network-detail/stats
  :<- [:network-detail/data]
  (fn [data _]
    (when data
      {:total-capacity-kwc (:total-capacity-kwc data)
       :energy-mix         (:energy-mix data)
       :consumer-count     (:consumer-count data)
       :production-count   (count (:productions data))})))

(rf/reg-sub :network-detail/has-productions?
  :<- [:network-detail/productions]
  (fn [productions _]
    (seq productions)))
