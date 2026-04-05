(ns infrastructure.rest-api.admin-handler
  (:require [application.consumption-scenarios :as consumption-scenarios]
            [application.network-scenarios :as network-scenarios]
            [application.production-scenarios :as production-scenarios]
            [application.user-scenarios :as user-scenarios]
            [clj-http.client :as http]
            [clojure.string]
            [com.brunobonacci.mulog :as mu]
            [domain.alert-banner :as alert]
            [domain.consumption :as consumption]
            [domain.document-signer :as document-signer]
            [domain.network :as network]
            [domain.production :as production]
            [domain.user :as user]
            [domain.id :as id]
            [infrastructure.rest-api.admin-middleware :as admin-mw]
            [infrastructure.rest-api.auth-middleware :as auth-mw])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (java.util.zip ZipEntry ZipOutputStream)))

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
      (let [{:keys [name center-lat center-lng radius-km description price-per-kwh]} (:body-params request)
            n (network-scenarios/create-network network-repo user-repo (user-id request) (id/build-id)
                                                name
                                                (double center-lat)
                                                (double center-lng)
                                                (double (or radius-km 1.0))
                                                {:description description
                                                 :price-per-kwh price-per-kwh})]
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

(defn- validate-network-handler [user-repo network-repo]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))
            n (network-scenarios/validate-network
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

(defn- serialize-production [p network-repo user-repo]
  (let [nid (:production/network-id p)
        net-name (when nid
                   (some-> (network/find-by-id network-repo nid)
                           :network/name))
        user (user/find-by-id user-repo (:production/user-id p))]
    (-> p
        (update :production/id str)
        (update :production/user-id str)
        (update :production/lifecycle name)
        (cond-> (:production/energy-type p)
                (update :production/energy-type name)
                nid
                (update :production/network-id str)
                net-name
                (assoc :production/network-name net-name)
                user
                (assoc :production/user-name (:user/name user)
                       :production/user-email (:user/email user))))))

(defn- list-productions-handler [production-repo network-repo user-repo]
  (fn [_request]
    {:status 200
     :body   (mapv #(serialize-production % network-repo user-repo) (production/find-all production-repo))}))

(defn- update-network-handler [user-repo network-repo]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))
            {:keys [name center-lat center-lng radius-km description price-per-kwh]} (:body-params request)
            attrs (cond-> {}
                    name           (assoc :network/name name)
                    center-lat     (assoc :network/center-lat (double center-lat))
                    center-lng     (assoc :network/center-lng (double center-lng))
                    radius-km      (assoc :network/radius-km (double radius-km))
                    (some? description)   (assoc :network/description description)
                    price-per-kwh  (assoc :network/price-per-kwh (double price-per-kwh)))
            n' (network-scenarios/update-network
                 network-repo user-repo (user-id request) network-id attrs)]
        {:status 200
         :body   (serialize-network n')})
      (catch Exception e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- get-network-detail-admin-handler [user-repo network-repo production-repo consumption-repo]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))
            detail (network-scenarios/get-network-detail-admin
                     network-repo production-repo consumption-repo
                     user-repo (user-id request) network-id)]
        {:status 200
         :body   detail})
      (catch clojure.lang.ExceptionInfo e
        {:status 404
         :body   {:error (.getMessage e)}}))))

(defn- delete-network-handler [user-repo network-repo consumption-repo production-repo email-sender]
  (fn [request]
    (try
      (let [network-id (id/build-id (get-in request [:path-params :id]))
            n (network-scenarios/delete-network
                network-repo consumption-repo production-repo
                user-repo email-sender (user-id request) network-id)]
        {:status 200
         :body   {:ok true :deleted-network (:network/name n)}})
      (catch clojure.lang.ExceptionInfo e
        (let [data (ex-data e)]
          (cond
            (:consumptions data)
            {:status 409
             :body   {:error        (.getMessage e)
                      :consumptions (:consumptions data)
                      :productions  (:productions data)}}
            (.contains (.getMessage e) "not found")
            {:status 404 :body {:error (.getMessage e)}}
            :else
            {:status 400 :body {:error (.getMessage e)}}))))))

