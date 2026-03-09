(ns infrastructure.rest-api.admin-handler
  (:require [application.admin-scenarios :as admin]
            [application.network-scenarios :as network-scenarios]
            [infrastructure.rest-api.admin-middleware :as admin-mw]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-user [u]
  (-> u
      (update :user/id str)
      (update :user/role name)
      (update :user/lifecycle name)
      (update :user/provider name)))

(defn- list-users-handler [user-repo]
  (fn [_request]
    {:status 200
     :body   (mapv serialize-user (admin/list-users user-repo))}))

(defn- list-networks-handler [network-repo]
  (fn [_request]
    {:status 200
     :body   (network-scenarios/list-networks network-repo)}))

(defn routes [user-repo network-repo jwt-secret]
  [["/api/v1/admin/users"
    {:get        (list-users-handler user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks"
    {:get        (list-networks-handler network-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]])
