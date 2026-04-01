(ns infrastructure.xtdb.xtdb-news-repo
  (:require [domain.news :as news]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- news->doc [n]
  (assoc n :xt/id (:news/id n)))

(defn- doc->news [doc]
  (when doc
    (news/build-news (dissoc doc :xt/id))))

(defrecord XtdbNewsRepo [node]
  news/NewsRepo

  (find-all [_]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :news/id _]]})]
      (mapv (fn [[doc]] (doc->news doc)) results)))

  (find-by-id [_ id]
    (doc->news (xt/entity (xt/db node) id)))

  (save! [_ n]
    (xt/submit-tx node [[::xt/put (news->doc n)]])
    (xt/sync node)
    n)

  (save! [_ original updated]
    (let [id (:news/id original)
          tx (xt/submit-tx node [[::xt/match id (news->doc original)]
                                 [::xt/put (news->doc updated)]])]
      (xt/sync node)
      (when-not (xt/tx-committed? node tx)
        (throw (ex-info "Concurrent modification detected" {:news-id id})))
      updated))

  (delete! [_ id]
    (xt/submit-tx node [[::xt/delete id]])
    (xt/sync node)
    nil))

(defmethod ig/init-key :news/xtdb-repo [_ {:keys [node]}]
  (->XtdbNewsRepo node))
