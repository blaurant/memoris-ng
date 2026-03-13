(ns infrastructure.rest-api.admin-handler
  (:require [application.network-scenarios :as network-scenarios]
            [application.user-scenarios :as user-scenarios]
            [domain.alert-banner :as alert]
            [domain.consumption :as consumption]
            [domain.production :as production]
            [domain.id :as id]
            [infrastructure.rest-api.admin-middleware :as admin-mw]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-user [u]
  (-> u
      (update :user/id str)
      (update :user/role name)
      (update :user/lifecycle name)
      (update :user/provider name)))

(defn- user-id [request]
  (id/build-id (get-in request [:identity :sub])))

(defn- list-users-handler [user-repo]
  (fn [request]
    {:status 200
     :body   (mapv serialize-user (user-scenarios/list-all-users user-repo (user-id request)))}))

(defn- serialize-network [n]
  (update n :network/lifecycle name))

(defn- list-networks-handler [user-repo network-repo consumption-repo]
  (fn [request]
    (let [networks (network-scenarios/list-all-networks network-repo user-repo (user-id request))]
      {:status 200
       :body   (mapv (fn [n]
                       (-> (serialize-network n)
                           (assoc :network/consumption-count
                                  (consumption/count-by-network-id consumption-repo (:network/id n)))))
                     networks)})))

(defn- create-network-handler [user-repo network-repo]
  (fn [request]
    (try
      (let [{:keys [name center-lat center-lng radius-km]} (:body-params request)
            n (network-scenarios/create-network network-repo user-repo (user-id request) (id/build-id)
                                                name
                                                (double center-lat)
                                                (double center-lng)
                                                (double (or radius-km 10.0)))]
        {:status 201
         :body   n})
      (catch Exception e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- toggle-network-visibility-handler [user-repo network-repo]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))
            n (network-scenarios/toggle-network-visibility
                network-repo user-repo (user-id request) network-id)]
        {:status 200
         :body   (serialize-network n)})
      (catch Exception e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- serialize-eligibility-check [check]
  (-> check
      (update :eligibility-check/id str)
      (update :eligibility-check/checked-at str)))

(defn- list-eligibility-checks-handler [ec-repo user-repo]
  (fn [request]
    {:status 200
     :body   (mapv serialize-eligibility-check
                   (network-scenarios/list-eligibility-checks ec-repo user-repo (user-id request)))}))

(defn- get-alert-handler [alert-banner-repo]
  (fn [_request]
    (let [banner (or (alert/find-current alert-banner-repo) (alert/default-alert-banner))]
      {:status 200
       :body   {:message (:alert-banner/message banner)
                :active  (:alert-banner/active? banner)}})))

(defn- update-alert-handler [alert-banner-repo]
  (fn [request]
    (try
      (let [{:keys [message active]} (:body-params request)
            current (or (alert/find-current alert-banner-repo) (alert/default-alert-banner))
            updated (alert/build-alert-banner
                      (assoc current
                             :alert-banner/message (or message "")
                             :alert-banner/active? (boolean active)))]
        (alert/save! alert-banner-repo updated)
        {:status 200
         :body   {:message (:alert-banner/message updated)
                  :active  (:alert-banner/active? updated)}})
      (catch Exception e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- serialize-production [p]
  (-> p
      (update :production/id str)
      (update :production/user-id str)
      (update :production/lifecycle name)
      (cond-> (:production/energy-type p)
              (update :production/energy-type name))))

(defn- list-productions-handler [production-repo]
  (fn [_request]
    {:status 200
     :body   (mapv serialize-production (production/find-all production-repo))}))

(defn routes [user-repo network-repo ec-repo alert-banner-repo consumption-repo production-repo jwt-secret]
  [["/api/v1/alert"
    {:get (get-alert-handler alert-banner-repo)}]
   ["/api/v1/admin/alert"
    {:get        (get-alert-handler alert-banner-repo)
     :put        (update-alert-handler alert-banner-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/users"
    {:get        (list-users-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks"
    {:get        (list-networks-handler user-repo network-repo consumption-repo)
     :post       (create-network-handler user-repo network-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks/:id/toggle-visibility"
    {:put        (toggle-network-visibility-handler user-repo network-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/eligibility-checks"
    {:get        (list-eligibility-checks-handler ec-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/productions"
    {:get        (list-productions-handler production-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]])
