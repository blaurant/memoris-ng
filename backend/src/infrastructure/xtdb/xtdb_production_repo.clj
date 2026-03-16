(ns infrastructure.xtdb.xtdb-production-repo
  (:require [domain.production :as production]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- production->doc [p]
  (assoc p :xt/id (:production/id p)))

(defn- doc->production [doc]
  (when doc
    (production/build-production (dissoc doc :xt/id))))

(defrecord XtdbProductionRepo [node]
  production/ProductionRepo

  (find-by-id [_ id]
    (doc->production (xt/entity (xt/db node) id)))

  (find-by-user-id [_ user-id]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :production/user-id uid]]
                          :in    [uid]}
                        user-id)]
      (mapv (fn [[doc]] (doc->production doc)) results)))

  (find-by-network-id [_ network-id]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :production/network-id nid]]
                          :in    [nid]}
                        network-id)]
      (mapv (fn [[doc]] (doc->production doc)) results)))

  (find-all [_]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :production/id _]]})]
      (mapv (fn [[doc]] (doc->production doc)) results)))

  (save! [_ p]
    (xt/submit-tx node [[::xt/put (production->doc p)]])
    (xt/sync node)
    p)

  (save! [_ original updated]
    (let [id  (:production/id original)
          tx  (xt/submit-tx node [[::xt/match id (production->doc original)]
                                  [::xt/put (production->doc updated)]])]
      (xt/sync node)
      (when-not (xt/tx-committed? node tx)
        (throw (ex-info "Concurrent modification detected"
                        {:production-id id})))
      updated)))

(defmethod ig/init-key :productions/xtdb-repo [_ {:keys [node]}]
  (->XtdbProductionRepo node))

(defmethod ig/halt-key! :productions/xtdb-repo [_ _] nil)
