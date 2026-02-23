(ns infrastructure.rest-api.server
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]))

(defmethod ig/init-key :http/server
  [_ {:keys [handler port]}]
  (println (str "Starting HTTP server on port " port))
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :http/server
  [_ server]
  (.stop server))
