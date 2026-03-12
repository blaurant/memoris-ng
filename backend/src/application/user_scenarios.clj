(ns application.user-scenarios
  (:require [domain.user :as user]))

(defn- assert-admin [user-repo user-id]
  (let [caller (user/find-by-id user-repo user-id)]
    (when-not caller
      (throw (ex-info "User not found" {:user-id user-id})))
    (when (not= :admin (:user/role caller))
      (throw (ex-info "Admin access required" {:user-id user-id})))))

(defn list-all-users
  "Returns all users. Requires admin role."
  [user-repo user-id]
  (assert-admin user-repo user-id)
  (user/find-all user-repo))
