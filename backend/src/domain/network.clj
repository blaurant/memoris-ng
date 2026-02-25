(ns domain.network
  (:require [malli.core :as m]))

(def Network
  [:map
   [:network/id   uuid?]
   [:network/name string?]
   [:network/center-lat double?]
   [:network/center-lng double?]
   [:network/radius-km  {:optional true} double?]])

(defn build-network
  "Validates attrs against the Network schema and returns the network map.
  Throws ex-info if attrs are invalid.
  Defaults :network/radius-km to 10.0 when not provided."
  [attrs]
  (let [with-defaults (update attrs :network/radius-km #(or % 10.0))]
    (if (m/validate Network with-defaults)
      with-defaults
      (throw (ex-info "Invalid network" {:attrs attrs
                                         :errors (m/explain Network with-defaults)})))))
