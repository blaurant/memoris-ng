(ns application.network-scenarios
  (:require [domain.geo :as geo]
            [domain.network :as network]
            [domain.network-repo :as repo]
            [domain.user-repo :as user-repo]))

(defn- assert-admin [user-repo user-id]
  (let [caller (user-repo/find-by-id user-repo user-id)]
    (when-not caller
      (throw (ex-info "User not found" {:user-id user-id})))
    (when (not= :admin (:user/role caller))
      (throw (ex-info "Admin access required" {:user-id user-id})))))

(defn list-networks
  "Returns all networks from the repository."
  [network-repo]
  (repo/find-all network-repo))

(defn check-eligibility
  "Checks whether the point (lat, lng) falls within any network.
  Returns {:eligible? true  :network <network>} or
          {:eligible? false :network nil}."
  [network-repo lat lng]
  (let [networks (repo/find-all network-repo)
        match    (some #(when (geo/within-network? % lat lng) %) networks)]
    {:eligible? (some? match)
     :network   match}))

(defn create-network
  "Create a new network with the given attributes. Requires admin role."
  [network-repo user-repo user-id id name center-lat center-lng radius-km]
  (assert-admin user-repo user-id)
  (let [n (network/build-network {:network/id         id
                                  :network/name       name
                                  :network/center-lat center-lat
                                  :network/center-lng center-lng
                                  :network/radius-km  radius-km})]
    (repo/save! network-repo n)))
