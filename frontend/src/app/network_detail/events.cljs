(ns app.network-detail.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

(rf/reg-event-fx :network-detail/fetch
  (fn [{:keys [db]} [_ network-id]]
    (let [admin?  (= "admin" (get-in db [:auth/user :role]))
          token   (:auth/token db)
          uri     (if admin?
                    (str config/API_BASE "/api/v1/admin/networks/" network-id "/detail")
                    (str config/API_BASE "/api/v1/networks/" network-id "/detail"))
          headers (if (and admin? token)
                    {"Authorization" (str "Bearer " token)}
                    {})]
      {:db         (-> db
                       (assoc :network-detail/loading? true)
                       (assoc :network-detail/error nil))
       :http-xhrio {:method          :get
                    :uri             uri
                    :headers         headers
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:network-detail/fetch-ok]
                    :on-failure      [:network-detail/fetch-err]}})))

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

(rf/reg-event-fx :network-detail/toggle-visibility
  (fn [{:keys [db]} [_ network-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/admin/networks/" network-id "/toggle-visibility")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:network-detail/toggle-visibility-ok network-id]
                  :on-failure      [:network-detail/toggle-visibility-err]}}))

(rf/reg-event-fx :network-detail/toggle-visibility-ok
  (fn [_ [_ network-id _response]]
    {:dispatch [:network-detail/fetch network-id]}))

(rf/reg-event-db :network-detail/toggle-visibility-err
  (fn [db _]
    (js/console.error "Failed to toggle network visibility")
    db))
