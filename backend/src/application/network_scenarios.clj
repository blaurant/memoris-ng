(ns application.network-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.consumption :as consumption]
            [domain.datetime :as dt]
            [domain.eligibility-check :as ec]
            [domain.email-sender :as email-sender]
            [domain.id :as id]
            [domain.network :as network]
            [domain.production :as production]
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
  "Checks whether the point (lat, lng) falls within any public network.
  Persists the check with address for admin review.
  Returns {:eligible? true  :network <network>} or
          {:eligible? false :network nil}."
  [networks ec-repo lat lng address]
  (let [match    (network/find-covering-network networks lat lng)
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
  [network-repo user-repo user-id id name center-lat center-lng radius-km & [{:keys [description price-per-kwh]}]]
  (assert-admin user-repo user-id)
  (let [n (network/build-network (cond-> {:network/id         id
                                          :network/name       name
                                          :network/center-lat center-lat
                                          :network/center-lng center-lng
                                          :network/radius-km  radius-km}
                                   description   (assoc :network/description description)
                                   price-per-kwh (assoc :network/price-per-kwh (double price-per-kwh))))]
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

(defn validate-network
  "Validate a pending-validation network (transitions to :private). Requires admin role."
  [network-repo user-repo user-id network-id]
  (assert-admin user-repo user-id)
  (let [n (network/find-by-id network-repo network-id)]
    (when-not n
      (throw (ex-info "Network not found" {:network-id network-id})))
    (let [n' (network/validate-network n)]
      (network/save! network-repo n')
      (mu/log ::network-validated :network-id network-id)
      n')))

(defn delete-network
  "Delete a network. Requires admin role. Sends notification to all admins."
  [network-repo user-repo email-sender user-id network-id]
  (assert-admin user-repo user-id)
  (let [n (network/find-by-id network-repo network-id)]
    (when-not n
      (throw (ex-info "Network not found" {:network-id network-id})))
    (network/delete! network-repo network-id)
    (mu/log ::network-deleted :network-id network-id :network-name (:network/name n))
    ;; Notify all admins
    (let [all-users    (user/find-all user-repo)
          admin-emails (keep (fn [u]
                               (when (= :admin (:user/role u))
                                 (:user/email u)))
                             all-users)]
      (when (seq admin-emails)
        (email-sender/send-admin-notification!
          email-sender
          admin-emails
          (str "Réseau supprimé : " (:network/name n))
          (str "<h2>Réseau supprimé</h2>"
               "<p>Le réseau <strong>" (:network/name n) "</strong> a été supprimé par un administrateur.</p>"
               "<p>Détails :</p>"
               "<ul>"
               "<li>Nom : " (:network/name n) "</li>"
               "<li>Localisation : " (:network/center-lat n) ", " (:network/center-lng n) "</li>"
               "<li>Rayon : " (:network/radius-km n) " km</li>"
               "</ul>"))))
    n))

(defn update-network
  "Update a network's editable fields. Requires admin role."
  [network-repo user-repo user-id network-id attrs]
  (assert-admin user-repo user-id)
  (let [n (network/find-by-id network-repo network-id)]
    (when-not n
      (throw (ex-info "Network not found" {:network-id network-id})))
    (let [n' (network/build-network
               (merge n (select-keys attrs [:network/name :network/center-lat
                                            :network/center-lng :network/radius-km
                                            :network/description :network/price-per-kwh])))]
      (network/save! network-repo n')
      (mu/log ::network-updated :network-id network-id)
      n')))

;; ── Public serialization (whitelist) ─────────────────────────────────────────

(defn- serialize-public-network [n]
  (select-keys n [:network/id :network/name :network/center-lat
                  :network/center-lng :network/radius-km :network/lifecycle
                  :network/description :network/price-per-kwh]))

(defn- serialize-public-production [p]
  (select-keys p [:production/id :production/energy-type
                  :production/installed-power :production/producer-address]))

;; ── Network detail (public) ─────────────────────────────────────────────────

(defn- aggregate-network-detail
  "Shared aggregation logic for network detail."
  [net production-repo consumption-repo]
  (let [network-id     (:network/id net)
        productions    (production/find-by-network-id production-repo network-id)
        active-prods   (filterv #(= :active (:production/lifecycle %)) productions)
        consumer-count (consumption/count-by-network-id consumption-repo network-id)
        total-kwc      (reduce + 0 (map :production/installed-power active-prods))
        power-by-type  (reduce (fn [acc p]
                                  (update acc (:production/energy-type p)
                                          (fnil + 0) (:production/installed-power p)))
                                {} active-prods)
        energy-mix     (when (pos? total-kwc)
                         (into {} (map (fn [[k v]] [k (Math/round (double (/ (* 100 v) total-kwc)))])
                                      power-by-type)))]
    (mu/log ::network-detail-fetched :network-id network-id
            :production-count (count active-prods)
            :consumer-count consumer-count)
    {:network         (serialize-public-network net)
     :productions     (mapv serialize-public-production active-prods)
     :consumer-count  consumer-count
     :total-capacity-kwc total-kwc
     :energy-mix      (or energy-mix {})}))

(defn get-network-detail
  "Aggregate a public network with its active productions and consumer count.
  Throws ex-info if the network does not exist or is not public."
  [network-repo production-repo consumption-repo network-id]
  (let [net (network/find-by-id network-repo network-id)]
    (when-not net
      (throw (ex-info "Network not found" {:network-id network-id})))
    (when (not= :public (:network/lifecycle net))
      (throw (ex-info "Network not found" {:network-id network-id})))
    (aggregate-network-detail net production-repo consumption-repo)))

(defn get-network-detail-admin
  "Admin: aggregate any network regardless of lifecycle. Requires admin role."
  [network-repo production-repo consumption-repo user-repo user-id network-id]
  (assert-admin user-repo user-id)
  (let [net (network/find-by-id network-repo network-id)]
    (when-not net
      (throw (ex-info "Network not found" {:network-id network-id})))
    (aggregate-network-detail net production-repo consumption-repo)))
