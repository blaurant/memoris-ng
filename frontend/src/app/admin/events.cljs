(ns app.admin.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

;; ── Fetch users ───────────────────────────────────────────────────────────────

(rf/reg-event-fx :admin/fetch-users
  (fn [{:keys [db]} _]
    {:db         (assoc db :admin/users-loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/admin/users")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/fetch-users-ok]
                  :on-failure      [:admin/fetch-users-err]}}))

(rf/reg-event-db :admin/fetch-users-ok
  (fn [db [_ users]]
    (-> db
        (assoc :admin/users users)
        (assoc :admin/users-loading? false))))

(rf/reg-event-db :admin/fetch-users-err
  (fn [db _]
    (js/console.error "Failed to fetch admin users")
    (assoc db :admin/users-loading? false)))

;; ── Fetch networks ────────────────────────────────────────────────────────────

(rf/reg-event-fx :admin/fetch-networks
  (fn [{:keys [db]} _]
    {:db         (assoc db :admin/networks-loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/admin/networks")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/fetch-networks-ok]
                  :on-failure      [:admin/fetch-networks-err]}}))

(rf/reg-event-db :admin/fetch-networks-ok
  (fn [db [_ networks]]
    (-> db
        (assoc :admin/networks networks)
        (assoc :admin/networks-loading? false))))

(rf/reg-event-db :admin/fetch-networks-err
  (fn [db _]
    (js/console.error "Failed to fetch admin networks")
    (assoc db :admin/networks-loading? false)))

;; ── Tab switch ────────────────────────────────────────────────────────────────

(rf/reg-event-db :admin/set-tab
  (fn [db [_ tab]]
    (assoc db :admin/active-tab tab)))
