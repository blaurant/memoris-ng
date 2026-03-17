(ns infrastructure.rest-api.auth-handler
  (:require [application.auth-scenarios :as auth]
            [clojure.string :as str]
            [domain.id :as id]
            [domain.user]
            [infrastructure.auth.jwt :as jwt]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-user [user]
  (cond-> {:id       (str (:user/id user))
           :email    (:user/email user)
           :name     (:user/name user)
           :role     (name (:user/role user))
           :provider (name (:user/provider user))}
    (:user/adhesion-signed-at user)
    (assoc :adhesion-signed-at (:user/adhesion-signed-at user))))

(defn- login-handler
  "POST /api/v1/auth/login — body {:provider \"google\" :id-token \"...\"}
  or {:provider \"email\" :email \"...\" :password \"...\"}
  Returns {:token \"jwt\" :user {...}}"
  [user-repo token-verifier password-hasher email-sender jwt-secret]
  (fn [request]
    (let [{:keys [provider id-token email password]} (:body-params request)
          provider-kw (keyword provider)]
      (try
        (let [user (if (= :email provider-kw)
                     (auth/login-with-email user-repo password-hasher email password)
                     (auth/login-with-provider user-repo token-verifier email-sender provider-kw id-token))
              token (jwt/issue-token jwt-secret user)]
          {:status 200
           :body   {:token token
                    :user  (serialize-user user)}})
        (catch clojure.lang.ExceptionInfo e
          (let [msg (.getMessage e)]
            {:status (cond
                       (str/includes? msg "not verified") 403
                       :else 401)
             :body   {:error msg}}))))))

(defn- register-handler
  "POST /api/v1/auth/register — body {:id :email :name :password}"
  [user-repo password-hasher email-sender vt-repo]
  (fn [request]
    (try
      (let [{:keys [id email name password]} (:body-params request)
            user-id (id/build-id id)
            _user   (auth/register-with-email
                      user-repo password-hasher email-sender vt-repo
                      user-id email name password)]
        {:status 201
         :body   {:message "Account created. Please check your email to verify your address."}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- verify-email-handler
  "POST /api/v1/auth/verify-email — body {:token \"...\"}"
  [user-repo vt-repo email-sender]
  (fn [request]
    (try
      (let [token-string (get-in request [:body-params :token])]
        (auth/verify-email user-repo vt-repo email-sender token-string)
        {:status 200
         :body   {:message "Email verified successfully. You can now sign in."}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- resend-verification-handler
  "POST /api/v1/auth/resend-verification — body {:email \"...\"}"
  [user-repo email-sender vt-repo]
  (fn [request]
    (try
      (let [email (get-in request [:body-params :email])]
        (auth/resend-verification-email user-repo email-sender vt-repo email)
        {:status 200
         :body   {:message "Verification email sent."}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- forgot-password-handler
  "POST /api/v1/auth/forgot-password — body {:email \"...\"}"
  [user-repo email-sender vt-repo]
  (fn [request]
    (let [email (get-in request [:body-params :email])]
      (auth/request-password-reset user-repo email-sender vt-repo email)
      {:status 200
       :body   {:message "If this email exists, a reset link has been sent."}})))

(defn- reset-password-handler
  "POST /api/v1/auth/reset-password — body {:token \"...\" :password \"...\"}"
  [user-repo password-hasher vt-repo]
  (fn [request]
    (try
      (let [{:keys [token password]} (:body-params request)]
        (auth/reset-password user-repo password-hasher vt-repo token password)
        {:status 200
         :body   {:message "Password reset successfully. You can now sign in."}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- me-handler
  "GET /api/v1/auth/me — protected, returns the full user from the database."
  [user-repo]
  (fn [request]
    (let [user-id (id/build-id (get-in request [:identity :sub]))
          user    (domain.user/find-by-id user-repo user-id)]
      (if user
        {:status 200
         :body   (serialize-user user)}
        {:status 200
         :body   (:identity request)}))))

(defn routes
  "Returns Reitit route vectors for auth endpoints."
  [user-repo token-verifier password-hasher email-sender vt-repo jwt-secret]
  [["/api/v1/auth/login"
    {:post (login-handler user-repo token-verifier password-hasher email-sender jwt-secret)}]
   ["/api/v1/auth/register"
    {:post (register-handler user-repo password-hasher email-sender vt-repo)}]
   ["/api/v1/auth/verify-email"
    {:post (verify-email-handler user-repo vt-repo email-sender)}]
   ["/api/v1/auth/resend-verification"
    {:post (resend-verification-handler user-repo email-sender vt-repo)}]
   ["/api/v1/auth/forgot-password"
    {:post (forgot-password-handler user-repo email-sender vt-repo)}]
   ["/api/v1/auth/reset-password"
    {:post (reset-password-handler user-repo password-hasher vt-repo)}]
   ["/api/v1/auth/me"
    {:get        (me-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]])
