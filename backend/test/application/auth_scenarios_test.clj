(ns application.auth-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.auth-scenarios :as auth]
            [domain.token-verifier :as tv]
            [domain.user :as user]
            [domain.user-repo :as user-repo]
            [infrastructure.users.in-memory-repo :as mem-repo]))

;; ── Mock Token Verifier ──────────────────────────────────────────────────────

(defrecord MockTokenVerifier []
  tv/TokenVerifier
  (verify-provider-token [_ _provider _id-token]
    {:sub   "provider-user-123"
     :email "alice@example.com"
     :name  "Alice"}))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-repo []
  (mem-repo/->InMemoryUserRepo (atom {})))

(defn- mock-verifier []
  (->MockTokenVerifier))

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

  (THEN "audit trail has a created and a login entry" [ctx]
    (assert (= 2 (count (:user/audit-trail (:user ctx)))))
    (assert (= :created (-> ctx :user :user/audit-trail first :audit/action)))
    (assert (= :login   (-> ctx :user :user/audit-trail second :audit/action)))
    (assert (= :google  (-> ctx :user :user/audit-trail second :audit/info :provider)))))

(defscenario "Existing user signs in — trail gets a new entry"
  (GIVEN "an existing user in the repo" [ctx]
    (let [repo     (fresh-repo)
          verifier (mock-verifier)
          user1    (auth/login-with-provider repo verifier :google "token1")]
      (assoc ctx :repo repo :verifier verifier :first-user user1)))

  (WHEN "the same user signs in again" [ctx]
    (assoc ctx :user
           (auth/login-with-provider (:repo ctx) (:verifier ctx)
                                     :google "token2")))

  (THEN "audit trail has three entries" [ctx]
    (assert (= 3 (count (:user/audit-trail (:user ctx)))))))

(defscenario "Suspended user signs in — exception thrown"
  (GIVEN "a suspended user in the repo" [ctx]
    (let [repo     (fresh-repo)
          verifier (mock-verifier)
          u        (auth/login-with-provider repo verifier :google "token1")
          suspended (user/suspend u)]
      (user-repo/save! repo suspended)
      (assoc ctx :repo repo :verifier verifier)))

  (WHEN "the suspended user tries to sign in" [ctx]
    (try
      (auth/login-with-provider (:repo ctx) (:verifier ctx)
                                :google "token2")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an error is thrown saying not active" [ctx]
    (assert (= "User account is not active" (:exception ctx)))))
