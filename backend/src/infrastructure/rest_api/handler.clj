(ns infrastructure.rest-api.handler
  (:require [infrastructure.rest-api.admin-handler :as admin-handler]
            [infrastructure.rest-api.auth-handler :as auth-handler]
            [infrastructure.rest-api.contact-handler :as contact-handler]
            [infrastructure.rest-api.consumption-handler :as consumption-handler]
            [infrastructure.rest-api.production-handler :as production-handler]
            [infrastructure.rest-api.logging :as logging]
            [infrastructure.rest-api.network-handler :as network-handler]
            [infrastructure.rest-api.news-handler :as news-handler]
            [integrant.core :as ig]
            [muuntaja.core :as m]
            [reitit.ring :as ring]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]))

(defn- hello-handler
  "Returns a greeting message."
  [_request]
  {:status 200
   :body   {:message "Hello you !"}})

(defn- build-router [network-repo user-repo consumption-repo production-repo ec-repo token-verifier
                     password-hasher email-sender vt-repo jwt-secret alert-banner-repo document-signer
                     news-repo]
  (ring/router
    (concat [["/api/v1/hello" {:get hello-handler}]]
            (network-handler/routes network-repo ec-repo production-repo consumption-repo)
            (auth-handler/routes user-repo token-verifier password-hasher email-sender vt-repo jwt-secret)
            (consumption-handler/routes consumption-repo production-repo network-repo user-repo document-signer alert-banner-repo email-sender jwt-secret)
            (production-handler/routes production-repo network-repo consumption-repo user-repo email-sender jwt-secret)
            (admin-handler/routes user-repo network-repo ec-repo alert-banner-repo consumption-repo production-repo email-sender jwt-secret document-signer)
            (news-handler/routes news-repo jwt-secret)
            (contact-handler/routes user-repo email-sender))
    {:data {:muuntaja   m/instance
            :middleware [wrap-params
                        muuntaja/format-middleware]}}))

(defmethod ig/init-key :http/handler
  [_ {:keys [cors-origins network-repo user-repo consumption-repo production-repo eligibility-check-repo
             token-verifier password-hasher email-sender verification-token-repo jwt-secret
             alert-banner-repo document-signer news-repo]}]
  (-> (ring/ring-handler
        (build-router network-repo user-repo consumption-repo production-repo eligibility-check-repo
                      token-verifier password-hasher email-sender verification-token-repo jwt-secret
                      alert-banner-repo document-signer news-repo)
        (ring/create-default-handler))
      (logging/wrap-request-logging)
      (wrap-cors
        :access-control-allow-origin  (map re-pattern cors-origins)
        :access-control-allow-methods [:get :post :put :delete]
        :access-control-allow-headers ["Content-Type" "Accept" "Authorization"])))

(defmethod ig/halt-key! :http/handler [_ _] nil)
