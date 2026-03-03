(ns application.auth-scenarios
    (:require [domain.id :as id]
      [domain.user :as user]
      [domain.user-repo :as user-repo]
      [domain.token-verifier :as tv]))


(defn login-with-provider
      "Authenticates a user via an OAuth provider.
      1. Get and verifies the id-token (provider-subject-id) with the provider
      2. Finds the user by provider + provider-subject-id
      3. Checks lifecycle is :alive (throws otherwise)
      4. if no user, create a new one and save it
      5. Returns the user"
      [user-repo token-verifier provider id-token]
      (let [{:keys [subject-id email name]} (tv/verify-provider-token token-verifier provider id-token)
            user (or (user-repo/find-by-provider user-repo provider subject-id)
                     (user-repo/save! user-repo (user/create-new-user subject-id provider email name)))]
           (user/check-alive user)))


