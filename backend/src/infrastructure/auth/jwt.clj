(ns infrastructure.auth.jwt
  (:require [integrant.core :as ig])
  (:import [com.auth0.jwt JWT]
           [com.auth0.jwt.algorithms Algorithm]
           [java.util Date]))

(defn issue-token
  "Issues a HS256 JWT for the given user. Expires in 24 hours."
  [secret user]
  (let [algo    (Algorithm/HMAC256 ^String secret)
        now     (System/currentTimeMillis)
        exp     (Date. (+ now (* 24 60 60 1000)))]
    (-> (JWT/create)
        (.withSubject  (str (:user/id user)))
        (.withClaim    "email" (:user/email user))
        (.withClaim    "name"  (:user/name user))
        (.withClaim    "role"  (name (:user/role user)))
        (.withIssuedAt (Date. now))
        (.withExpiresAt exp)
        (.sign algo))))

(defn verify-token
  "Verifies a HS256 JWT and returns the claims as a map.
  Throws on invalid or expired token."
  [secret token]
  (let [algo     (Algorithm/HMAC256 ^String secret)
        verifier (-> (JWT/require algo)
                     (.build))
        decoded  (.verify verifier token)]
    {:sub   (.getSubject decoded)
     :email (.asString (.getClaim decoded "email"))
     :name  (.asString (.getClaim decoded "name"))
     :role  (keyword (.asString (.getClaim decoded "role")))}))

(defmethod ig/init-key :auth/jwt-secret
  [_ {:keys [secret]}]
  (or secret "dev-jwt-secret-change-in-production"))

(defmethod ig/halt-key! :auth/jwt-secret [_ _] nil)
