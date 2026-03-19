(ns ^{:domain/type :entity} domain.consumption
    (:require [clojure.string]
              [domain.id :as id]
              [malli.core :as m]
              [malli.util :as mu])
    (:import (java.time Instant)))

(def address?
  [:and
   string?
   [:fn {:error/message "must not be blank"} #(not (clojure.string/blank? %))]])

(def linky-reference?
  [:and
   string?
   [:fn {:error/message "must not be blank"} #(not (clojure.string/blank? %))]])

;; ── Schema maps ─────────────────────────────────────────────────────────────

(def BaseConsumption
  [:map
   [:consumption/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:consumption/user-id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:consumption/lifecycle [:enum :consumer-information :linky-reference
                            :billing-address :contract-signature :pending :active
                            :terminated :abandoned]]])

(def ConsumerInformation
  [:map
   [:consumption/consumer-address address?]
   [:consumption/network-id [:fn {:error/message "must be a valid ID"} id/id?]]])

(def LinkyReference
  [:map
   [:consumption/linky-reference linky-reference?]])

(def BillingAddress
  [:map
   [:consumption/billing-address address?]])

(def ContractSignature
  [:map
   [:consumption/contract-signed-at {:optional true} string?]
   [:consumption/producer-contract-signed-at {:optional true} string?]
   [:consumption/sepa-mandate-signed-at {:optional true} string?]])

;; Full schema: step fields optional (for deserialization from storage)
(def Consumption
  (-> BaseConsumption
      (mu/merge (mu/optional-keys ConsumerInformation))
      (mu/merge (mu/optional-keys LinkyReference))
      (mu/merge (mu/optional-keys BillingAddress))
      (mu/merge (mu/optional-keys ContractSignature))
      (mu/merge [:map
                 [:consumption/price-per-kwh {:optional true} [:maybe double?]]
                 [:consumption/contract-start-date {:optional true} [:maybe string?]]
                 [:consumption/last-monthly-kwh {:optional true} [:maybe double?]]])))

;; ── Validation ──────────────────────────────────────────────────────────────

(defn- validate
       "Validates attrs against schema. Throws ex-info if invalid."
       [schema attrs]
       (if (m/validate schema attrs)
         attrs
         (throw (ex-info "Invalid consumption" {:attrs  attrs
                                                :errors (m/explain schema attrs)}))))

(defn build-consumption
      "Validates attrs against the full Consumption schema. Throws if invalid."
      [attrs]
      (validate Consumption attrs))

;; ── Factory ─────────────────────────────────────────────────────────────────

(defn create-new-consumption
      "Create a new consumption in :consumer-information state."
      [id user-id]
      (validate BaseConsumption {:consumption/id        id
                                 :consumption/user-id   user-id
                                 :consumption/lifecycle :consumer-information}))

;; ── Queries ─────────────────────────────────────────────────────────────────

(defn onboarding?
      "Returns true if the consumption is in one of the 4 onboarding states."
      [c]
      (contains? #{:consumer-information :linky-reference :billing-address :contract-signature}
                 (:consumption/lifecycle c)))

;; ── Transitions ─────────────────────────────────────────────────────────────

(defn- assert-lifecycle
  "Guard: throws if the consumption is not in the expected lifecycle state."
  [c expected]
  (when (not= expected (:consumption/lifecycle c))
    (throw (ex-info (str "Expected lifecycle " expected " but was " (:consumption/lifecycle c))
                    {:expected expected :actual (:consumption/lifecycle c)}))))

(defn register-consumer-information
      "Transition :consumer-information -> :linky-reference with address and network-id."
      [c address network-id]
      (let [_ (assert-lifecycle c :consumer-information)]
        (validate (mu/merge BaseConsumption ConsumerInformation)
                  (assoc c
                         :consumption/lifecycle :linky-reference
                         :consumption/consumer-address address
                         :consumption/network-id network-id))))

(defn associate-linky-reference
      "Transition :linky-reference -> :billing-address with linky reference."
      [c linky-ref]
      (let [_ (assert-lifecycle c :linky-reference)]
        (validate (-> BaseConsumption
                      (mu/merge ConsumerInformation)
                      (mu/merge LinkyReference))
                  (assoc c
                         :consumption/lifecycle :billing-address
                         :consumption/linky-reference linky-ref))))

(defn complete-billing-address
      "Transition :billing-address -> :contract-signature with billing address."
      [c billing-addr]
      (let [_ (assert-lifecycle c :billing-address)]
        (validate (-> BaseConsumption
                      (mu/merge ConsumerInformation)
                      (mu/merge LinkyReference)
                      (mu/merge BillingAddress))
                  (assoc c
                         :consumption/lifecycle :contract-signature
                         :consumption/billing-address billing-addr))))

(def ^:private contract-type->key
  {:producer  :consumption/producer-contract-signed-at
   :sepa      :consumption/sepa-mandate-signed-at})

(defn- consumption-contracts-signed? [c]
  (and (some? (:consumption/producer-contract-signed-at c))
       (some? (:consumption/sepa-mandate-signed-at c))))

(defn sign-contract
      "Sign one contract (contract-type = :producer | :sepa).
       Transitions to :pending only when both contracts are signed
       AND the user has signed the adhesion (adhesion-signed? must be true)."
      ([c contract-type adhesion-signed?]
       (sign-contract c contract-type adhesion-signed? (str (Instant/now))))
      ([c contract-type adhesion-signed? signed-at]
       (let [_ (assert-lifecycle c :contract-signature)
             k  (or (contract-type->key contract-type)
                     (throw (ex-info "Unknown contract type" {:contract-type contract-type})))
             c' (assoc c k signed-at)
             c' (assoc c' :consumption/lifecycle
                       (if (and adhesion-signed? (consumption-contracts-signed? c'))
                         :pending
                         :contract-signature))]
         (validate (-> BaseConsumption
                       (mu/merge ConsumerInformation)
                       (mu/merge LinkyReference)
                       (mu/merge BillingAddress)
                       (mu/merge ContractSignature))
                   c'))))

(def ^:private onboarding-states
  #{:consumer-information :linky-reference :billing-address :contract-signature})

(def ^:private previous-step
  {:linky-reference    :consumer-information
   :billing-address    :linky-reference
   :contract-signature :billing-address})

(defn go-back
      "Move a consumption back to the previous onboarding step."
      [c]
      (let [current (:consumption/lifecycle c)
            prev    (previous-step current)]
        (when-not prev
          (throw (ex-info "Cannot go back from this state"
                          {:lifecycle current})))
        (assoc c :consumption/lifecycle prev)))

(defn abandon
      "Abandon a consumption during onboarding. Transitions to :abandoned."
      [c]
      (when-not (contains? onboarding-states (:consumption/lifecycle c))
        (throw (ex-info "Can only abandon a consumption during onboarding"
                        {:lifecycle (:consumption/lifecycle c)})))
      (assoc c :consumption/lifecycle :abandoned))

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol ConsumptionRepo
  (find-by-id      [repo id]                    "Find a consumption by ID.")
  (find-by-user-id [repo user-id]               "Find all consumptions for a user. Returns a vector.")
  (count-by-network-id [repo network-id]        "Count consumptions linked to a network.")
  (save!           [repo consumption]
                   [repo original updated]       "Persist a consumption. 2-arity: insert. 3-arity: optimistic update (fails if entity changed since read)."))
