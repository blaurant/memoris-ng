(ns infrastructure.users.in-memory-repo
  (:require [domain.user-repo :as user-repo]
            [integrant.core :as ig]))

(defrecord InMemoryUserRepo [store]
  user-repo/UserRepo
  (find-by-id [_ id]
    (some (fn [u] (when (= id (:user/id u)) u))
          (vals @store)))

  (find-by-provider [_ provider provider-subject-identifier]
    (some (fn [u]
            (when (and (= provider                      (:user/provider u))
                       (= provider-subject-identifier   (:user/provider-subject-identifier u)))
              u))
          (vals @store)))

  (save! [_ user]
    (swap! store assoc (:user/id user) user)
    user))

(defmethod ig/init-key :users/in-memory-repo
  [_ _]
  (->InMemoryUserRepo (atom {})))

(defmethod ig/halt-key! :users/in-memory-repo [_ _] nil)
