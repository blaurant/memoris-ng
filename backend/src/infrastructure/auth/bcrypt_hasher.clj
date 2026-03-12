(ns infrastructure.auth.bcrypt-hasher
  (:require [buddy.hashers :as hashers]
            [domain.password-hasher :as password-hasher]
            [integrant.core :as ig]))

(defrecord BcryptHasher []
  password-hasher/PasswordHasher

  (hash-password [_ raw-password]
    (hashers/derive raw-password {:alg :bcrypt+sha512}))

  (check-password [_ raw-password hashed]
    (hashers/check raw-password hashed)))

(defmethod ig/init-key :auth/password-hasher [_ _]
  (->BcryptHasher))

(defmethod ig/halt-key! :auth/password-hasher [_ _] nil)
