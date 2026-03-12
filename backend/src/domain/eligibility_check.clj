(ns domain.eligibility-check
  (:require [domain.datetime :as dt]
            [domain.id :as id]
            [malli.core :as m]))

(def EligibilityCheck
  [:map
   [:eligibility-check/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:eligibility-check/address string?]
   [:eligibility-check/lat double?]
   [:eligibility-check/lng double?]
   [:eligibility-check/eligible? boolean?]
   [:eligibility-check/network-name {:optional true} [:maybe string?]]
   [:eligibility-check/checked-at [:fn {:error/message "must be a datetime"} dt/datetime?]]
   [:eligibility-check/notification-email {:optional true} [:maybe string?]]])

(defn build-eligibility-check
  "Build and validate an eligibility check record."
  [attrs]
  (if (m/validate EligibilityCheck attrs)
    attrs
    (throw (ex-info "Invalid eligibility check" {:attrs attrs}))))

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol EligibilityCheckRepo
  (save! [repo check] "Persist an eligibility check.")
  (find-by-id [repo id] "Find an eligibility check by ID.")
  (find-all [repo] "Return all eligibility checks."))
