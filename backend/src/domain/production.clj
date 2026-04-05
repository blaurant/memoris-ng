(ns ^{:domain/type :entity} domain.production
    (:require [clojure.string]
              [domain.id :as id]
              [malli.core :as m]
              [malli.util :as mu])
    (:import (java.time Instant)))

(def non-blank-string?
  [:and
   string?
   [:fn {:error/message "must not be blank"} #(not (clojure.string/blank? %))]])

;; ── Schema maps ─────────────────────────────────────────────────────────────

(def BaseProduction
  [:map
   [:production/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:production/user-id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:production/lifecycle [:enum :producer-information :installation-info :payment-info :contract-signature
                           :pending :active :terminated :abandoned]]])

(def ProducerInformation
  [:map
   [:production/producer-address non-blank-string?]
   [:production/network-id [:fn {:error/message "must be a valid ID"} id/id?]]])

(def InstallationInfo
  [:map
   [:production/pdl-prm [:and string? [:re {:error/message "PDL/PRM must be exactly 14 digits"} #"^\d{14}$"]]]
   [:production/installed-power [:and number? [:fn {:error/message "must be positive"} pos?]]]
   [:production/energy-type [:enum :solar :wind :hydro :biomass :cogeneration]]
   [:production/linky-meter non-blank-string?]])

(def iban?
  [:and string?
   [:fn {:error/message "IBAN must be 2 letters + 2 digits + 11-30 alphanumeric characters (spaces allowed)"}
    #(re-matches #"^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$" (clojure.string/upper-case (clojure.string/replace % #"\s" "")))]])

(def PaymentInfo
  [:map
   [:production/iban-holder non-blank-string?]
   [:production/iban iban?]
   [:production/bic {:optional true} [:maybe string?]]
   [:production/payment-address non-blank-string?]])

(def ContractSignature
  [:map
   [:production/adhesion-signed-at {:optional true} string?]])

;; Full schema: step fields optional (for deserialization from storage)
(def Production
  (-> BaseProduction
      (mu/merge (mu/optional-keys ProducerInformation))
      (mu/merge (mu/optional-keys InstallationInfo))
      (mu/merge (mu/optional-keys PaymentInfo))
      (mu/merge (mu/optional-keys ContractSignature))
      (mu/merge [:map
                 [:production/monthly-history {:optional true}
                  [:maybe [:vector [:map
                                    [:year int?]
                                    [:month [:int {:min 1 :max 12}]]
                                    [:kwh double?]]]]]])))

;; ── Validation ──────────────────────────────────────────────────────────────

(defn- validate
       "Validates attrs against schema. Throws ex-info if invalid."
       [schema attrs]
       (if (m/validate schema attrs)
         attrs
         (throw (ex-info "Invalid production" {:attrs  attrs
                                               :errors (m/explain schema attrs)}))))

(defn build-production
      "Validates attrs against the full Production schema. Throws if invalid."
      [attrs]
      (validate Production attrs))

;; ── Factory ─────────────────────────────────────────────────────────────────

(defn create-new-production
      "Create a new production in :producer-information state."
      [id user-id]
      (validate BaseProduction {:production/id        id
                                :production/user-id   user-id
                                :production/lifecycle :producer-information}))

;; ── Monthly history helpers ────────────────────────────────────────────────

(defn last-monthly-kwh
  "Derives the most recent month's kWh from monthly-history, or nil."
  [p]
  (when-let [history (seq (:production/monthly-history p))]
    (:kwh (first (sort-by (juxt :year :month) #(compare %2 %1) history)))))

(defn set-monthly-history
  "Replace the monthly-history on a production. Entries sorted desc by date."
  [p entries]
  (let [sorted (->> entries
                    (sort-by (juxt :year :month) #(compare %2 %1))
                    vec)]
    (assoc p :production/monthly-history sorted)))

;; ── Queries ─────────────────────────────────────────────────────────────────

(defn onboarding?
      "Returns true if the production is in one of the 4 onboarding states."
      [p]
      (contains? #{:producer-information :installation-info :payment-info :contract-signature}
                 (:production/lifecycle p)))

;; ── Transitions ─────────────────────────────────────────────────────────────

(defn- assert-lifecycle
  "Guard: throws if the production is not in the expected lifecycle state."
  [p expected]
  (when (not= expected (:production/lifecycle p))
    (throw (ex-info (str "Expected lifecycle " expected " but was " (:production/lifecycle p))
                    {:expected expected :actual (:production/lifecycle p)}))))

(defn register-producer-information
      "Transition :producer-information -> :installation-info with address and network."
      [p producer-address network-id]
      (let [_ (assert-lifecycle p :producer-information)]
        (validate (mu/merge BaseProduction ProducerInformation)
                  (assoc p
                         :production/lifecycle :installation-info
                         :production/producer-address producer-address
                         :production/network-id network-id))))

(defn register-installation-info
      "Transition :installation-info -> :payment-info with installation details."
      [p pdl-prm installed-power energy-type linky-meter]
      (let [_ (assert-lifecycle p :installation-info)]
        (validate (-> BaseProduction
                      (mu/merge ProducerInformation)
                      (mu/merge InstallationInfo))
                  (assoc p
                         :production/lifecycle :payment-info
                         :production/pdl-prm pdl-prm
                         :production/installed-power installed-power
                         :production/energy-type energy-type
                         :production/linky-meter linky-meter))))

(defn submit-payment-info
      "Transition :payment-info -> :contract-signature with IBAN holder, IBAN, BIC and payment address."
      [p iban-holder iban bic payment-address]
      (let [_ (assert-lifecycle p :payment-info)]
        (validate (-> BaseProduction
                      (mu/merge ProducerInformation)
                      (mu/merge InstallationInfo)
                      (mu/merge PaymentInfo))
                  (cond-> (assoc p
                                 :production/lifecycle :contract-signature
                                 :production/iban-holder iban-holder
                                 :production/iban iban
                                 :production/payment-address payment-address)
                    (seq bic) (assoc :production/bic bic)))))

(defn sign-contract
      "Transitions to :pending if the user's adhesion is signed.
       Throws if adhesion is not signed."
      [p adhesion-signed?]
      (assert-lifecycle p :contract-signature)
      (when-not adhesion-signed?
        (throw (ex-info "Adhesion must be signed before completing production" {})))
      (validate (-> BaseProduction
                    (mu/merge ProducerInformation)
                    (mu/merge InstallationInfo)
                    (mu/merge PaymentInfo))
                (assoc p :production/lifecycle :pending)))

(def ^:private onboarding-states
  #{:producer-information :installation-info :payment-info :contract-signature})

(def ^:private previous-step
  {:installation-info    :producer-information
   :payment-info         :installation-info
   :contract-signature   :payment-info})

(defn go-back
      "Move a production back to the previous onboarding step."
      [p]
      (let [current (:production/lifecycle p)
            prev    (previous-step current)]
        (when-not prev
          (throw (ex-info "Cannot go back from this state"
                          {:lifecycle current})))
        (assoc p :production/lifecycle prev)))

(defn activate
      "Activate a pending production. Transitions :pending -> :active."
      [p]
      (assert-lifecycle p :pending)
      (assoc p :production/lifecycle :active))

(defn abandon
      "Abandon a production during onboarding. Transitions to :abandoned."
      [p]
      (when-not (contains? onboarding-states (:production/lifecycle p))
        (throw (ex-info "Can only abandon a production during onboarding"
                        {:lifecycle (:production/lifecycle p)})))
      (assoc p :production/lifecycle :abandoned))

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol ProductionRepo
  (find-by-id      [repo id]                    "Find a production by ID.")
  (find-by-user-id [repo user-id]               "Find all productions for a user. Returns a vector.")
  (find-by-network-id [repo network-id]         "Find all productions for a network. Returns a vector.")
  (find-all        [repo]                        "Find all productions (admin).")
  (save!           [repo production]
                   [repo original updated]       "Persist a production. 2-arity: insert. 3-arity: optimistic update.")
  (delete!         [repo id]                     "Delete a production by ID."))
