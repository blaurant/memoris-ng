(ns infrastructure.in-memory-repo.mem-consumption-repo
  (:require [domain.consumption :as consumption]
            [integrant.core :as ig]))

(defrecord InMemoryConsumptionRepo [store]
  consumption/ConsumptionRepo

  (find-by-id [_ id]
    (when-let [c (get @store id)]
      (consumption/build-consumption c)))

  (find-by-user-id [_ user-id]
    (vec (keep (fn [c]
                 (when (= user-id (:consumption/user-id c))
                   (consumption/build-consumption c)))
               (vals @store))))

  (save! [_ c]
    (swap! store assoc (:consumption/id c) c)
    c)

  (save! [_ original updated]
    (let [id      (:consumption/id original)
          applied (swap! store
                         (fn [m]
                           (if (= original (get m id))
                             (assoc m id updated)
                             m)))]
      (when (not= updated (get applied id))
        (throw (ex-info "Concurrent modification detected"
                        {:consumption-id id})))
      updated)))

(defmethod ig/init-key :consumptions/in-memory-repo
  [_ _]
  (->InMemoryConsumptionRepo (atom {})))

(defmethod ig/halt-key! :consumptions/in-memory-repo [_ _] nil)
