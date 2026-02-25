(ns infrastructure.networks.in-memory-repo
  (:require [domain.network :as network]
            [integrant.core :as ig]))

(defprotocol NetworkRepo
  (find-all [repo] "Returns all networks."))

(def ^:private demo-networks
  [(network/build-network
    {:network/id         #uuid "11111111-0000-0000-0000-000000000001"
     :network/name       "Réseau EnR Bordeaux Métropole"
     :network/center-lat 44.8378
     :network/center-lng -0.5792
     :network/radius-km  10.0})
   (network/build-network
    {:network/id         #uuid "22222222-0000-0000-0000-000000000002"
     :network/name       "Réseau Solaire Lyon Sud"
     :network/center-lat 45.7597
     :network/center-lng 4.8422
     :network/radius-km  10.0})])

(defrecord InMemoryNetworkRepo [networks]
  NetworkRepo
  (find-all [_] networks))

(defmethod ig/init-key :networks/in-memory-repo
  [_ _]
  (->InMemoryNetworkRepo demo-networks))

(defmethod ig/halt-key! :networks/in-memory-repo [_ _] nil)