(defn- activate-production-handler [production-repo network-repo user-repo]
  (fn [request]
    (try
      (let [production-id (id/build-id (get-in request [:path-params :id]))
            p' (production-scenarios/activate-production production-repo network-repo production-id)]
        {:status 200
         :body   (serialize-production p' network-repo user-repo)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- update-production-monthly-history-handler [production-repo network-repo user-repo]
  (fn [request]
    (try
      (let [production-id (id/build-id (get-in request [:path-params :id]))
            entries       (get-in request [:body-params :monthly-history])
            entries       (mapv (fn [e]
                                  {:year  (int (:year e))
                                   :month (int (:month e))
                                   :kwh   (double (:kwh e))})
                                entries)
            p' (production-scenarios/update-monthly-history
                 production-repo production-id entries)]
        {:status 200
         :body   (serialize-production p' network-repo user-repo)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- serialize-consumption [c network-repo user-repo]
  (let [nid (:consumption/network-id c)
        net-name (when nid
                   (some-> (network/find-by-id network-repo nid)
                           :network/name))
        user (user/find-by-id user-repo (:consumption/user-id c))]
    (-> c
        (update :consumption/id str)
        (update :consumption/user-id str)
        (update :consumption/lifecycle name)
        (cond-> nid       (update :consumption/network-id str)
                net-name  (assoc :consumption/network-name net-name)
                user      (assoc :consumption/user-name (:user/name user)
                                 :consumption/user-email (:user/email user))))))

(defn- list-consumptions-handler [consumption-repo network-repo user-repo]
  (fn [_request]
    {:status 200
     :body   (mapv #(serialize-consumption % network-repo user-repo) (consumption/find-all consumption-repo))}))

(defn- delete-consumption-handler [consumption-repo]
  (fn [request]
    (try
      (let [consumption-id (id/build-id (get-in request [:path-params :id]))]
        (consumption/delete! consumption-repo consumption-id)
        {:status 200
         :body   {:ok true}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- activate-consumption-handler [consumption-repo network-repo user-repo]
  (fn [request]
    (try
      (let [consumption-id (id/build-id (get-in request [:path-params :id]))
            c' (consumption-scenarios/activate-consumption consumption-repo consumption-id)]
        {:status 200
         :body   (serialize-consumption c' network-repo user-repo)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- update-monthly-history-handler [consumption-repo network-repo user-repo]
  (fn [request]
    (try
      (let [consumption-id (id/build-id (get-in request [:path-params :id]))
            entries        (get-in request [:body-params :monthly-history])
            entries        (mapv (fn [e]
                                  {:year  (int (:year e))
                                   :month (int (:month e))
                                   :kwh   (double (:kwh e))})
                                entries)
            c' (consumption-scenarios/update-monthly-history
                 consumption-repo consumption-id entries)]
        {:status 200
         :body   (serialize-consumption c' network-repo user-repo)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- sanitize-filename
  "Replace spaces and special chars with underscores for safe filenames."
  [s]
  (when s
    (-> s
        (clojure.string/replace #"[^a-zA-Z0-9àâäéèêëïîôùûüÿçÀÂÄÉÈÊËÏÎÔÙÛÜŸÇ_\-]" "_")
        (clojure.string/replace #"_+" "_"))))

(defn- download-pdf-bytes
  "Download PDF bytes from a URL. Returns byte array or nil on failure."
  [url]
  (try
    (let [resp (http/get url {:as :byte-array :throw-exceptions false})]
      (when (<= 200 (:status resp) 299)
        (:body resp)))
    (catch Exception e
      (mu/log ::pdf-download-failed :url url :error (.getMessage e))
      nil)))

(defn- collect-contract-entries
  "Collect all signed contract entries with their submission IDs and metadata."
  [user-repo consumption-repo]
  (let [users        (user/find-all user-repo)
        consumptions (consumption/find-all consumption-repo)]
    (concat
      ;; Adhesions
      (for [u     users
            :when (and (:user/adhesion-signed-at u)
                       (:user/docuseal-submission-id u))]
        {:type          "adhesion"
         :submission-id (:user/docuseal-submission-id u)
         :name          (or (:user/name u) (str (:user/email u)))
         :signed-at     (:user/adhesion-signed-at u)})
      ;; Producer contracts
      (for [c     consumptions
            :when (:consumption/docuseal-producer-submission-id c)
            :let  [u (user/find-by-id user-repo (:consumption/user-id c))]]
        {:type          "producteur"
         :submission-id (:consumption/docuseal-producer-submission-id c)
         :name          (or (:user/name u) (str (:user/email u)))
         :signed-at     (:consumption/producer-contract-signed-at c)})
      ;; SEPA mandates
      (for [c     consumptions
            :when (:consumption/docuseal-sepa-submission-id c)
            :let  [u (user/find-by-id user-repo (:consumption/user-id c))]]
        {:type          "sepa"
         :submission-id (:consumption/docuseal-sepa-submission-id c)
         :name          (or (:user/name u) (str (:user/email u)))
         :signed-at     (:consumption/sepa-mandate-signed-at c)}))))

(defn- export-contracts-zip-handler [user-repo consumption-repo document-signer]
  (fn [_request]
    (try
      (let [entries   (collect-contract-entries user-repo consumption-repo)
            baos      (ByteArrayOutputStream.)
            zos       (ZipOutputStream. baos)
            added     (atom 0)]
        (doseq [{:keys [type submission-id name signed-at]} entries]
          (when-let [url (document-signer/get-signed-document-url document-signer submission-id)]
            (when-let [pdf-bytes (download-pdf-bytes url)]
              (let [date-part (or (some-> signed-at (subs 0 10)) "unknown")
                    filename  (str "contrats/" type "/"
                                   (sanitize-filename name) "_" type "_" date-part ".pdf")]
                (.putNextEntry zos (ZipEntry. filename))
                (.write zos ^bytes pdf-bytes)
                (.closeEntry zos)
                (swap! added inc)))))
        (.close zos)
        (mu/log ::contracts-zip-exported :count @added)
        (if (pos? @added)
          {:status  200
           :headers {"Content-Type"        "application/zip"
                     "Content-Disposition" "attachment; filename=\"contrats-elink-co.zip\""}
           :body    (ByteArrayInputStream. (.toByteArray baos))}
          {:status 200
           :body   {:message "Aucun contrat signé à exporter."}}))
      (catch Exception e
        (mu/log ::contracts-zip-error :error (.getMessage e))
        {:status 500
         :body   {:error "Erreur lors de la génération du ZIP."}}))))

(defn routes [user-repo network-repo ec-repo alert-banner-repo consumption-repo production-repo email-sender jwt-secret document-signer]
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
   ["/api/v1/admin/networks/:id/detail"
    {:get        (get-network-detail-admin-handler user-repo network-repo production-repo consumption-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks/:id"
    {:put        (update-network-handler user-repo network-repo)
     :delete     (delete-network-handler user-repo network-repo consumption-repo production-repo email-sender)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks/:id/toggle-visibility"
    {:put        (toggle-network-visibility-handler user-repo network-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/networks/:id/validate"
    {:put        (validate-network-handler user-repo network-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/eligibility-checks"
    {:get        (list-eligibility-checks-handler ec-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/consumptions"
    {:get        (list-consumptions-handler consumption-repo network-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/consumptions/:id"
    {:delete     (delete-consumption-handler consumption-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/consumptions/:id/activate"
    {:put        (activate-consumption-handler consumption-repo network-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/consumptions/:id/monthly-history"
    {:put        (update-monthly-history-handler consumption-repo network-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/productions"
    {:get        (list-productions-handler production-repo network-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/productions/:id/activate"
    {:put        (activate-production-handler production-repo network-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/productions/:id/monthly-history"
    {:put        (update-production-monthly-history-handler production-repo network-repo user-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/contracts/export-zip"
    {:get        (export-contracts-zip-handler user-repo consumption-repo document-signer)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]])
