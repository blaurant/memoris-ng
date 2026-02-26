(ns app.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [app.db :as db]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfee]))

(rf/reg-event-db :app/initialize
  (fn [_ _]
    db/default-db))

;; ── Hello (example) ──────────────────────────────────────────────────────────

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

;; ── Router ───────────────────────────────────────────────────────────────────

(rf/reg-event-db :router/navigated
  (fn [db [_ page-name]]
    (assoc db :router/current-page page-name)))

(rf/reg-event-fx :router/navigate
  (fn [_ [_ page-name]]
    {:navigate page-name}))

(rf/reg-fx :navigate
  (fn [page-name]
    (rfee/push-state page-name)))

;; ── Networks ──────────────────────────────────────────────────────────────────

(rf/reg-event-fx :networks/fetch
  (fn [{:keys [db]} _]
    {:db         (assoc db :networks/loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/networks")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:networks/fetch-ok]
                  :on-failure      [:networks/fetch-err]}}))

(rf/reg-event-db :networks/fetch-ok
  (fn [db [_ networks]]
    (-> db
        (assoc :networks/list networks)
        (assoc :networks/loading? false))))

(rf/reg-event-db :networks/fetch-err
  (fn [db _]
    (js/console.error "Failed to fetch networks")
    (assoc db :networks/loading? false)))

;; ── Eligibility ───────────────────────────────────────────────────────────────

(rf/reg-event-fx :eligibility/check
  (fn [{:keys [db]} [_ {:keys [lat lng address]}]]
    {:db         (-> db
                     (assoc :eligibility/loading? true)
                     (assoc :eligibility/lat lat)
                     (assoc :eligibility/lng lng))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/networks/check-eligibility")
                  :params          {:lat lat :lng lng}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:eligibility/check-ok address]
                  :on-failure      [:eligibility/check-err]}}))

(rf/reg-event-db :eligibility/check-ok
  (fn [db [_ address result]]
    (-> db
        (assoc :eligibility/result result)
        (assoc :eligibility/address address)
        (assoc :eligibility/loading? false))))

(rf/reg-event-db :eligibility/check-err
  (fn [db _]
    (js/console.error "Failed to check eligibility")
    (assoc db :eligibility/loading? false)))
