(ns infrastructure.rest-api.logging
  (:require [com.brunobonacci.mulog :as u]
            [integrant.core :as ig]))

(defmethod ig/init-key :log/mulog
  [_ config]
  (u/log ::logging-started :publisher (:type config))
  (u/start-publisher! config))

(defmethod ig/halt-key! :log/mulog
  [_ publisher]
  (publisher))

(defn wrap-request-logging
  "Ring middleware that logs every HTTP request/response with mulog."
  [handler]
  (fn [request]
    (let [start    (System/nanoTime)
          response (handler request)
          duration (/ (- (System/nanoTime) start) 1e6)]
      (u/log ::http-request
             :method   (name (:request-method request))
             :uri      (:uri request)
             :status   (:status response)
             :duration-ms (Math/round duration))
      response)))
