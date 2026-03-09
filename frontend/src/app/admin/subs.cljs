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

(rf/reg-sub :admin/active-tab
  (fn [db _] (:admin/active-tab db)))
