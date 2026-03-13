(ns infrastructure.in-memory-repo.mem-production-repo
  (:require [domain.production :as production]
            [integrant.core :as ig]))

(defrecord InMemoryProductionRepo [store]
  production/ProductionRepo

  (find-by-id [_ id]
    (when-let [p (get @store id)]
      (production/build-production p)))

  (find-by-user-id [_ user-id]
    (vec (keep (fn [p]
                 (when (= user-id (:production/user-id p))
                   (production/build-production p)))
               (vals @store))))

  (find-all [_]
    (mapv production/build-production (vals @store)))

  (save! [_ p]
    (swap! store assoc (:production/id p) p)
    p)

  (save! [_ original updated]
    (let [id      (:production/id original)
          applied (swap! store
                         (fn [m]
                           (if (= original (get m id))
                             (assoc m id updated)
                             m)))]
      (when (not= updated (get applied id))
        (throw (ex-info "Concurrent modification detected"
                        {:production-id id})))
      updated)))

(defmethod ig/init-key :productions/in-memory-repo
  [_ _]
  (->InMemoryProductionRepo (atom {})))

(defmethod ig/halt-key! :productions/in-memory-repo [_ _] nil)
