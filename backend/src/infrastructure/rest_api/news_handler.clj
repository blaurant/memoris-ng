(ns infrastructure.rest-api.news-handler
  (:require [application.news-scenarios :as news-scenarios]
            [domain.id :as id]
            [infrastructure.rest-api.admin-middleware :as admin-mw]
            [infrastructure.rest-api.auth-middleware :as auth-mw]))

(defn- serialize-news [n]
  (-> n
      (update :news/id str)))

;; ── Public ──────────────────────────────────────────────────────────────────

(defn- list-published-handler [news-repo]
  (fn [_request]
    {:status 200
     :body   (mapv serialize-news (news-scenarios/list-published-news news-repo))}))

;; ── Admin ───────────────────────────────────────────────────────────────────

(defn- list-all-handler [news-repo]
  (fn [_request]
    {:status 200
     :body   (mapv serialize-news (news-scenarios/list-news news-repo))}))

(defn- create-handler [news-repo]
  (fn [request]
    (try
      (let [{:keys [title content image-url]} (:body-params request)
            n (news-scenarios/create-news news-repo (id/build-id) title content image-url)]
        {:status 201
         :body   (serialize-news n)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- update-handler [news-repo]
  (fn [request]
    (try
      (let [nid (id/build-id (get-in request [:path-params :id]))
            {:keys [title content image-url published-at]} (:body-params request)
            n (news-scenarios/update-news news-repo nid title content image-url published-at)]
        {:status 200
         :body   (serialize-news n)})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

(defn- delete-handler [news-repo]
  (fn [request]
    (try
      (let [nid (id/build-id (get-in request [:path-params :id]))]
        (news-scenarios/delete-news news-repo nid)
        {:status 200
         :body   {:ok true}})
      (catch clojure.lang.ExceptionInfo e
        {:status 400
         :body   {:error (.getMessage e)}}))))

;; ── Routes ──────────────────────────────────────────────────────────────────

(defn routes [news-repo jwt-secret]
  [["/api/v1/news"
    {:get (list-published-handler news-repo)}]
   ["/api/v1/admin/news"
    {:get        (list-all-handler news-repo)
     :post       (create-handler news-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]
   ["/api/v1/admin/news/:id"
    {:put        (update-handler news-repo)
     :delete     (delete-handler news-repo)
     :middleware [[auth-mw/wrap-jwt-auth jwt-secret]
                  [admin-mw/wrap-admin-only]]}]])
