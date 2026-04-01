(ns application.news-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.news :as news])
  (:import (java.time Instant)))

(defn list-news
  "Returns all news articles sorted by created-at desc."
  [news-repo]
  (->> (news/find-all news-repo)
       (sort-by :news/created-at #(compare %2 %1))
       vec))

(defn list-published-news
  "Returns published news sorted by published-at desc."
  [news-repo]
  (->> (news/find-all news-repo)
       (filter :news/published-at)
       (sort-by :news/published-at #(compare %2 %1))
       vec))

(defn create-news
  "Create a new news article."
  [news-repo id title content image-url]
  (let [now (str (Instant/now))
        n   (news/build-news {:news/id           id
                              :news/title        title
                              :news/content      content
                              :news/image-url    image-url
                              :news/published-at now
                              :news/created-at   now})
        n   (news/save! news-repo n)]
    (mu/log ::news-created :news-id id)
    n))

(defn update-news
  "Update an existing news article."
  [news-repo id title content image-url published-at]
  (let [n (news/find-by-id news-repo id)]
    (when-not n
      (throw (ex-info "News not found" {:news-id id})))
    (let [n' (news/build-news (assoc n
                                     :news/title        title
                                     :news/content      content
                                     :news/image-url    image-url
                                     :news/published-at published-at))
          n' (news/save! news-repo n n')]
      (mu/log ::news-updated :news-id id)
      n')))

(defn delete-news
  "Delete a news article."
  [news-repo id]
  (news/delete! news-repo id)
  (mu/log ::news-deleted :news-id id)
  nil)
