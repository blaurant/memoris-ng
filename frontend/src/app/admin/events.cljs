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

;; ── Create network ────────────────────────────────────────────────────────────

(rf/reg-event-fx :admin/create-network
  (fn [{:keys [db]} [_ params]]
    {:http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/admin/networks")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          params
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/create-network-ok]
                  :on-failure      [:admin/create-network-err]}}))

(rf/reg-event-db :admin/create-network-ok
  (fn [db [_ network]]
    (update db :admin/networks conj network)))

(rf/reg-event-db :admin/create-network-err
  (fn [db _]
    (js/console.error "Failed to create network")
    db))

;; ── Toggle network visibility ────────────────────────────────────────────────

(rf/reg-event-fx :admin/toggle-network-visibility
  (fn [{:keys [db]} [_ network-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/admin/networks/" network-id "/toggle-visibility")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/toggle-network-visibility-ok]
                  :on-failure      [:admin/toggle-network-visibility-err]}}))

(rf/reg-event-db :admin/toggle-network-visibility-ok
  (fn [db [_ updated-network]]
    (let [nid (:network/id updated-network)]
      (update db :admin/networks
              (fn [networks]
                (mapv #(if (= nid (:network/id %)) updated-network %) networks))))))

(rf/reg-event-db :admin/toggle-network-visibility-err
  (fn [db _]
    (js/console.error "Failed to toggle network visibility")
    db))

;; ── Fetch eligibility checks ─────────────────────────────────────────────────

(rf/reg-event-fx :admin/fetch-eligibility-checks
  (fn [{:keys [db]} _]
    {:db         (assoc db :admin/eligibility-checks-loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/admin/eligibility-checks")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/fetch-eligibility-checks-ok]
                  :on-failure      [:admin/fetch-eligibility-checks-err]}}))

(rf/reg-event-db :admin/fetch-eligibility-checks-ok
  (fn [db [_ checks]]
    (-> db
        (assoc :admin/eligibility-checks checks)
        (assoc :admin/eligibility-checks-loading? false))))

(rf/reg-event-db :admin/fetch-eligibility-checks-err
  (fn [db _]
    (js/console.error "Failed to fetch eligibility checks")
    (assoc db :admin/eligibility-checks-loading? false)))

;; ── Alert banner admin ──────────────────────────────────────────────────────

(rf/reg-event-fx :admin/fetch-alert
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/admin/alert")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/fetch-alert-ok]
                  :on-failure      [:admin/fetch-alert-err]}}))

(rf/reg-event-db :admin/fetch-alert-ok
  (fn [db [_ {:keys [message active]}]]
    (-> db
        (assoc :admin/alert-message message)
        (assoc :admin/alert-active? active))))

(rf/reg-event-db :admin/fetch-alert-err
  (fn [db _]
    (js/console.error "Failed to fetch alert")
    db))

(rf/reg-event-fx :admin/toggle-alert
  (fn [{:keys [db]} [_ active?]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/admin/alert")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:active active? :message (or (:admin/alert-message db) "")}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/update-alert-ok]
                  :on-failure      [:admin/update-alert-err]}}))

(rf/reg-event-fx :admin/update-alert
  (fn [{:keys [db]} [_ params]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/admin/alert")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          params
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/update-alert-ok]
                  :on-failure      [:admin/update-alert-err]}}))

(rf/reg-event-db :admin/update-alert-ok
  (fn [db [_ {:keys [message active]}]]
    (-> db
        (assoc :admin/alert-message message)
        (assoc :admin/alert-active? active)
        (assoc :alert/message message)
        (assoc :alert/active? active))))

(rf/reg-event-db :admin/update-alert-err
  (fn [db _]
    (js/console.error "Failed to update alert")
    db))

;; ── Fetch productions (admin) ────────────────────────────────────────────────

(rf/reg-event-fx :admin/fetch-productions
  (fn [{:keys [db]} _]
    {:db         (assoc db :admin/productions-loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/admin/productions")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:admin/fetch-productions-ok]
                  :on-failure      [:admin/fetch-productions-err]}}))

(rf/reg-event-db :admin/fetch-productions-ok
  (fn [db [_ productions]]
    (-> db
        (assoc :admin/productions productions)
        (assoc :admin/productions-loading? false))))

(rf/reg-event-db :admin/fetch-productions-err
  (fn [db _]
    (js/console.error "Failed to fetch admin productions")
    (assoc db :admin/productions-loading? false)))

;; ── Tab switch ────────────────────────────────────────────────────────────────

(rf/reg-event-db :admin/set-tab
  (fn [db [_ tab]]
    (assoc db :admin/active-tab tab)))
