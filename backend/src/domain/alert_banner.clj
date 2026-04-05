(ns domain.alert-banner
  (:require [malli.core :as m]))

(def AlertBanner
  [:map
   [:alert-banner/id uuid?]
   [:alert-banner/message string?]
   [:alert-banner/active? boolean?]
   [:alert-banner/contract-notifications? {:optional true} boolean?]])

(defn build-alert-banner [attrs]
  (when-not (m/validate AlertBanner attrs)
    (throw (ex-info "Invalid alert banner" {:errors (m/explain AlertBanner attrs)})))
  attrs)

;; Well-known singleton ID for the unique alert banner
(def ^:private singleton-id #uuid "00000000-0000-0000-0000-000000000001")

(defn default-alert-banner []
  (build-alert-banner
    {:alert-banner/id       singleton-id
     :alert-banner/message  ""
     :alert-banner/active?  false}))

(defprotocol AlertBannerRepo
  (find-current [repo])
  (save! [repo banner]))
