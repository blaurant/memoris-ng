(ns infrastructure.xtdb.xtdb-verification-token-repo
  (:require [domain.verification-token :as vt]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- token->doc [t]
  (assoc t :xt/id (:verification-token/id t)))

(defn- doc->token [doc]
  (when doc
    (vt/build-verification-token (dissoc doc :xt/id))))

(defrecord XtdbVerificationTokenRepo [node]
  vt/VerificationTokenRepo

  (save! [_ token]
    (xt/submit-tx node [[::xt/put (token->doc token)]])
    (xt/sync node)
    token)

  (find-by-token [_ token-string]
    (let [results (xt/q (xt/db node)
                        '{:find  [e]
                          :where [[e :verification-token/token t]]
                          :in    [t]}
                        token-string)]
      (when-let [eid (ffirst results)]
        (doc->token (xt/entity (xt/db node) eid)))))

  (delete! [_ id]
    (xt/submit-tx node [[::xt/delete id]])
    (xt/sync node)
    nil))

(defmethod ig/init-key :verification-tokens/xtdb-repo [_ {:keys [node]}]
  (->XtdbVerificationTokenRepo node))

(defmethod ig/halt-key! :verification-tokens/xtdb-repo [_ _] nil)
