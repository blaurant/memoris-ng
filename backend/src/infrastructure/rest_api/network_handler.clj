(ns infrastructure.rest-api.network-handler
  (:require [application.network-scenarios :as scenarios]))

(defn list-networks-handler
  "GET /api/v1/networks — returns all networks serialised as JSON."
  [network-repo]
  (fn [_request]
    {:status 200
     :body   (scenarios/list-networks network-repo)}))

(defn check-eligibility-handler
  "POST /api/v1/networks/check-eligibility — body {:lat double :lng double :address string}
  Returns eligibility result for the supplied coordinates.
  Persists the check with address for admin review."
  [network-repo ec-repo]
  (fn [request]
    (let [{:keys [lat lng address]} (:body-params request)]
      (if (and (number? lat) (number? lng))
        {:status 200
         :body   (scenarios/check-eligibility network-repo ec-repo (double lat) (double lng) address)}
        {:status 400
         :body   {:error "Missing or invalid lat/lng parameters"}}))))

(defn subscribe-notification-handler
  "POST /api/v1/networks/eligibility-checks/:id/notify — body {:email string}
  Adds a notification email to an existing eligibility check."
  [ec-repo]
  (fn [request]
    (let [check-id (get-in request [:path-params :id])
          email    (get-in request [:body-params :email])]
      (if (and (seq check-id) (seq email))
        (try
          (scenarios/subscribe-notification ec-repo check-id email)
          {:status 200
           :body   {:message "Notification enregistrée."}}
          (catch clojure.lang.ExceptionInfo e
            {:status 404
             :body   {:error (.getMessage e)}}))
        {:status 400
         :body   {:error "Missing check-id or email"}}))))

(defn routes
  "Returns Reitit route vectors for network-related endpoints."
  [network-repo ec-repo]
  [["/api/v1/networks"
    {:get (list-networks-handler network-repo)}]
   ["/api/v1/networks/check-eligibility"
    {:post (check-eligibility-handler network-repo ec-repo)}]
   ["/api/v1/networks/eligibility-checks/:id/notify"
    {:post (subscribe-notification-handler ec-repo)}]])
