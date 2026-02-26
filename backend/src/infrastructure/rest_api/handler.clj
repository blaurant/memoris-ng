(ns infrastructure.rest-api.handler
  (:require [infrastructure.rest-api.auth-handler :as auth-handler]
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

(defn- build-router [network-repo user-repo token-verifier jwt-secret]
  (ring/router
    (concat [["/api/v1/hello" {:get hello-handler}]]
            (network-handler/routes network-repo)
            (auth-handler/routes user-repo token-verifier jwt-secret))
    {:data {:muuntaja   m/instance
            :middleware [muuntaja/format-middleware]}}))

(defmethod ig/init-key :http/handler
  [_ {:keys [cors-origins network-repo user-repo token-verifier jwt-secret]}]
  (-> (ring/ring-handler
        (build-router network-repo user-repo token-verifier jwt-secret)
        (ring/create-default-handler))
      (logging/wrap-request-logging)
      (wrap-cors
        :access-control-allow-origin  (map re-pattern cors-origins)
        :access-control-allow-methods [:get :post :put :delete]
        :access-control-allow-headers ["Content-Type" "Accept" "Authorization"])))

(defmethod ig/halt-key! :http/handler [_ _] nil)
