(ns app.auth.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :auth/token
  (fn [db _] (:auth/token db)))

(rf/reg-sub :auth/user
  (fn [db _] (:auth/user db)))

(rf/reg-sub :auth/loading?
  (fn [db _] (:auth/loading? db)))

(rf/reg-sub :auth/error
  (fn [db _] (:auth/error db)))

(rf/reg-sub :auth/logged-in?
  :<- [:auth/token]
  (fn [token _] (some? token)))

(rf/reg-sub :auth/user-name
  :<- [:auth/user]
  (fn [user _] (:name user)))

(rf/reg-sub :auth/user-role
  :<- [:auth/user]
  (fn [user _] (:role user)))
