(ns application.user-scenarios
  (:require [domain.user-repo :as user-repo]))

(defn- assert-admin [user-repo user-id]
  (let [caller (user-repo/find-by-id user-repo user-id)]
    (when-not caller
      (throw (ex-info "User not found" {:user-id user-id})))
    (when (not= :admin (:user/role caller))
      (throw (ex-info "Admin access required" {:user-id user-id})))))

(defn list-all-users
  "Returns all users. Requires admin role."
  [user-repo user-id]
  (assert-admin user-repo user-id)
  (user-repo/find-all user-repo))
