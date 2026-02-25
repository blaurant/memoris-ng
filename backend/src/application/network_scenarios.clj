(ns application.network-scenarios
  (:require [domain.geo :as geo]
            [infrastructure.networks.in-memory-repo :as repo]))

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
