(ns infrastructure.rest-api.consumption-handler
  (:require [application.consumption-scenarios :as scenarios]
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
            consumption-id (id/build-id (get-in request [:body-params :id]))
            c              (scenarios/create-consumption consumption-repo consumption-id user-id)]
        {:status 201
         :body   (serialize-consumption c)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- submit-consumer-information-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            {:keys [address network-id]} (:body-params request)
            c' (scenarios/register-consumer-information
                 consumption-repo user-id consumption-id
                 address (id/build-id network-id))]
        {:status 200
         :body   (serialize-consumption c')})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- submit-linky-reference-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            linky-ref      (get-in request [:body-params :linky-reference])]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/associate-linky-reference
                     consumption-repo user-id consumption-id linky-ref))})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- submit-billing-address-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            billing-addr   (get-in request [:body-params :billing-address])]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/complete-billing-address
                     consumption-repo user-id consumption-id billing-addr))})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- sign-contract-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            contract-type  (keyword (get-in request [:body-params :contract-type]))]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/sign-contract
                     consumption-repo user-id consumption-id contract-type))})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
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
        {:put        (submit-consumer-information-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/linky-reference"
        {:put        (submit-linky-reference-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/billing-address"
        {:put        (submit-billing-address-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/contract-signature"
        {:put        (sign-contract-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]])
