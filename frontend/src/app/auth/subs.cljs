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

(rf/reg-sub :auth/admin?
  :<- [:auth/user-role]
  (fn [role _] (= "admin" role)))

(rf/reg-sub :auth/register-success?
  (fn [db _] (:auth/register-success? db)))

(rf/reg-sub :auth/register-email
  (fn [db _] (:auth/register-email db)))

(rf/reg-sub :auth/verification-status
  (fn [db _] (:auth/verification-status db)))

(rf/reg-sub :auth/resend-success?
  (fn [db _] (:auth/resend-success? db)))

(rf/reg-sub :auth/forgot-email
  (fn [db _] (:auth/forgot-email db)))

(rf/reg-sub :auth/forgot-password-sent?
  (fn [db _] (:auth/forgot-password-sent? db)))

(rf/reg-sub :auth/reset-password-success?
  (fn [db _] (:auth/reset-password-success? db)))

(rf/reg-sub :auth/docuseal-signing-url
  (fn [db _] (:auth/docuseal-signing-url db)))

(rf/reg-sub :auth/adhesion-loading?
  (fn [db _] (:auth/adhesion-loading? db)))

(rf/reg-sub :auth/profile-complete?
  :<- [:auth/user]
  (fn [user _]
    (let [np (:natural-person user)]
      (and (seq (:first-name np))
           (seq (:last-name np))))))
