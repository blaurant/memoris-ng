(ns infrastructure.in-memory-repo.mem-network-repo
  (:require [domain.network :as network]
            [integrant.core :as ig]))

(defrecord InMemoryNetworkRepo [store]
  network/NetworkRepo

  (find-all [_]
    (mapv network/build-network (vals @store)))

  (find-by-id [_ id]
    (when-let [n (get @store id)]
      (network/build-network n)))

  (save! [_ n]
    (swap! store assoc (:network/id n) n)
    n)

  (delete! [_ id]
    (swap! store dissoc id)
    nil))

(defmethod ig/init-key :networks/in-memory-repo
  [_ _]
  (->InMemoryNetworkRepo (atom {})))

(defmethod ig/halt-key! :networks/in-memory-repo [_ _] nil)
