(ns infrastructure.rest-api.auth-middleware
  (:require [infrastructure.auth.jwt :as jwt]))

(defn wrap-jwt-auth
  "Ring middleware that extracts and verifies a Bearer JWT from
  the Authorization header. Adds :identity to the request on success.
  Returns 401 if the token is missing or invalid."
  [handler jwt-secret]
  (fn [request]
    (let [auth-header (get-in request [:headers "authorization"])]
      (if-let [token (when auth-header
                       (second (re-matches #"(?i)Bearer\s+(.*)" auth-header)))]
        (try
          (let [claims (jwt/verify-token jwt-secret token)]
            (handler (assoc request :identity claims)))
          (catch Exception _
            {:status 401
             :body   {:error "Invalid or expired token"}}))
        {:status 401
         :body   {:error "Missing Authorization header"}}))))
