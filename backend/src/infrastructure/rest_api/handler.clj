(ns infrastructure.rest-api.handler
  (:require [infrastructure.rest-api.logging :as logging]
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

(defn- build-router [network-repo]
  (ring/router
    (into [["/api/v1/hello" {:get hello-handler}]]
          (network-handler/routes network-repo))
    {:data {:muuntaja   m/instance
            :middleware [muuntaja/format-middleware]}}))

(defmethod ig/init-key :http/handler
  [_ {:keys [cors-origins network-repo]}]
  (-> (ring/ring-handler
        (build-router network-repo)
        (ring/create-default-handler))
      (logging/wrap-request-logging)
      (wrap-cors
        :access-control-allow-origin  (map re-pattern cors-origins)
        :access-control-allow-methods [:get :post :put :delete]
        :access-control-allow-headers ["Content-Type" "Accept"])))

(defmethod ig/halt-key! :http/handler [_ _] nil)
