(ns infrastructure.consumptions.xtdb-repo
  (:require [domain.consumption :as consumption]
            [domain.consumption-repo :as consumption-repo]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- consumption->doc [c]
  (assoc c :xt/id (:consumption/id c)))

(defn- doc->consumption [doc]
  (when doc
    (consumption/build-consumption (dissoc doc :xt/id))))

(defrecord XtdbConsumptionRepo [node]
  consumption-repo/ConsumptionRepo

  (find-by-id [_ id]
    (doc->consumption (xt/entity (xt/db node) id)))

  (find-by-user-id [_ user-id]
    (let [results (xt/q (xt/db node)
                        '{:find  [e]
                          :where [[e :consumption/user-id uid]]
                          :in    [uid]}
                        user-id)]
      (mapv (fn [[eid]]
              (doc->consumption (xt/entity (xt/db node) eid)))
            results)))

  (save! [_ c]
    (xt/submit-tx node [[::xt/put (consumption->doc c)]])
    (xt/sync node)
    c))

(defmethod ig/init-key :consumptions/xtdb-repo [_ {:keys [node]}]
  (->XtdbConsumptionRepo node))

(defmethod ig/halt-key! :consumptions/xtdb-repo [_ _] nil)
