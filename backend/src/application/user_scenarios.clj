(ns application.user-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.user :as user]))

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

(defn update-natural-person
  "Update the user's natural person profile."
  [user-repo user-id person-info]
  (let [u (user/find-by-id user-repo user-id)]
    (when-not u (throw (ex-info "User not found" {:user-id user-id})))
    (let [u' (user/update-natural-person u person-info)]
      (user/save! user-repo u')
      (mu/log ::natural-person-updated :user-id user-id)
      u')))

(defn add-legal-person
  "Add a legal person to the user."
  [user-repo user-id legal-info]
  (let [u (user/find-by-id user-repo user-id)]
    (when-not u (throw (ex-info "User not found" {:user-id user-id})))
    (let [u' (user/add-legal-person u legal-info)]
      (user/save! user-repo u')
      (mu/log ::legal-person-added :user-id user-id)
      u')))

(defn update-legal-person
  "Update a legal person at given index."
  [user-repo user-id index legal-info]
  (let [u (user/find-by-id user-repo user-id)]
    (when-not u (throw (ex-info "User not found" {:user-id user-id})))
    (let [u' (user/update-legal-person u index legal-info)]
      (user/save! user-repo u')
      (mu/log ::legal-person-updated :user-id user-id :index index)
      u')))

(defn remove-legal-person
  "Remove a legal person at given index."
  [user-repo user-id index]
  (let [u (user/find-by-id user-repo user-id)]
    (when-not u (throw (ex-info "User not found" {:user-id user-id})))
    (let [u' (user/remove-legal-person u index)]
      (user/save! user-repo u')
      (mu/log ::legal-person-removed :user-id user-id :index index)
      u')))
