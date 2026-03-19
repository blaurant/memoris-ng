(ns ^{:domain/type :entity} domain.network
  (:require [domain.geo :as geo]
            [domain.id :as id]
            [malli.core :as m]))

(def lifecycle-states #{:private :public :pending-validation})

(def Network
  [:map
   [:network/id   [:fn {:error/message "must be a valid ID"} id/id?]]
   [:network/name string?]
   [:network/center-lat double?]
   [:network/center-lng double?]
   [:network/radius-km  {:optional true} double?]
   [:network/description {:optional true} string?]
   [:network/price-per-kwh {:optional true} double?]
   [:network/lifecycle [:enum :pending-validation :private :public ]]])

(defn- round4
  "Round a double to 4 decimal places."
  [d]
  (when d
    (/ (Math/round (* d 10000.0)) 10000.0)))

(defn build-network
  "Validates attrs against the Network schema and returns the network map.
  Throws ex-info if attrs are invalid.
  Defaults :network/radius-km to 1.0 and :network/lifecycle to :private.
  Rounds lat/lng to 4 decimal places."
  [attrs]
  (let [with-defaults (-> attrs
                          (update :network/center-lat round4)
                          (update :network/center-lng round4)
                          (update :network/radius-km #(or % 1.0))
                          (update :network/lifecycle #(or % :private)))]
    (if (m/validate Network with-defaults)
      with-defaults
      (throw (ex-info "Invalid network" {:attrs attrs
                                         :errors (m/explain Network with-defaults)})))))

(defn publish
  "Transition a network from :private to :public."
  [network]
  (when (not= :private (:network/lifecycle network))
    (throw (ex-info "Network is not private" {:lifecycle (:network/lifecycle network)})))
  (assoc network :network/lifecycle :public))

(defn unpublish
  "Transition a network from :public to :private."
  [network]
  (when (not= :public (:network/lifecycle network))
    (throw (ex-info "Network is not public" {:lifecycle (:network/lifecycle network)})))
  (assoc network :network/lifecycle :private))

(defn validate-network
  "Transition a network from :pending-validation to :private."
  [network]
  (when (not= :pending-validation (:network/lifecycle network))
    (throw (ex-info "Network is not pending validation" {:lifecycle (:network/lifecycle network)})))
  (assoc network :network/lifecycle :private))

;; ── Queries ──────────────────────────────────────────────────────────────

(defn contains-point?
  "Returns true if the point (lat, lng) lies within this network's radius."
  [network lat lng]
  (geo/within-network? network lat lng))

(defn find-covering-network
  "Returns the first network that contains the point (lat, lng), or nil."
  [networks lat lng]
  (some #(when (contains-point? % lat lng) %) networks))

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol NetworkRepo
  (find-all   [repo]         "Returns all networks.")
  (find-by-id [repo id]      "Find a network by ID.")
  (save!      [repo network] "Persist a network (insert or update).")
  (delete!    [repo id]      "Delete a network by ID."))
