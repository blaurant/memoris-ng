(ns infrastructure.in-memory-repo.mem-user-repo
  (:require [domain.user :as user]
            [domain.user-repo :as user-repo]
            [integrant.core :as ig]))

(defrecord InMemoryUserRepo [store]
  user-repo/UserRepo
  (find-by-id [_ id]
    (some (fn [u] (when (= id (:user/id u)) (user/build-user u)))
          (vals @store)))

  (find-by-email [_ provider email]
    (some (fn [u]
            (when (and (= provider (:user/provider u))
                       (= email    (:user/email u)))
              (user/build-user u)))
          (vals @store)))

  (find-by-provider [_ provider provider-subject-identifier]
    (some (fn [u]
            (when (and (= provider                      (:user/provider u))
                       (= provider-subject-identifier   (:user/provider-subject-identifier u)))
              (user/build-user u)))
          (vals @store)))

  (find-all [_]
    (mapv user/build-user (vals @store)))

  (save! [_ user]
    (swap! store assoc (:user/id user) user)
    user))

(defmethod ig/init-key :users/in-memory-repo
  [_ _]
  (->InMemoryUserRepo (atom {})))

(defmethod ig/halt-key! :users/in-memory-repo [_ _] nil)
