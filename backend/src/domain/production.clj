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
   [:production/lifecycle [:enum :installation-info :payment-info :contract-signature
                           :pending :active :terminated :abandoned]]])

(def InstallationInfo
  [:map
   [:production/pdl-prm non-blank-string?]
   [:production/installed-power [:and number? [:fn {:error/message "must be positive"} pos?]]]
   [:production/energy-type [:enum :solar :wind :hydro :biomass :cogeneration]]
   [:production/linky-meter non-blank-string?]])

(def PaymentInfo
  [:map
   [:production/iban non-blank-string?]])

(def ContractSignature
  [:map
   [:production/adhesion-signed-at {:optional true} string?]])

;; Full schema: step fields optional (for deserialization from storage)
(def Production
  (-> BaseProduction
      (mu/merge (mu/optional-keys InstallationInfo))
      (mu/merge (mu/optional-keys PaymentInfo))
      (mu/merge (mu/optional-keys ContractSignature))))

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
      "Create a new production in :installation-info state."
      [id user-id]
      (validate BaseProduction {:production/id        id
                                :production/user-id   user-id
                                :production/lifecycle :installation-info}))

;; ── Queries ─────────────────────────────────────────────────────────────────

(defn onboarding?
      "Returns true if the production is in one of the 3 onboarding states."
      [p]
      (contains? #{:installation-info :payment-info :contract-signature}
                 (:production/lifecycle p)))

;; ── Transitions ─────────────────────────────────────────────────────────────

(defn- assert-lifecycle
  "Guard: throws if the production is not in the expected lifecycle state."
  [p expected]
  (when (not= expected (:production/lifecycle p))
    (throw (ex-info (str "Expected lifecycle " expected " but was " (:production/lifecycle p))
                    {:expected expected :actual (:production/lifecycle p)}))))

(defn register-installation-info
      "Transition :installation-info -> :payment-info with installation details."
      [p pdl-prm installed-power energy-type linky-meter]
      (let [_ (assert-lifecycle p :installation-info)]
        (validate (mu/merge BaseProduction InstallationInfo)
                  (assoc p
                         :production/lifecycle :payment-info
                         :production/pdl-prm pdl-prm
                         :production/installed-power installed-power
                         :production/energy-type energy-type
                         :production/linky-meter linky-meter))))

(defn submit-payment-info
      "Transition :payment-info -> :contract-signature with IBAN."
      [p iban]
      (let [_ (assert-lifecycle p :payment-info)]
        (validate (-> BaseProduction
                      (mu/merge InstallationInfo)
                      (mu/merge PaymentInfo))
                  (assoc p
                         :production/lifecycle :contract-signature
                         :production/iban iban))))

(defn sign-contract
      "Sign the adhesion contract. Transitions directly to :pending."
      ([p]
       (sign-contract p (str (Instant/now))))
      ([p signed-at]
       (let [_ (assert-lifecycle p :contract-signature)]
         (validate (-> BaseProduction
                       (mu/merge InstallationInfo)
                       (mu/merge PaymentInfo)
                       (mu/merge ContractSignature))
                   (assoc p
                          :production/lifecycle :pending
                          :production/adhesion-signed-at signed-at)))))

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol ProductionRepo
  (find-by-id      [repo id]                    "Find a production by ID.")
  (find-by-user-id [repo user-id]               "Find all productions for a user. Returns a vector.")
  (find-all        [repo]                        "Find all productions (admin).")
  (save!           [repo production]
                   [repo original updated]       "Persist a production. 2-arity: insert. 3-arity: optimistic update."))
