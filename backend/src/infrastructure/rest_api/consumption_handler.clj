(ns infrastructure.rest-api.consumption-handler
  (:require [application.consumption-scenarios :as scenarios]
            [clojure.string :as str]
            [domain.id :as id]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-consumption
       "Convert a consumption map to a JSON-friendly format."
       [c]
       (-> c
           (update :consumption/id str)
           (update :consumption/user-id str)
           (update :consumption/lifecycle name)
           (cond-> (:consumption/network-id c)
                   (update :consumption/network-id str))))

(defn- user-id-from-request
       "Extract the user-id UUID from the JWT identity in the request."
       [request]
       (id/build-id (get-in request [:identity :sub])))

(defn- error-status
       "Map an exception message to the appropriate HTTP status code."
       [ex]
       (let [msg (.getMessage ex)]
         (cond
           (str/includes? msg "not found")            404
           (str/includes? msg "does not belong")      403
           (str/includes? msg "Concurrent")           409
           :else                                      400)))

(defn- require-param
       "Extract a required parameter from body-params, throw 400 if missing or blank."
       [request param-key label]
       (let [v (get-in request [:body-params param-key])]
         (when (or (nil? v) (and (string? v) (str/blank? v)))
           (throw (ex-info (str label " is required") {:param param-key})))
         v))

;; ── Handlers ────────────────────────────────────────────────────────────────

(defn- list-consumptions-handler [consumption-repo]
  (fn [request]
    (let [user-id       (user-id-from-request request)
          consumptions  (scenarios/list-consumptions consumption-repo user-id)]
      {:status 200
       :body   (mapv serialize-consumption consumptions)})))

(defn- create-consumption-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (require-param request :id "id"))
            c              (scenarios/create-consumption consumption-repo consumption-id user-id)]
        {:status 201
         :body   (serialize-consumption c)})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- register-consumer-information-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            address        (require-param request :address "address")
            network-id     (id/build-id (require-param request :network-id "network-id"))
            c'             (scenarios/register-consumer-information
                             consumption-repo user-id consumption-id
                             address network-id)]
        {:status 200
         :body   (serialize-consumption c')})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- associate-linky-reference-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            linky-ref      (require-param request :linky-reference "linky-reference")]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/associate-linky-reference
                     consumption-repo user-id consumption-id linky-ref))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- complete-billing-address-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            billing-addr   (require-param request :billing-address "billing-address")]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/complete-billing-address
                     consumption-repo user-id consumption-id billing-addr))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- sign-contract-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            ct-str         (require-param request :contract-type "contract-type")
            contract-type  (keyword ct-str)]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/sign-contract
                     consumption-repo user-id consumption-id contract-type))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

;; ── Routes ──────────────────────────────────────────────────────────────────

(defn routes
      "Returns Reitit route vectors for consumption endpoints."
      [consumption-repo jwt-secret]
      [["/api/v1/consumptions"
        {:get        (list-consumptions-handler consumption-repo)
         :post       (create-consumption-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/consumer-information"
        {:put        (register-consumer-information-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/linky-reference"
        {:put        (associate-linky-reference-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/billing-address"
        {:put        (complete-billing-address-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/contract-signature"
        {:put        (sign-contract-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]])
