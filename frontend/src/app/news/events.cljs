(ns app.news.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

(rf/reg-event-fx :news/fetch
  (fn [{:keys [db]} _]
    {:db         (assoc db :news/loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/news")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:news/fetch-ok]
                  :on-failure      [:news/fetch-err]}}))

(rf/reg-event-db :news/fetch-ok
  (fn [db [_ news-list]]
    (-> db
        (assoc :news/list news-list)
        (assoc :news/loading? false))))

(rf/reg-event-db :news/fetch-err
  (fn [db _]
    (js/console.error "Failed to fetch news")
    (assoc db :news/loading? false)))
