(ns infrastructure.in-memory-repo.mem-news-repo
  (:require [domain.news :as news]
            [integrant.core :as ig]))

(defrecord InMemoryNewsRepo [store]
  news/NewsRepo

  (find-all [_]
    (mapv news/build-news (vals @store)))

  (find-by-id [_ id]
    (when-let [n (get @store id)]
      (news/build-news n)))

  (save! [_ n]
    (swap! store assoc (:news/id n) n)
    n)

  (save! [_ original updated]
    (let [id      (:news/id original)
          applied (swap! store
                         (fn [m]
                           (if (= original (get m id))
                             (assoc m id updated)
                             m)))]
      (when (not= updated (get applied id))
        (throw (ex-info "Concurrent modification detected" {:news-id id})))
      updated))

  (delete! [_ id]
    (swap! store dissoc id)
    nil))

(defmethod ig/init-key :news/in-memory-repo [_ _]
  (->InMemoryNewsRepo (atom {})))
