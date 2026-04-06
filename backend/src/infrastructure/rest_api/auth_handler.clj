(ns infrastructure.rest-api.auth-handler
  (:require [application.auth-scenarios :as auth]
            [application.user-scenarios :as user-scenarios]
            [clojure.string :as str]
            [domain.id :as id]
            [domain.user :as user]
            [infrastructure.auth.jwt :as jwt]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-user [user]
  (cond-> {:id       (str (:user/id user))
           :email    (:user/email user)
           :name     (:user/name user)
           :role     (name (:user/role user))
           :provider (name (:user/provider user))}
    (:user/adhesion-signed-at user)
    (assoc :adhesion-signed-at (:user/adhesion-signed-at user))
    (:user/natural-person user)
    (assoc :natural-person (:user/natural-person user))
    (:user/legal-persons user)
    (assoc :legal-persons (:user/legal-persons user))))

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
          user    (user/find-by-id user-repo user-id)]
      (if user
        {:status 200
         :body   (serialize-user user)}
        {:status 200
         :body   (:identity request)}))))

(defn- update-natural-person-handler
  "PUT /api/v1/auth/profile/natural — update natural person.
   Accepts optional target-user-id param; if present, caller must be admin."
  [user-repo]
  (fn [request]
    (try
      (let [caller-id      (id/build-id (get-in request [:identity :sub]))
            target-id-str  (get-in request [:body-params :target-user-id])
            target-id      (if target-id-str
                             (id/build-id target-id-str)
                             caller-id)
            _              (when (and target-id-str (not= target-id caller-id))
                             (let [caller (user/find-by-id user-repo caller-id)]
                               (when-not (= :admin (:user/role caller))
                                 (throw (ex-info "Only admins can edit other users" {})))))
            info           (dissoc (:body-params request) :target-user-id)
            user           (user-scenarios/update-natural-person user-repo target-id info)]
        {:status 200 :body (serialize-user user)})
      (catch clojure.lang.ExceptionInfo e
        {:status (if (.contains (.getMessage e) "Only admins") 403 400)
         :body   {:error (.getMessage e)}}))))

(defn- add-legal-person-handler
  "POST /api/v1/auth/profile/legal — add a legal person."
  [user-repo]
  (fn [request]
    (try
      (let [user-id (id/build-id (get-in request [:identity :sub]))
            info    (:body-params request)
            user    (user-scenarios/add-legal-person user-repo user-id info)]
        {:status 200 :body (serialize-user user)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400 :body {:error (.getMessage e)}}))))

(defn- update-legal-person-handler
  "PUT /api/v1/auth/profile/legal/:index — update a legal person."
  [user-repo]
  (fn [request]
    (try
      (let [user-id (id/build-id (get-in request [:identity :sub]))
            index   (parse-long (get-in request [:path-params :index]))
            info    (:body-params request)
            user    (user-scenarios/update-legal-person user-repo user-id index info)]
        {:status 200 :body (serialize-user user)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400 :body {:error (.getMessage e)}}))))

(defn- remove-legal-person-handler
  "DELETE /api/v1/auth/profile/legal/:index — remove a legal person."
  [user-repo]
  (fn [request]
    (try
      (let [user-id (id/build-id (get-in request [:identity :sub]))
            index   (parse-long (get-in request [:path-params :index]))
            user    (user-scenarios/remove-legal-person user-repo user-id index)]
        {:status 200 :body (serialize-user user)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400 :body {:error (.getMessage e)}}))))

(defn- change-password-handler
  "PUT /api/v1/auth/change-password — change password for logged-in email user."
  [user-repo password-hasher]
  (fn [request]
    (try
      (let [user-id          (id/build-id (get-in request [:identity :sub]))
            {:keys [current-password new-password]} (:body-params request)]
        (auth/change-password user-repo password-hasher user-id current-password new-password)
        {:status 200 :body {:message "Mot de passe modifié avec succès."}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400 :body {:error (.getMessage e)}}))))

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
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
   ["/api/v1/auth/profile/natural"
    {:put        (update-natural-person-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
   ["/api/v1/auth/profile/legal"
    {:post       (add-legal-person-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
   ["/api/v1/auth/profile/legal/:index"
    {:put        (update-legal-person-handler user-repo)
     :delete     (remove-legal-person-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
   ["/api/v1/auth/change-password"
    {:put        (change-password-handler user-repo password-hasher)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]])
