(ns infrastructure.rest-api.admin-handler
  (:require [application.network-scenarios :as network-scenarios]
            [application.user-scenarios :as user-scenarios]
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

(defn- list-networks-handler [user-repo network-repo]
  (fn [request]
    {:status 200
     :body   (mapv serialize-network
                   (network-scenarios/list-all-networks network-repo user-repo (user-id request)))}))

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

(defn routes [user-repo network-repo ec-repo jwt-secret]
  [["/api/v1/admin/users"
    {:get        (list-users-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks"
    {:get        (list-networks-handler user-repo network-repo)
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
                  [admin-mw/wrap-admin-only]]}]])
