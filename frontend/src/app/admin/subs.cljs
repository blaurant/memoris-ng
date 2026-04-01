(ns app.admin.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :admin/users
  (fn [db _] (:admin/users db)))

(rf/reg-sub :admin/users-loading?
  (fn [db _] (:admin/users-loading? db)))

(rf/reg-sub :admin/networks
  (fn [db _] (:admin/networks db)))

(rf/reg-sub :admin/networks-loading?
  (fn [db _] (:admin/networks-loading? db)))

(rf/reg-sub :admin/eligibility-checks
  (fn [db _] (:admin/eligibility-checks db)))

(rf/reg-sub :admin/eligibility-checks-loading?
  (fn [db _] (:admin/eligibility-checks-loading? db)))

(rf/reg-sub :admin/consumptions
  (fn [db _] (:admin/consumptions db)))

(rf/reg-sub :admin/consumptions-loading?
  (fn [db _] (:admin/consumptions-loading? db)))

(rf/reg-sub :admin/productions
  (fn [db _] (:admin/productions db)))

(rf/reg-sub :admin/productions-loading?
  (fn [db _] (:admin/productions-loading? db)))

(rf/reg-sub :admin/active-tab
  (fn [db _] (:admin/active-tab db)))

(rf/reg-sub :admin/production-error
  (fn [db _] (:admin/production-error db)))

(rf/reg-sub :admin/network-delete-blocked
  (fn [db _] (:admin/network-delete-blocked db)))

(rf/reg-sub :admin/alert-message
  (fn [db _] (:admin/alert-message db)))

(rf/reg-sub :admin/alert-active?
  (fn [db _] (:admin/alert-active? db)))

(rf/reg-sub :admin/news-list
  (fn [db _] (:admin/news-list db)))

(rf/reg-sub :admin/news-loading?
  (fn [db _] (:admin/news-loading? db)))
