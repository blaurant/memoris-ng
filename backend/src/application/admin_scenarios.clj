(ns application.admin-scenarios
  (:require [domain.network :as network]
            [domain.network-repo :as network-repo]
            [domain.user-repo :as user-repo]))

(defn list-users
  "Returns all users."
  [user-repo]
  (user-repo/find-all user-repo))

(defn create-network
  "Create a new network with the given attributes."
  [network-repo id name center-lat center-lng radius-km]
  (let [n (network/build-network {:network/id         id
                                  :network/name       name
                                  :network/center-lat center-lat
                                  :network/center-lng center-lng
                                  :network/radius-km  radius-km})]
    (network-repo/save! network-repo n)))
