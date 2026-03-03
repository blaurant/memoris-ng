(ns infrastructure.users.xtdb-repo
  (:require [domain.user :as user]
            [domain.user-repo :as user-repo]
            [integrant.core :as ig]
            [xtdb.api :as xt]))

(defn- user->doc [u]
  (assoc u :xt/id (:user/id u)))

(defn- doc->user [doc]
  (when doc
    (user/build-user (dissoc doc :xt/id))))

(defrecord XtdbUserRepo [node]
  user-repo/UserRepo

  (find-by-id [_ id]
    (doc->user (xt/entity (xt/db node) id)))

  (find-by-provider [_ provider provider-subject-identifier]
    (let [results (xt/q (xt/db node)
                        '{:find  [e]
                          :where [[e :user/provider p]
                                  [e :user/provider-subject-identifier psi]]
                          :in    [p psi]}
                        provider provider-subject-identifier)]
      (when-let [eid (ffirst results)]
        (doc->user (xt/entity (xt/db node) eid)))))

  (save! [_ u]
    (xt/submit-tx node [[::xt/put (user->doc u)]])
    (xt/sync node)
    u))

(defmethod ig/init-key :users/xtdb-repo [_ {:keys [node]}]
  (->XtdbUserRepo node))

(defmethod ig/halt-key! :users/xtdb-repo [_ _] nil)
