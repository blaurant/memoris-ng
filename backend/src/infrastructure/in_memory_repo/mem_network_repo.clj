(ns infrastructure.in-memory-repo.mem-network-repo
  (:require [domain.network :as network]
            [domain.network-repo :as network-repo]
            [integrant.core :as ig]))

(defrecord InMemoryNetworkRepo [store]
  network-repo/NetworkRepo

  (find-all [_]
    (mapv network/build-network (vals @store)))

  (find-by-id [_ id]
    (when-let [n (get @store id)]
      (network/build-network n)))

  (save! [_ n]
    (swap! store assoc (:network/id n) n)
    n))

(defmethod ig/init-key :networks/in-memory-repo
  [_ _]
  (->InMemoryNetworkRepo (atom {})))

(defmethod ig/halt-key! :networks/in-memory-repo [_ _] nil)
