(ns infrastructure.rest-api.server
  (:require [com.brunobonacci.mulog :as u]
            [integrant.core :as ig]
            [ring.adapter.jetty :as jetty]))

(defmethod ig/init-key :http/server
  [_ {:keys [handler port]}]
  (u/log ::server-started :port port)
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :http/server
  [_ server]
  (.stop server))
