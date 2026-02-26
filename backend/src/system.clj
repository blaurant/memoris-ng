(ns system
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [integrant.core :as ig]
            [infrastructure.auth.jwt]
            [infrastructure.auth.token-verifier]
            [infrastructure.networks.in-memory-repo]
            [infrastructure.rest-api.handler]
            [infrastructure.rest-api.logging]
            [infrastructure.rest-api.server]
            [infrastructure.users.in-memory-repo]))

;; Teach aero how to read #ig/ref tags from system.edn
(defmethod aero/reader 'ig/ref [_ _ value] (ig/ref value))

(defn read-config
  "Load system.edn from classpath for the given profile keyword."
  [profile]
  (aero/read-config (io/resource "system.edn")
                    {:profile profile}))

(defn- deployed-env?
  "Returns true when the HOSTNAME env var contains the given string (uppercased)."
  [env]
  (some-> (System/getenv "HOSTNAME")
          (str/upper-case)
          (str/includes? env)))

(defn active-profile
  "Resolve active profile from APP_ENV, defaulting to :dev locally.
  Throws if APP_ENV is absent in a detected deployed environment."
  []
  (if-let [env (System/getenv "APP_ENV")]
    (keyword env)
    (if (deployed-env? "PROD")
      (throw (ex-info "APP_ENV must be set in deployed environments" {}))
      :dev)))

(defn start
  "Start all Integrant components from system.edn."
  []
  (let [config (read-config (active-profile))]
    (ig/load-namespaces config)
    (ig/init config)))

(defn stop
  "Halt all running Integrant components."
  [system]
  (ig/halt! system))
