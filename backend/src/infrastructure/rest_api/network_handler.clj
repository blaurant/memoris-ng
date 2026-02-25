(ns infrastructure.rest-api.network-handler
  (:require [application.network-scenarios :as scenarios]))

(defn list-networks-handler
  "GET /api/v1/networks — returns all networks serialised as JSON."
  [network-repo]
  (fn [_request]
    {:status 200
     :body   (scenarios/list-networks network-repo)}))

(defn check-eligibility-handler
  "POST /api/v1/networks/check-eligibility — body {:lat double :lng double}
  Returns eligibility result for the supplied coordinates."
  [network-repo]
  (fn [request]
    (let [{:keys [lat lng]} (:body-params request)]
      (if (and (number? lat) (number? lng))
        {:status 200
         :body   (scenarios/check-eligibility network-repo (double lat) (double lng))}
        {:status 400
         :body   {:error "Missing or invalid lat/lng parameters"}}))))

(defn routes
  "Returns Reitit route vectors for network-related endpoints."
  [network-repo]
  [["/api/v1/networks"
    {:get (list-networks-handler network-repo)}]
   ["/api/v1/networks/check-eligibility"
    {:post (check-eligibility-handler network-repo)}]])
