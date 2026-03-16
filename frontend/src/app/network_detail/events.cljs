(ns app.network-detail.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

(rf/reg-event-fx :network-detail/fetch
  (fn [{:keys [db]} [_ network-id]]
    {:db         (-> db
                     (assoc :network-detail/loading? true)
                     (assoc :network-detail/error nil))
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/networks/" network-id "/detail")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:network-detail/fetch-ok]
                  :on-failure      [:network-detail/fetch-err]}}))

(rf/reg-event-db :network-detail/fetch-ok
  (fn [db [_ response]]
    (-> db
        (assoc :network-detail/data response)
        (assoc :network-detail/loading? false))))

(rf/reg-event-db :network-detail/fetch-err
  (fn [db [_ error]]
    (-> db
        (assoc :network-detail/error error)
        (assoc :network-detail/loading? false))))
