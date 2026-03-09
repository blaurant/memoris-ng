(ns application.auth-scenarios
    (:require [clojure.string :as str]
              [domain.user :as user]
              [domain.user-repo :as user-repo]
              [domain.token-verifier :as tv]))

(def ^:private admin-emails
  #{"laurantb@gmail.com"})

(defn- maybe-promote-admin
  "Promotes a user to admin if their email is in the admin set."
  [user-repo u]
  (if (and (= :customer (:user/role u))
           (contains? admin-emails (str/lower-case (:user/email u))))
    (let [promoted (assoc u :user/role :admin)]
      (user-repo/save! user-repo promoted))
    u))

(defn login-with-provider
      "Authenticates a user via an OAuth provider.
      1. Verifies the id-token with the provider
      2. Finds the user by provider + provider-subject-id, or creates a new one
      3. Checks lifecycle is :alive (throws otherwise)
      4. Promotes to admin if eligible
      5. Returns the user"
      [user-repo token-verifier provider id-token]
      (let [{:keys [subject-id email name]} (tv/verify-provider-token token-verifier provider id-token)
            u (or (user-repo/find-by-provider user-repo provider subject-id)
                  (user-repo/save! user-repo (user/create-new-user subject-id provider email name)))
            u (user/check-alive u)
            u (maybe-promote-admin user-repo u)]
        u))


