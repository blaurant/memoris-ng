(ns infrastructure.in-memory-repo.mem-eligibility-check-repo
  (:require [domain.eligibility-check :as ec]
            [domain.eligibility-check-repo :as ec-repo]
            [integrant.core :as ig]))

(defrecord InMemoryEligibilityCheckRepo [store]
  ec-repo/EligibilityCheckRepo

  (save! [_ check]
    (swap! store assoc (:eligibility-check/id check) check)
    check)

  (find-all [_]
    (mapv ec/build-eligibility-check (vals @store))))

(defmethod ig/init-key :eligibility-checks/in-memory-repo
  [_ _]
  (->InMemoryEligibilityCheckRepo (atom {})))

(defmethod ig/halt-key! :eligibility-checks/in-memory-repo [_ _] nil)
