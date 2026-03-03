(ns infrastructure.xtdb.node
  (:require [integrant.core :as ig]
            [xtdb.api :as xt]))

(defmethod ig/init-key :xtdb/node [_ {:keys [dir]}]
  (xt/start-node
    {:xtdb/tx-log         {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                                      :db-dir (str dir "/tx-log")}}
     :xtdb/document-store {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                                      :db-dir (str dir "/doc-store")}}
     :xtdb/index-store    {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                                      :db-dir (str dir "/index-store")}}}))

(defmethod ig/halt-key! :xtdb/node [_ node]
  (.close node))
