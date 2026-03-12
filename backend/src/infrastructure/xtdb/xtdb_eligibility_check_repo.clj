(ns infrastructure.xtdb.xtdb-eligibility-check-repo
  (:require [domain.datetime :as dt]
            [domain.eligibility-check :as ec]
            [domain.eligibility-check-repo :as ec-repo]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- check->doc [c]
  (-> c
      (assoc :xt/id (:eligibility-check/id c))
      (update :eligibility-check/checked-at str)))

(defn- doc->check [doc]
  (when doc
    (-> (dissoc doc :xt/id)
        (update :eligibility-check/checked-at dt/build-datetime)
        (ec/build-eligibility-check))))

(defrecord XtdbEligibilityCheckRepo [node]
  ec-repo/EligibilityCheckRepo

  (save! [_ check]
    (xt/submit-tx node [[::xt/put (check->doc check)]])
    (xt/sync node)
    check)

  (find-all [_]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :eligibility-check/id _]]})]
      (mapv (fn [[doc]] (doc->check doc)) results))))

(defmethod ig/init-key :eligibility-checks/xtdb-repo [_ {:keys [node]}]
  (->XtdbEligibilityCheckRepo node))

(defmethod ig/halt-key! :eligibility-checks/xtdb-repo [_ _] nil)
