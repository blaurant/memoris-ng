(ns application.auth-scenarios
  (:require [domain.id :as id]
            [domain.user :as user]
            [domain.user-repo :as user-repo]
            [domain.token-verifier :as tv]))

(defn login-with-provider
  "Authenticates a user via an OAuth provider.
  1. Verifies the id-token with the provider
  2. Finds or creates the user by provider + provider-subject-identifier
  3. Checks lifecycle is :alive (throws otherwise)
  4. Records the login in the audit trail
  5. Returns the user"
  [user-repo token-verifier provider id-token]
  (let [{:keys [sub email name]} (tv/verify-provider-token token-verifier provider id-token)
        existing (user-repo/find-by-provider user-repo provider sub)
        u        (or existing
                     (user/build-user {:user/id           (id/build-id)
                                       :user/email        email
                                       :user/name         name
                                       :user/provider     provider
                                       :user/provider-subject-identifier sub}))]
    (when-not (user/alive? u)
      (throw (ex-info "User account is not active"
                      {:lifecycle (:user/lifecycle u)})))
    (let [updated (user/record-login u provider)]
      (user-repo/save! user-repo updated)
      updated)))
