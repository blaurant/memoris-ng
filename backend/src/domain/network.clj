(ns ^{:domain/type :entity} domain.network
  (:require [domain.id :as id]
            [malli.core :as m]))

(def lifecycle-states #{:private :public :pending-validation})

(def Network
  [:map
   [:network/id   [:fn {:error/message "must be a valid ID"} id/id?]]
   [:network/name string?]
   [:network/center-lat double?]
   [:network/center-lng double?]
   [:network/radius-km  {:optional true} double?]
   [:network/lifecycle [:enum :private :public :pending-validation]]])

(defn build-network
  "Validates attrs against the Network schema and returns the network map.
  Throws ex-info if attrs are invalid.
  Defaults :network/radius-km to 10.0 and :network/lifecycle to :private."
  [attrs]
  (let [with-defaults (-> attrs
                          (update :network/radius-km #(or % 10.0))
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

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol NetworkRepo
  (find-all   [repo]         "Returns all networks.")
  (find-by-id [repo id]      "Find a network by ID.")
  (save!      [repo network] "Persist a network (insert or update)."))
