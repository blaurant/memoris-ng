(ns infrastructure.xtdb.xtdb-consumption-repo
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
                        '{:find  [(pull e [*])]
                          :where [[e :consumption/user-id uid]]
                          :in    [uid]}
                        user-id)]
      (mapv (fn [[doc]] (doc->consumption doc)) results)))

  (save! [_ c]
    (xt/submit-tx node [[::xt/put (consumption->doc c)]])
    (xt/sync node)
    c)

  (save! [_ original updated]
    (let [id  (:consumption/id original)
          tx  (xt/submit-tx node [[::xt/match id (consumption->doc original)]
                                  [::xt/put (consumption->doc updated)]])]
      (xt/sync node)
      (when-not (xt/tx-committed? node tx)
        (throw (ex-info "Concurrent modification detected"
                        {:consumption-id id})))
      updated)))

(defmethod ig/init-key :consumptions/xtdb-repo [_ {:keys [node]}]
  (->XtdbConsumptionRepo node))

(defmethod ig/halt-key! :consumptions/xtdb-repo [_ _] nil)
