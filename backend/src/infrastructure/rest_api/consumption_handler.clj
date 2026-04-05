(ns infrastructure.rest-api.consumption-handler
  (:require [application.consumption-scenarios :as scenarios]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [com.brunobonacci.mulog :as mu]
            [domain.id :as id]
            [domain.user :as user]
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

(defn- register-consumer-information-handler [consumption-repo network-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            address        (require-param request :address "address")
            network-id     (get-in request [:body-params :network-id])
            network-opts   (cond-> {}
                             network-id
                             (assoc :network-id network-id)
                             (not network-id)
                             (merge {:network-name   (require-param request :network-name "network-name")
                                     :network-lat    (double (require-param request :network-lat "network-lat"))
                                     :network-lng    (double (require-param request :network-lng "network-lng"))
                                     :network-radius (some-> (get-in request [:body-params :network-radius]) double)}))
            c'             (scenarios/register-consumer-information
                             consumption-repo network-repo user-id consumption-id
                             address network-opts)]
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
            billing-addr   (require-param request :billing-address "billing-address")
            iban-holder    (require-param request :iban-holder "iban-holder")
            iban           (require-param request :iban "iban")
            bic            (get-in request [:body-params :bic])]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/complete-billing-address
                     consumption-repo user-id consumption-id billing-addr iban-holder iban bic))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- go-back-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/go-back-consumption
                     consumption-repo user-id consumption-id))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- abandon-consumption-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/abandon-consumption
                     consumption-repo user-id consumption-id))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- sign-adhesion-handler [user-repo document-signer]
  (fn [request]
    (try
      (let [user-id (user-id-from-request request)
            result  (scenarios/sign-adhesion user-repo document-signer user-id)]
        {:status 200
         :body   result})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- docuseal-webhook-handler [user-repo]
  (fn [request]
    (try
      (let [body          (json/read-str (slurp (:body request)) :key-fn keyword)
            event-type    (:event_type body)
            data          (:data body)
            submission-id (when data
                            (or (:submission_id data) (:id data)))]
        (mu/log ::docuseal-webhook :event-type event-type :submission-id submission-id)
        (when (and (= "submission.completed" event-type) submission-id)
          (scenarios/complete-adhesion-webhook user-repo submission-id))
        {:status 200 :body {:ok true}})
      (catch clojure.lang.ExceptionInfo e
        (mu/log ::docuseal-webhook-error :error (.getMessage e))
        {:status 200 :body {:ok true}}))))

(defn- check-adhesion-handler [user-repo document-signer]
  (fn [request]
    (try
      (let [user-id (user-id-from-request request)
            result  (scenarios/check-adhesion-status
                      user-repo document-signer user-id)]
        {:status 200
         :body   result})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- adhesion-document-handler [user-repo document-signer]
  (fn [request]
    (try
      (let [user-id (user-id-from-request request)
            result  (scenarios/get-adhesion-document-url
                      user-repo document-signer user-id)]
        {:status 200
         :body   result})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- initiate-contract-handler [consumption-repo user-repo document-signer]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            ct-str         (require-param request :contract-type "contract-type")
            contract-type  (keyword ct-str)
            result         (scenarios/initiate-contract-signing
                             consumption-repo user-repo document-signer
                             user-id consumption-id contract-type)]
        {:status 200
         :body   result})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- check-contract-handler [consumption-repo user-repo document-signer]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            ct-str         (get-in request [:query-params "contract-type"])
            contract-type  (keyword ct-str)
            result         (scenarios/check-contract-status
                             consumption-repo user-repo document-signer
                             user-id consumption-id contract-type)]
        {:status 200
         :body   result})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- contract-document-handler [consumption-repo document-signer]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            ct-str         (get-in request [:query-params "contract-type"])
            contract-type  (keyword ct-str)
            result         (scenarios/get-contract-document-url
                             consumption-repo document-signer
                             user-id consumption-id contract-type)]
        {:status 200
         :body   result})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- update-consumer-address-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            new-addr       (get-in request [:body-params :consumer-address])]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/update-consumer-address consumption-repo user-id consumption-id new-addr))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- update-billing-address-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            new-addr       (get-in request [:body-params :billing-address])]
        {:status 200
         :body   (serialize-consumption
                   (scenarios/update-billing-address consumption-repo user-id consumption-id new-addr))})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- delete-consumption-handler [consumption-repo]
  (fn [request]
    (try
      (let [user-id        (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))]
        (scenarios/delete-consumption consumption-repo user-id consumption-id)
        {:status 200
         :body   {:ok true}})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

(defn- dashboard-handler [consumption-repo production-repo network-repo]
  (fn [request]
    (try
      (let [user-id       (user-id-from-request request)
            consumption-id (id/build-id (get-in request [:path-params :id]))
            dashboard     (scenarios/get-consumption-dashboard
                            consumption-repo production-repo network-repo
                            user-id consumption-id)]
        {:status 200
         :body   (update dashboard :consumption serialize-consumption)})
      (catch clojure.lang.ExceptionInfo e
        {:status (error-status e)
         :body   {:error (.getMessage e)}}))))

;; ── Routes ──────────────────────────────────────────────────────────────────

(defn routes
      "Returns Reitit route vectors for consumption endpoints."
      [consumption-repo production-repo network-repo user-repo document-signer jwt-secret]
      [["/api/v1/consumptions"
        {:get        (list-consumptions-handler consumption-repo)
         :post       (create-consumption-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id"
        {:delete     (delete-consumption-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/update-address"
        {:put        (update-consumer-address-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/update-billing-address"
        {:put        (update-billing-address-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/dashboard"
        {:get        (dashboard-handler consumption-repo production-repo network-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/go-back"
        {:put        (go-back-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/abandon"
        {:put        (abandon-consumption-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/consumer-information"
        {:put        (register-consumer-information-handler consumption-repo network-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/linky-reference"
        {:put        (associate-linky-reference-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/billing-address"
        {:put        (complete-billing-address-handler consumption-repo)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/step/contract-signature"
        {:put        (initiate-contract-handler consumption-repo user-repo document-signer)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/check-contract"
        {:get        (check-contract-handler consumption-repo user-repo document-signer)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/consumptions/:id/contract-document"
        {:get        (contract-document-handler consumption-repo document-signer)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/auth/sign-adhesion"
        {:put        (sign-adhesion-handler user-repo document-signer)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/auth/check-adhesion"
        {:get        (check-adhesion-handler user-repo document-signer)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/auth/adhesion-document"
        {:get        (adhesion-document-handler user-repo document-signer)
         :middleware [[auth-mw/wrap-jwt-auth jwt-secret]]}]
       ["/api/v1/webhooks/docuseal"
        {:post       (docuseal-webhook-handler user-repo)}]])
