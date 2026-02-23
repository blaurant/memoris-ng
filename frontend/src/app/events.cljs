(ns app.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [app.db :as db]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]))

(rf/reg-event-db :app/initialize
  (fn [_ _]
    db/default-db))

(rf/reg-event-fx :hello/fetch
  (fn [{:keys [db]} _]
    {:db         (assoc db :hello/loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/hello")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:hello/fetch-ok]
                  :on-failure      [:hello/fetch-err]}}))

(rf/reg-event-db :hello/fetch-ok
  (fn [db [_ response]]
    (-> db
        (assoc :hello/message (:message response))
        (assoc :hello/loading? false))))

(rf/reg-event-db :hello/fetch-err
  (fn [db _]
    (-> db
        (assoc :hello/message "Error: could not reach the server.")
        (assoc :hello/loading? false))))
