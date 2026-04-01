(ns domain.news
  (:require [clojure.string]
            [domain.id :as id]
            [malli.core :as m]))

(def News
  [:map
   [:news/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:news/title [:and string? [:fn {:error/message "must not be blank"} #(not (clojure.string/blank? %))]]]
   [:news/content string?]
   [:news/image-url {:optional true} [:maybe string?]]
   [:news/published-at {:optional true} [:maybe string?]]
   [:news/created-at string?]])

(defn build-news [attrs]
  (if (m/validate News attrs)
    attrs
    (throw (ex-info "Invalid news" {:attrs attrs :errors (m/explain News attrs)}))))

(defprotocol NewsRepo
  (find-all [repo]            "Returns all news articles.")
  (find-by-id [repo id]      "Find a news article by ID.")
  (save! [repo news]
         [repo original updated] "Persist a news article.")
  (delete! [repo id]          "Delete a news article by ID."))
