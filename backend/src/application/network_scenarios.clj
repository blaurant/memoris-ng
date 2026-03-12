(ns application.network-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.datetime :as dt]
            [domain.eligibility-check :as ec]
            [domain.geo :as geo]
            [domain.id :as id]
            [domain.network :as network]
            [domain.user :as user]))

(defn- assert-admin [user-repo user-id]
  (let [caller (user/find-by-id user-repo user-id)]
    (when-not caller
      (throw (ex-info "User not found" {:user-id user-id})))
    (when (not= :admin (:user/role caller))
      (throw (ex-info "Admin access required" {:user-id user-id})))))

(defn list-networks
  "Returns all public networks from the repository."
  [network-repo]
  (filterv #(= :public (:network/lifecycle %)) (network/find-all network-repo)))

(defn check-eligibility
  "Checks whether the point (lat, lng) falls within any network.
  Persists the check with address for admin review.
  Returns {:eligible? true  :network <network>} or
          {:eligible? false :network nil}."
  [network-repo ec-repo lat lng address]
  (let [networks (filter #(= :public (:network/lifecycle %)) (network/find-all network-repo))
        match    (some #(when (geo/within-network? % lat lng) %) networks)
        result   {:eligible? (some? match)
                  :network   match}
        check    (ec/build-eligibility-check
                   {:eligibility-check/id           (id/build-id)
                    :eligibility-check/address      (or address "")
                    :eligibility-check/lat          lat
                    :eligibility-check/lng          lng
                    :eligibility-check/eligible?    (some? match)
                    :eligibility-check/network-name (when match (:network/name match))
                    :eligibility-check/checked-at   (dt/now)})]
    (ec/save! ec-repo check)
    (mu/log ::eligibility-checked :address address :eligible? (some? match))
    (assoc result :check-id (str (:eligibility-check/id check)))))

(defn subscribe-notification
  "Add a notification email to an existing eligibility check."
  [ec-repo check-id email]
  (let [check (ec/find-by-id ec-repo (id/build-id check-id))]
    (when-not check
      (throw (ex-info "Eligibility check not found" {:check-id check-id})))
    (let [updated (assoc check :eligibility-check/notification-email email)]
      (ec/save! ec-repo updated)
      (mu/log ::notification-subscribed :check-id check-id :email email)
      :ok)))

(defn list-eligibility-checks
  "Returns all eligibility checks. Requires admin role."
  [ec-repo user-repo user-id]
  (assert-admin user-repo user-id)
  (ec/find-all ec-repo))

(defn create-network
  "Create a new network with the given attributes. Requires admin role."
  [network-repo user-repo user-id id name center-lat center-lng radius-km]
  (assert-admin user-repo user-id)
  (let [n (network/build-network {:network/id         id
                                  :network/name       name
                                  :network/center-lat center-lat
                                  :network/center-lng center-lng
                                  :network/radius-km  radius-km})]
    (network/save! network-repo n)))

(defn list-all-networks
  "Returns all networks (private and public). Requires admin role."
  [network-repo user-repo user-id]
  (assert-admin user-repo user-id)
  (network/find-all network-repo))

(defn toggle-network-visibility
  "Toggle a network between :private and :public. Requires admin role."
  [network-repo user-repo user-id network-id]
  (assert-admin user-repo user-id)
  (let [n (network/find-by-id network-repo network-id)]
    (when-not n
      (throw (ex-info "Network not found" {:network-id network-id})))
    (let [n' (if (= :public (:network/lifecycle n))
               (network/unpublish n)
               (network/publish n))]
      (network/save! network-repo n')
      (mu/log ::network-visibility-toggled :network-id network-id
              :lifecycle (:network/lifecycle n'))
      n')))
