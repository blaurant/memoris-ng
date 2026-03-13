(ns infrastructure.rest-api.handler
  (:require [infrastructure.rest-api.admin-handler :as admin-handler]
            [infrastructure.rest-api.auth-handler :as auth-handler]
            [infrastructure.rest-api.consumption-handler :as consumption-handler]
            [infrastructure.rest-api.production-handler :as production-handler]
            [infrastructure.rest-api.logging :as logging]
            [infrastructure.rest-api.network-handler :as network-handler]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.cors :refer [wrap-cors]]))

(defn- hello-handler
  "Returns a greeting message."
  [_request]
  {:status 200
   :body   {:message "Hello you !"}})

(defn- build-router [network-repo user-repo consumption-repo production-repo ec-repo token-verifier
                     password-hasher email-sender vt-repo jwt-secret alert-banner-repo]
  (ring/router
    (concat [["/api/v1/hello" {:get hello-handler}]]
            (network-handler/routes network-repo ec-repo)
            (auth-handler/routes user-repo token-verifier password-hasher email-sender vt-repo jwt-secret)
            (consumption-handler/routes consumption-repo jwt-secret)
            (production-handler/routes production-repo jwt-secret)
            (admin-handler/routes user-repo network-repo ec-repo alert-banner-repo consumption-repo production-repo jwt-secret))
    {:data {:muuntaja   m/instance
            :middleware [muuntaja/format-middleware]}}))

(defmethod ig/init-key :http/handler
  [_ {:keys [cors-origins network-repo user-repo consumption-repo production-repo eligibility-check-repo
             token-verifier password-hasher email-sender verification-token-repo jwt-secret
             alert-banner-repo]}]
  (-> (ring/ring-handler
        (build-router network-repo user-repo consumption-repo production-repo eligibility-check-repo
                      token-verifier password-hasher email-sender verification-token-repo jwt-secret
                      alert-banner-repo)
        (ring/create-default-handler))
      (logging/wrap-request-logging)
      (wrap-cors
        :access-control-allow-origin  (map re-pattern cors-origins)
        :access-control-allow-methods [:get :post :put :delete]
        :access-control-allow-headers ["Content-Type" "Accept" "Authorization"])))

(defmethod ig/halt-key! :http/handler [_ _] nil)
