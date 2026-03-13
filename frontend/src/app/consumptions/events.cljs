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

(rf/reg-event-fx :consumptions/submit-step4
  (fn [{:keys [db]} [_ consumption-id contract-type]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/consumptions/" consumption-id "/step/contract-signature")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:contract-type (name contract-type)}
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
