(ns infrastructure.in-memory-repo.mem-eligibility-check-repo
  (:require [domain.eligibility-check :as ec]
            [integrant.core :as ig]))

(defrecord InMemoryEligibilityCheckRepo [store]
  ec/EligibilityCheckRepo

  (save! [_ check]
    (swap! store assoc (:eligibility-check/id check) check)
    check)

  (find-by-id [_ id]
    (get @store id))

  (find-all [_]
    (mapv ec/build-eligibility-check (vals @store))))

(defmethod ig/init-key :eligibility-checks/in-memory-repo
  [_ _]
  (->InMemoryEligibilityCheckRepo (atom {})))

(defmethod ig/halt-key! :eligibility-checks/in-memory-repo [_ _] nil)
