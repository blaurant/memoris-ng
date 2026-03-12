(ns infrastructure.xtdb.xtdb-network-repo
  (:require [domain.network :as network]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- network->doc [n]
  (assoc n :xt/id (:network/id n)))

(defn- doc->network [doc]
  (when doc
    (network/build-network (dissoc doc :xt/id))))

(defrecord XtdbNetworkRepo [node]
  network/NetworkRepo

  (find-all [_]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :network/id _]]})]
      (mapv (fn [[doc]] (doc->network doc)) results)))

  (find-by-id [_ id]
    (doc->network (xt/entity (xt/db node) id)))

  (save! [_ n]
    (xt/submit-tx node [[::xt/put (network->doc n)]])
    (xt/sync node)
    n))

(defmethod ig/init-key :networks/xtdb-repo [_ {:keys [node]}]
  (->XtdbNetworkRepo node))

(defmethod ig/halt-key! :networks/xtdb-repo [_ _] nil)
