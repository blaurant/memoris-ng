(ns infrastructure.rest-api.auth-handler
  (:require [application.auth-scenarios :as auth]
            [infrastructure.auth.jwt :as jwt]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- login-handler
  "POST /api/v1/auth/login — body {:provider \"google\" :id-token \"...\"}
  Returns {:token \"jwt\" :user {...}}"
  [user-repo token-verifier jwt-secret]
  (fn [request]
    (let [{:keys [provider id-token]} (:body-params request)
          provider-kw (keyword provider)]
      (try
        (let [user  (auth/login-with-provider user-repo token-verifier
                                              provider-kw id-token)
              token (jwt/issue-token jwt-secret user)]
          {:status 200
           :body   {:token token
                    :user  {:id       (str (:user/id user))
                            :email    (:user/email user)
                            :name     (:user/name user)
                            :role     (name (:user/role user))
                            :provider (name (:user/provider user))}}})
        (catch clojure.lang.ExceptionInfo e
          {:status 401
           :body   {:error (.getMessage e)}})))))

(defn- me-handler
  "GET /api/v1/auth/me — protected, returns the current user from JWT claims."
  []
  (fn [request]
    (let [identity (:identity request)]
      {:status 200
       :body   identity})))

(defn routes
  "Returns Reitit route vectors for auth endpoints."
  [user-repo token-verifier jwt-secret]
  [["/api/v1/auth/login"
    {:post (login-handler user-repo token-verifier jwt-secret)}]
   ["/api/v1/auth/me"
    {:get        (me-handler)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]])
