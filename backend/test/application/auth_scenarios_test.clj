(ns application.auth-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.auth-scenarios :as auth]
            [domain.email-sender :as email-sender]
            [domain.id :as id]
            [domain.password-hasher :as password-hasher]
            [domain.token-verifier :as tv]
            [domain.user :as user]
            [domain.verification-token :as vt]
            [infrastructure.in-memory-repo.mem-user-repo :as mem-repo]))

;; ── Mock Token Verifier ──────────────────────────────────────────────────────

(defrecord MockTokenVerifier []
  tv/TokenVerifier
  (verify-provider-token [_ _provider _id-token]
    {:subject-id "provider-user-123"
     :email      "alice@example.com"
     :name       "Alice"}))

;; ── Mock Password Hasher ────────────────────────────────────────────────────

(defrecord MockPasswordHasher []
  password-hasher/PasswordHasher
  (hash-password [_ raw] (str "hashed:" raw))
  (check-password [_ raw hashed] (= (str "hashed:" raw) hashed)))

;; ── Mock Email Sender ───────────────────────────────────────────────────────

(defrecord MockEmailSender [sent]
  email-sender/EmailSender
  (send-verification-email! [_ email token]
    (swap! sent conj {:email email :token token})))

;; ── In-memory Verification Token Repo ───────────────────────────────────────

(defrecord InMemoryVtRepo [store]
  vt/VerificationTokenRepo
  (save! [_ token]
    (swap! store assoc (:verification-token/id token) token)
    token)
  (find-by-token [_ token-string]
    (some (fn [t] (when (= token-string (:verification-token/token t)) t))
          (vals @store)))
  (delete! [_ id]
    (swap! store dissoc id)
    nil))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-repo []
  (mem-repo/->InMemoryUserRepo (atom {})))

(defn- mock-verifier []
  (->MockTokenVerifier))

(defn- fresh-hasher []
  (->MockPasswordHasher))

(defn- fresh-email-sender []
  (->MockEmailSender (atom [])))

(defn- fresh-vt-repo []
  (->InMemoryVtRepo (atom {})))

;; ── Scenarios ────────────────────────────────────────────────────────────────

(defscenario "New user signs in — user is created with defaults"
  (GIVEN "a fresh repo and verifier" [ctx]
    (assoc ctx
           :repo     (fresh-repo)
           :verifier (mock-verifier)))

  (WHEN "a new user signs in with Google" [ctx]
    (assoc ctx :user
           (auth/login-with-provider (:repo ctx) (:verifier ctx)
                                     :google "fake-id-token")))

  (THEN "user is created with role customer and lifecycle alive" [ctx]
    (assert (= :customer (:user/role (:user ctx))))
    (assert (= :alive    (:user/lifecycle (:user ctx))))
    (assert (= "alice@example.com" (:user/email (:user ctx)))))

)

(defscenario "Existing user signs in — same user is returned"
  (GIVEN "an existing user in the repo" [ctx]
    (let [repo     (fresh-repo)
          verifier (mock-verifier)
          user1    (auth/login-with-provider repo verifier :google "token1")]
      (assoc ctx :repo repo :verifier verifier :first-user user1)))

  (WHEN "the same user signs in again" [ctx]
    (assoc ctx :user
           (auth/login-with-provider (:repo ctx) (:verifier ctx)
                                     :google "token2")))

  (THEN "the same user is returned" [ctx]
    (assert (= (:user/id (:first-user ctx)) (:user/id (:user ctx))))))

