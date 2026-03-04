(ns infrastructure.consumptions.in-memory-repo
  (:require [domain.consumption :as consumption]
            [domain.consumption-repo :as consumption-repo]
            [integrant.core :as ig]))

(defrecord InMemoryConsumptionRepo [store]
  consumption-repo/ConsumptionRepo
  (find-by-id [_ id]
    (some (fn [c] (when (= id (:consumption/id c)) (consumption/build-consumption c)))
          (vals @store)))

  (find-by-user-id [_ user-id]
    (vec (keep (fn [c]
                 (when (= user-id (:consumption/user-id c))
                   (consumption/build-consumption c)))
               (vals @store))))

  (save! [_ c]
    (swap! store assoc (:consumption/id c) c)
    c))

(defmethod ig/init-key :consumptions/in-memory-repo
  [_ _]
  (->InMemoryConsumptionRepo (atom {})))

(defmethod ig/halt-key! :consumptions/in-memory-repo [_ _] nil)
