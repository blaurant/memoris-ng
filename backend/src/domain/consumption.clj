(ns domain.consumption
    (:require [domain.id :as id]
      [malli.core :as m]
      [malli.util :as mu]))

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
   [:consumption/contract-signed-at {:optional true} [:maybe string?]]
   [:consumption/producer-contract-signed-at {:optional true} [:maybe string?]]
   [:consumption/sepa-mandate-signed-at {:optional true} [:maybe string?]]])

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
      "Returns true if the consumption is in one of the 3 onboarding states."
      [c]
      (contains? #{:consumer-information :linky-reference :billing-address :contract-signature}
                 (:consumption/lifecycle c)))

;; ── Transitions ─────────────────────────────────────────────────────────────

(defn register-consumer-information
      "Transition :consumer-information -> :linky-reference with address and network-id."
      [c address network-id]
      (when-not (= :consumer-information (:consumption/lifecycle c))
                (throw (ex-info "Can only register consumer informations from :consumer-information state"
                                {:lifecycle (:consumption/lifecycle c)})))
      (validate (mu/merge BaseConsumption ConsumerInformation)
                (assoc c
                       :consumption/lifecycle :linky-reference
                       :consumption/consumer-address address
                       :consumption/network-id network-id)))

(defn associate-linky-reference
      "Transition :linky-reference -> :billing-address with linky reference."
      [c linky-ref]
      (when-not (= :linky-reference (:consumption/lifecycle c))
                (throw (ex-info "Can only associate linky reference from :linky-reference state"
                                {:lifecycle (:consumption/lifecycle c)})))
      (validate (-> BaseConsumption
                    (mu/merge ConsumerInformation)
                    (mu/merge LinkyReference))
                (assoc c
                       :consumption/lifecycle :billing-address
                       :consumption/linky-reference linky-ref)))

(defn complete-billing-address
      "Transition :billing-address -> :pending with billing address."
      [c billing-addr]
      (when-not (= :billing-address (:consumption/lifecycle c))
                (throw (ex-info "Can only complete billing address from :billing-address state"
                                {:lifecycle (:consumption/lifecycle c)})))
      (validate (-> BaseConsumption
                    (mu/merge ConsumerInformation)
                    (mu/merge LinkyReference)
                    (mu/merge BillingAddress))
                (assoc c
                       :consumption/lifecycle :contract-signature
                       :consumption/billing-address billing-addr)))

(def ^:private contract-type->key
  {:proxywatt :consumption/contract-signed-at
   :producer  :consumption/producer-contract-signed-at
   :sepa      :consumption/sepa-mandate-signed-at})

(defn- all-contracts-signed? [c]
  (and (some? (:consumption/contract-signed-at c))
       (some? (:consumption/producer-contract-signed-at c))
       (some? (:consumption/sepa-mandate-signed-at c))))

(defn sign-contract
      "Sign one contract (contract-type = :proxywatt | :producer | :sepa).
       Transitions to :pending only when all 3 contracts are signed."
      [c contract-type signed-at]
      (when-not (= :contract-signature (:consumption/lifecycle c))
                (throw (ex-info "Can only sign contract from :contract-signature state"
                                {:lifecycle (:consumption/lifecycle c)})))
      (let [k  (or (contract-type->key contract-type)
                    (throw (ex-info "Unknown contract type" {:contract-type contract-type})))
            c' (assoc c k signed-at)
            c' (assoc c' :consumption/lifecycle
                      (if (all-contracts-signed? c') :pending :contract-signature))]
        (validate (-> BaseConsumption
                      (mu/merge ConsumerInformation)
                      (mu/merge LinkyReference)
                      (mu/merge BillingAddress)
                      (mu/merge ContractSignature))
                  c')))
