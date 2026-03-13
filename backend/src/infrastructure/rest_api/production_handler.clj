(ns infrastructure.rest-api.production-handler
  (:require [application.production-scenarios :as scenarios]
            [clojure.string :as str]
            [domain.id :as id]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-production
       "Convert a production map to a JSON-friendly format."
       [p]
       (-> p
           (update :production/id str)
           (update :production/user-id str)
           (update :production/lifecycle name)
           (cond-> (:production/energy-type p)
                   (update :production/energy-type name))))

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

(defn- list-productions-handler [production-repo]
  (fn [request]
    (let [user-id      (user-id-from-request request)
          productions  (scenarios/list-productions production-repo user-id)]
      {:status 200
       :body   (mapv serialize-production productions)})))

(defn- create-production-handler [production-repo]
  (fn [request]
    (try
      (let [user-id       (user-id-from-request request)
            production-id (id/build-id (require-param request :id "id"))
            p             (scenarios/create-production production-repo production-id user-id)]
        {:status 201
         :body   (serialize-production p)})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- register-installation-info-handler [production-repo]
  (fn [request]
    (try
      (let [user-id       (user-id-from-request request)
            production-id (id/build-id (get-in request [:path-params :id]))
            pdl-prm       (require-param request :pdl-prm "pdl-prm")
            power         (double (require-param request :installed-power "installed-power"))
            energy-type   (keyword (require-param request :energy-type "energy-type"))
            linky-meter   (require-param request :linky-meter "linky-meter")
            p'            (scenarios/register-installation-info
                            production-repo user-id production-id
                            pdl-prm power energy-type linky-meter)]
        {:status 200
         :body   (serialize-production p')})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- submit-payment-info-handler [production-repo]
  (fn [request]
    (try
      (let [user-id       (user-id-from-request request)
            production-id (id/build-id (get-in request [:path-params :id]))
            iban          (require-param request :iban "iban")]
        {:status 200
         :body   (serialize-production
                   (scenarios/submit-payment-info
                     production-repo user-id production-id iban))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- sign-contract-handler [production-repo]
  (fn [request]
    (try
      (let [user-id       (user-id-from-request request)
            production-id (id/build-id (get-in request [:path-params :id]))]
        {:status 200
         :body   (serialize-production
                   (scenarios/sign-contract
                     production-repo user-id production-id))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

;; ── Routes ──────────────────────────────────────────────────────────────────

(defn routes
      "Returns Reitit route vectors for production endpoints."
      [production-repo jwt-secret]
      [["/api/v1/productions"
        {:get        (list-productions-handler production-repo)
         :post       (create-production-handler production-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/productions/:id/step/installation-info"
        {:put        (register-installation-info-handler production-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/productions/:id/step/payment-info"
        {:put        (submit-payment-info-handler production-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/productions/:id/step/contract-signature"
        {:put        (sign-contract-handler production-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]])