(defscenario "Suspended user signs in — exception thrown"
  (GIVEN "a suspended user in the repo" [ctx]
    (let [repo     (fresh-repo)
          verifier (mock-verifier)
          u        (auth/login-with-provider repo verifier :google "token1")
          suspended (user/suspend u)]
      (user/save! repo suspended)
      (assoc ctx :repo repo :verifier verifier)))

  (WHEN "the suspended user tries to sign in" [ctx]
    (try
      (auth/login-with-provider (:repo ctx) (:verifier ctx)
                                :google "token2")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an error is thrown saying not active" [ctx]
    (assert (= "User account is not alive" (:exception ctx)))))

;; ── Email/password scenarios ────────────────────────────────────────────────

(defscenario "Register with email — user created, verification email sent"
  (GIVEN "empty repos and mocks" [ctx]
    (assoc ctx
           :repo     (fresh-repo)
           :hasher   (fresh-hasher)
           :sender   (fresh-email-sender)
           :vt-repo  (fresh-vt-repo)))

  (WHEN "a new user registers with email" [ctx]
    (let [uid (id/build-id)
          u   (auth/register-with-email
                (:repo ctx) (:hasher ctx) (:sender ctx) (:vt-repo ctx)
                uid "bob@example.com" "Bob" "password123")]
      (assoc ctx :user u :user-id uid)))

  (THEN "user is created with email-verified? false and email is sent" [ctx]
    (assert (= false (:user/email-verified? (:user ctx))))
    (assert (= :email (:user/provider (:user ctx))))
    (assert (= 1 (count @(:sent (:sender ctx)))))))

(defscenario "Register with duplicate user ID — throws"
  (GIVEN "an existing user" [ctx]
    (let [repo    (fresh-repo)
          hasher  (fresh-hasher)
          sender  (fresh-email-sender)
          vt-repo (fresh-vt-repo)
          uid     (id/build-id)]
      (auth/register-with-email repo hasher sender vt-repo
                                uid "bob@example.com" "Bob" "password123")
      (assoc ctx :repo repo :hasher hasher :sender sender :vt-repo vt-repo :uid uid)))

  (WHEN "registering with the same user ID" [ctx]
    (try
      (auth/register-with-email (:repo ctx) (:hasher ctx) (:sender ctx) (:vt-repo ctx)
                                (:uid ctx) "other@example.com" "Other" "password456")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an error is thrown" [ctx]
    (assert (= "User ID already exists" (:exception ctx)))))

(defscenario "Register with duplicate email — throws"
  (GIVEN "an existing email user" [ctx]
    (let [repo    (fresh-repo)
          hasher  (fresh-hasher)
          sender  (fresh-email-sender)
          vt-repo (fresh-vt-repo)]
      (auth/register-with-email repo hasher sender vt-repo
                                (id/build-id) "bob@example.com" "Bob" "password123")
      (assoc ctx :repo repo :hasher hasher :sender sender :vt-repo vt-repo)))

  (WHEN "registering with the same email" [ctx]
    (try
      (auth/register-with-email (:repo ctx) (:hasher ctx) (:sender ctx) (:vt-repo ctx)
                                (id/build-id) "bob@example.com" "Bob2" "password456")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an error is thrown" [ctx]
    (assert (= "An account with this email already exists" (:exception ctx)))))

(defscenario "Verify email — user becomes verified"
  (GIVEN "a registered unverified user" [ctx]
    (let [repo    (fresh-repo)
          hasher  (fresh-hasher)
          sender  (fresh-email-sender)
          vt-repo (fresh-vt-repo)]
      (auth/register-with-email repo hasher sender vt-repo
                                (id/build-id) "bob@example.com" "Bob" "password123")
      (assoc ctx :repo repo :vt-repo vt-repo
             :token (:token (first @(:sent sender))))))

  (WHEN "the user verifies their email" [ctx]
    (assoc ctx :user (auth/verify-email (:repo ctx) (:vt-repo ctx) (:token ctx))))

  (THEN "user is marked as verified" [ctx]
    (assert (= true (:user/email-verified? (:user ctx))))))

(defscenario "Login with email — verified user can sign in"
  (GIVEN "a verified email user" [ctx]
    (let [repo    (fresh-repo)
          hasher  (fresh-hasher)
          sender  (fresh-email-sender)
          vt-repo (fresh-vt-repo)]
      (auth/register-with-email repo hasher sender vt-repo
                                (id/build-id) "bob@example.com" "Bob" "password123")
      (let [token (:token (first @(:sent sender)))]
        (auth/verify-email repo vt-repo token))
      (assoc ctx :repo repo :hasher hasher)))

  (WHEN "the user logs in with correct password" [ctx]
    (assoc ctx :user (auth/login-with-email (:repo ctx) (:hasher ctx)
                                            "bob@example.com" "password123")))

  (THEN "the user is returned" [ctx]
    (assert (= "bob@example.com" (:user/email (:user ctx))))))

(defscenario "Login with email — wrong password throws"
  (GIVEN "a verified email user" [ctx]
    (let [repo    (fresh-repo)
          hasher  (fresh-hasher)
          sender  (fresh-email-sender)
          vt-repo (fresh-vt-repo)]
      (auth/register-with-email repo hasher sender vt-repo
                                (id/build-id) "bob@example.com" "Bob" "password123")
      (let [token (:token (first @(:sent sender)))]
        (auth/verify-email repo vt-repo token))
      (assoc ctx :repo repo :hasher hasher)))

  (WHEN "the user logs in with wrong password" [ctx]
    (try
      (auth/login-with-email (:repo ctx) (:hasher ctx)
                             "bob@example.com" "wrongpassword")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an error is thrown" [ctx]
    (assert (= "Invalid credentials" (:exception ctx)))))

(defscenario "Login with email — unverified user throws"
  (GIVEN "an unverified email user" [ctx]
    (let [repo    (fresh-repo)
          hasher  (fresh-hasher)
          sender  (fresh-email-sender)
          vt-repo (fresh-vt-repo)]
      (auth/register-with-email repo hasher sender vt-repo
                                (id/build-id) "bob@example.com" "Bob" "password123")
      (assoc ctx :repo repo :hasher hasher)))

  (WHEN "the user tries to log in" [ctx]
    (try
      (auth/login-with-email (:repo ctx) (:hasher ctx)
                             "bob@example.com" "password123")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an error about email verification is thrown" [ctx]
    (assert (= "Email not verified" (:exception ctx)))))
