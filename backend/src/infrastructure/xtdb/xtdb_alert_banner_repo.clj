(ns infrastructure.xtdb.xtdb-alert-banner-repo
  (:require [domain.alert-banner :as alert]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- banner->doc [b]
  (assoc b :xt/id (:alert-banner/id b)))

(defn- doc->banner [doc]
  (when doc
    (alert/build-alert-banner (dissoc doc :xt/id))))

(defrecord XtdbAlertBannerRepo [node]
  alert/AlertBannerRepo

  (find-current [_]
    (let [results (xt/q (xt/db node)
                        '{:find  [(pull e [*])]
                          :where [[e :alert-banner/id _]]})]
      (when-let [[doc] (first results)]
        (doc->banner doc))))

  (save! [_ banner]
    (xt/submit-tx node [[::xt/put (banner->doc banner)]])
    (xt/sync node)
    banner))

(defmethod ig/init-key :alerts/xtdb-repo [_ {:keys [node]}]
  (->XtdbAlertBannerRepo node))

(defmethod ig/halt-key! :alerts/xtdb-repo [_ _] nil)
