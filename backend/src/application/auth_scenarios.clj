(ns application.auth-scenarios
    (:require [clojure.string :as str]
              [com.brunobonacci.mulog :as mu]
              [domain.email-sender :as email-sender]
              [domain.id :as id]
              [domain.password-hasher :as password-hasher]
              [domain.token-verifier :as tv]
              [domain.user :as user]
              [domain.verification-token :as vt])
    (:import (java.time Instant)))

(def ^:private admin-emails
  #{"laurantb@gmail.com" "jurgenklein28@gmail.com"})

(defn- maybe-promote-admin
  "Promotes a user to admin if their email is in the admin set."
  [user-repo u]
  (if (and (= :customer (:user/role u))
           (contains? admin-emails (str/lower-case (:user/email u))))
    (let [promoted (assoc u :user/role :admin)]
      (user/save! user-repo promoted))
    u))

(defn login-with-provider
      "Authenticates a user via an OAuth provider.
      1. Verifies the id-token with the provider
      2. Finds the user by provider + provider-subject-id, or creates a new one
      3. Checks lifecycle is :alive (throws otherwise)
      4. Promotes to admin if eligible
      5. Returns the user"
      [user-repo token-verifier email-sender provider id-token]
      (let [{:keys [subject-id email name]} (tv/verify-provider-token token-verifier provider id-token)
            existing (user/find-by-provider user-repo provider subject-id)
            u (or existing
                  (user/save! user-repo (user/create-new-user subject-id provider email name)))
            u (user/check-alive u)
            u (maybe-promote-admin user-repo u)]
        (when-not existing
          (email-sender/send-welcome-email! email-sender email name))
        u))

;; ── Email/password authentication ─────────────────────────────────────────────

(defn register-with-email
      "Register a new user with email and password.
      1. Check no existing email user
      2. Hash password
      3. Create and save user
      4. Generate and save verification token
      5. Send verification email
      6. Return the user"
      [user-repo password-hasher email-sender vt-repo user-id email name raw-password]
      (when (user/find-by-id user-repo user-id)
        (throw (ex-info "User ID already exists" {:user-id user-id})))
      (when (user/find-by-email user-repo :email email)
        (throw (ex-info "An account with this email already exists" {:email email})))
      (user/assert-valid-password raw-password)
      (let [hashed (password-hasher/hash-password password-hasher raw-password)
            u      (user/create-email-user user-id email name hashed)
            u      (user/save! user-repo u)
            token  (vt/make-1d-token (id/build-id) user-id (str (id/build-id)))]
        (vt/save! vt-repo token)
        (email-sender/send-verification-email! email-sender email (:verification-token/token token))
        (mu/log ::user-registered :user-id user-id :email email)
        u))

(defn verify-email
      "Verify a user's email using a verification token string.
      1. Find token
      2. Check not expired
      3. Mark user email as verified
      4. Delete token
      5. Return the user"
      [user-repo vt-repo email-sender token-string]
      (let [token (vt/find-by-token vt-repo token-string)]
        (when-not token
          (throw (ex-info "Invalid verification token" {:token token-string})))
        (when (vt/expired? token (Instant/now))
          (throw (ex-info "Verification token has expired" {:token token-string})))
        (let [u (user/find-by-id user-repo (:verification-token/user-id token))]
          (when-not u
            (throw (ex-info "User not found" {:user-id (:verification-token/user-id token)})))
          (let [u' (user/verify-email u)]
            (user/save! user-repo u')
            (vt/delete! vt-repo (:verification-token/id token))
            (email-sender/send-welcome-email! email-sender (:user/email u') (:user/name u'))
            (mu/log ::email-verified :user-id (:user/id u'))
            u'))))

(defn login-with-email
      "Authenticate a user with email and password.
      1. Find user by email
      2. Check password
      3. Check alive
      4. Check email verified
      5. Promote admin if eligible
      6. Return the user"
      [user-repo password-hasher email raw-password]
      (let [u (user/find-by-email user-repo :email email)]
        (when-not u
          (throw (ex-info "Invalid credentials" {:email email})))
        (when-not (password-hasher/check-password password-hasher raw-password (:user/password-hash u))
          (throw (ex-info "Invalid credentials" {:email email})))
        (user/check-alive u)
        (user/assert-email-verified u)
        (maybe-promote-admin user-repo u)))

(defn request-password-reset
      "Request a password reset email.
      Always succeeds silently (no user enumeration).
      If the email exists and is an email-provider user, sends a reset link."
      [user-repo email-sender vt-repo email]
      (let [u (user/find-by-email user-repo :email email)]
        (when (and u (= :email (:user/provider u)))
          (let [token (vt/make-1d-token (id/build-id) (:user/id u) (str (id/build-id)))]
            (vt/save! vt-repo token)
            (email-sender/send-password-reset-email! email-sender email (:verification-token/token token))
            (mu/log ::password-reset-requested :email email)))))

(defn reset-password
      "Reset a user's password using a verification token.
      1. Find and validate token
      2. Hash new password
      3. Update user
      4. Delete token"
      [user-repo password-hasher vt-repo token-string new-password]
      (let [token (vt/find-by-token vt-repo token-string)]
        (when-not token
          (throw (ex-info "Invalid or expired reset link" {:token token-string})))
        (when (vt/expired? token (Instant/now))
          (throw (ex-info "Invalid or expired reset link" {:token token-string})))
        (let [u (user/find-by-id user-repo (:verification-token/user-id token))]
          (when-not u
            (throw (ex-info "User not found" {})))
          (user/assert-valid-password new-password)
          (let [hashed (password-hasher/hash-password password-hasher new-password)
                u'     (assoc u :user/password-hash hashed)]
            (user/save! user-repo u')
            (vt/delete! vt-repo (:verification-token/id token))
            (mu/log ::password-reset :user-id (:user/id u'))
            u'))))

(defn resend-verification-email
      "Resend the verification email to a user."
      [user-repo email-sender vt-repo email]
      (let [u (user/find-by-email user-repo :email email)]
        (when-not u
          (throw (ex-info "User not found" {:email email})))
        (when (user/email-verified? u)
          (throw (ex-info "Email already verified" {:email email})))
        (let [token (vt/make-1d-token (id/build-id) (:user/id u) (str (id/build-id)))]
          (vt/save! vt-repo token)
          (email-sender/send-verification-email! email-sender email (:verification-token/token token))
          (mu/log ::verification-email-resent :email email))))
