(ns app.consumptions.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

;; ── Fetch consumptions ──────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/fetch
  (fn [{:keys [db]} _]
    {:db         (assoc db :consumptions/loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/consumptions")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/fetch-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

(rf/reg-event-db :consumptions/fetch-ok
  (fn [db [_ consumptions]]
    (-> db
        (assoc :consumptions/list consumptions)
        (assoc :consumptions/loading? false))))

(rf/reg-event-db :consumptions/fetch-err
  (fn [db _]
    (js/console.error "Failed to fetch consumptions")
    (assoc db :consumptions/loading? false)))

;; ── Fetch consumption dashboard ───────────────────────────────────────────

(rf/reg-event-fx :consumptions/fetch-dashboard
  (fn [{:keys [db]} [_ consumption-id]]
    {:db         (assoc-in db [:consumptions/dashboard-loading consumption-id] true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/dashboard")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/fetch-dashboard-ok consumption-id]
                  :on-failure      [:consumptions/fetch-dashboard-err consumption-id]}}))

(rf/reg-event-db :consumptions/fetch-dashboard-ok
  (fn [db [_ consumption-id data]]
    (-> db
        (assoc-in [:consumptions/dashboards consumption-id] data)
        (assoc-in [:consumptions/dashboard-loading consumption-id] false))))

(rf/reg-event-db :consumptions/fetch-dashboard-err
  (fn [db [_ consumption-id]]
    (assoc-in db [:consumptions/dashboard-loading consumption-id] false)))

;; ── Update consumer address ─────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/update-address
  (fn [{:keys [db]} [_ consumption-id new-address]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/update-address")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:consumer-address new-address}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

;; ── Update billing address ──────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/update-billing-address
  (fn [{:keys [db]} [_ consumption-id new-address]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/update-billing-address")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:billing-address new-address}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))


;; ── Create consumption ──────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/create
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/consumptions")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:id (str (random-uuid))}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

;; ── Submit step 1 ───────────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/submit-step1
  (fn [{:keys [db]} [_ consumption-id address network-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/step/consumer-information")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:address address :network-id network-id}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

;; ── Submit step 2 ───────────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/submit-step2
  (fn [{:keys [db]} [_ consumption-id linky-reference]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/step/linky-reference")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:linky-reference linky-reference}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

;; ── Submit step 3 ───────────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/submit-step3
  (fn [{:keys [db]} [_ consumption-id billing-address]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/step/billing-address")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:billing-address billing-address}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

;; ── Submit step 4 ───────────────────────────────────────────────────────────

;; Initiate contract signing via DocuSeal (returns signing-url)
(rf/reg-event-fx :consumptions/submit-step4
  (fn [{:keys [db]} [_ consumption-id contract-type]]
    {:db (assoc db :consumptions/contract-loading? contract-type)
     :http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/step/contract-signature")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:contract-type (name contract-type)}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/contract-signing-url-ok contract-type]
                  :on-failure      [:consumptions/contract-signing-err]}}))

(rf/reg-event-db :consumptions/contract-signing-url-ok
  (fn [db [_ contract-type response]]
    (let [url (:signing-url response)]
      (when url (.open js/window url "_blank"))
      (-> db
          (dissoc :consumptions/contract-loading?)
          (assoc :consumptions/contract-signing-url url)
          (assoc :consumptions/contract-signing-type contract-type)))))

(rf/reg-event-db :consumptions/contract-signing-err
  (fn [db _]
    (js/console.error "Failed to initiate contract signing")
    (dissoc db :consumptions/contract-loading?)))

(rf/reg-event-fx :consumptions/contract-signing-complete
  (fn [{:keys [db]} [_ consumption-id contract-type]]
    {:db (-> db
             (dissoc :consumptions/contract-signing-url)
             (dissoc :consumptions/contract-signing-type))
     :dispatch [:consumptions/check-contract-status consumption-id contract-type]}))

(rf/reg-event-fx :consumptions/check-contract-status
  (fn [{:keys [db]} [_ consumption-id contract-type]]
    {:http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/check-contract?contract-type=" (name contract-type))
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/check-contract-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

(rf/reg-event-fx :consumptions/check-contract-ok
  (fn [_ [_ response]]
    (when (:signed response)
      {:dispatch [:consumptions/fetch]})))

(rf/reg-event-fx :consumptions/download-contract
  (fn [{:keys [db]} [_ consumption-id contract-type]]
    {:http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/contract-document?contract-type=" (name contract-type))
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/download-contract-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

(rf/reg-event-db :consumptions/download-contract-ok
  (fn [db [_ response]]
    (when-let [url (:document-url response)]
      (.open js/window url "_blank"))
    db))

;; ── Go back to previous step ───────────────────────────────────────────────

(rf/reg-event-fx :consumptions/go-back
  (fn [{:keys [db]} [_ consumption-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/go-back")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/step-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

;; ── Abandon consumption ────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/abandon
  (fn [{:keys [db]} [_ consumption-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/abandon")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/abandon-ok]
                  :on-failure      [:consumptions/fetch-err]}}))

(rf/reg-event-db :consumptions/abandon-ok
  (fn [db [_ abandoned]]
    (update db :consumptions/list
            (fn [items]
              (filterv #(not= (:consumption/id %) (:consumption/id abandoned)) items)))))

;; ── Delete consumption ─────────────────────────────────────────────────────

(rf/reg-event-fx :consumptions/delete
  (fn [{:keys [db]} [_ consumption-id]]
    {:http-xhrio {:method          :delete
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id)
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:consumptions/delete-ok consumption-id]
                  :on-failure      [:consumptions/fetch-err]}}))

(rf/reg-event-db :consumptions/delete-ok
  (fn [db [_ consumption-id _response]]
    (update db :consumptions/list
            (fn [items]
              (filterv #(not= consumption-id (:consumption/id %)) items)))))

;; ── Step success — upsert consumption in the list ───────────────────────────

(rf/reg-event-db :consumptions/step-ok
  (fn [db [_ updated]]
    (let [id    (keyword (:consumption/id updated))
          items (:consumptions/list db)
          idx   (some (fn [[i c]] (when (= (:consumption/id c) (:consumption/id updated)) i))
                      (map-indexed vector items))]
      (assoc db :consumptions/list
             (if idx
               (assoc items idx updated)
               (conj items updated))))))
