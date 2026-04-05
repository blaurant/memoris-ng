(ns app.productions.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

;; ── Fetch productions ──────────────────────────────────────────────────────

(rf/reg-event-fx :productions/fetch
  (fn [{:keys [db]} _]
    {:db         (assoc db :productions/loading? true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/productions")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/fetch-ok]
                  :on-failure      [:productions/fetch-err]}}))

(rf/reg-event-db :productions/fetch-ok
  (fn [db [_ productions]]
    (-> db
        (assoc :productions/list productions)
        (assoc :productions/loading? false))))

(rf/reg-event-db :productions/fetch-err
  (fn [db _]
    (js/console.error "Failed to fetch productions")
    (assoc db :productions/loading? false)))

;; ── Create production ──────────────────────────────────────────────────────

(rf/reg-event-fx :productions/create
  (fn [{:keys [db]} _]
    {:http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/productions")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:id (str (random-uuid))}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Submit step 0 — producer information (address + network) ──────────────

(rf/reg-event-fx :productions/submit-step0
  (fn [{:keys [db]} [_ production-id producer-address network-opts]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/step/producer-information")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          (merge {:producer-address producer-address}
                                          network-opts)
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Submit step 1 — installation info ──────────────────────────────────────

(rf/reg-event-fx :productions/submit-step1
  (fn [{:keys [db]} [_ production-id pdl-prm installed-power energy-type linky-meter]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/step/installation-info")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:pdl-prm         pdl-prm
                                    :installed-power (js/parseFloat installed-power)
                                    :energy-type     (name energy-type)
                                    :linky-meter     linky-meter}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Submit step 2 — payment info ───────────────────────────────────────────

(rf/reg-event-fx :productions/submit-step2
  (fn [{:keys [db]} [_ production-id iban-holder iban bic payment-address]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/step/payment-info")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          (cond-> {:iban-holder iban-holder :iban iban :payment-address payment-address}
                                    (seq bic) (assoc :bic bic))
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Submit step 3 — contract signature ─────────────────────────────────────

(rf/reg-event-fx :productions/submit-step3
  (fn [{:keys [db]} [_ production-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/step/contract-signature")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Go back to previous step ───────────────────────────────────────────────

(rf/reg-event-fx :productions/go-back
  (fn [{:keys [db]} [_ production-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/go-back")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Abandon production ─────────────────────────────────────────────────────

(rf/reg-event-fx :productions/abandon
  (fn [{:keys [db]} [_ production-id]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/abandon")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/abandon-ok]
                  :on-failure      [:productions/fetch-err]}}))

(rf/reg-event-db :productions/abandon-ok
  (fn [db [_ abandoned]]
    (update db :productions/list
            (fn [items]
              (filterv #(not= (:production/id %) (:production/id abandoned)) items)))))

;; ── Delete production ─────────────────────────────────────────────────────

(rf/reg-event-fx :productions/delete
  (fn [{:keys [db]} [_ production-id]]
    {:http-xhrio {:method          :delete
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id)
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/delete-ok production-id]
                  :on-failure      [:productions/fetch-err]}}))

(rf/reg-event-db :productions/delete-ok
  (fn [db [_ production-id _response]]
    (update db :productions/list
            (fn [items]
              (filterv #(not= production-id (:production/id %)) items)))))

;; ── Update producer address ──────────────────────────────────────────────

(rf/reg-event-fx :productions/update-address
  (fn [{:keys [db]} [_ production-id new-address]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/update-address")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:producer-address new-address}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/update-address-ok production-id]
                  :on-failure      [:productions/fetch-err]}}))

(rf/reg-event-fx :productions/update-address-ok
  (fn [{:keys [db]} [_ production-id updated]]
    (let [items (:productions/list db)
          idx   (some (fn [[i p]] (when (= (:production/id p) (:production/id updated)) i))
                      (map-indexed vector items))]
      {:db       (cond-> db idx (assoc-in [:productions/list idx] updated))
       :dispatch [:productions/fetch-dashboard production-id]})))

;; ── Update PDL/PRM ───────────────────────────────────────────────────────

(rf/reg-event-fx :productions/update-pdl-prm
  (fn [{:keys [db]} [_ production-id new-pdl]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/update-pdl-prm")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:pdl-prm new-pdl}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Update Linky meter ───────────────────────────────────────────────────

(rf/reg-event-fx :productions/update-linky-meter
  (fn [{:keys [db]} [_ production-id new-linky]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/update-linky-meter")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:linky-meter new-linky}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/step-ok]
                  :on-failure      [:productions/fetch-err]}}))

;; ── Update IBAN ──────────────────────────────────────────────────────────

(rf/reg-event-fx :productions/update-iban
  (fn [{:keys [db]} [_ production-id new-iban on-success]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/update-iban")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:iban new-iban}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/update-iban-ok on-success]
                  :on-failure      [:productions/fetch-err]}}))

(rf/reg-event-db :productions/update-iban-ok
  (fn [db [_ on-success updated]]
    (when on-success (on-success))
    (let [items (:productions/list db)
          idx   (some (fn [[i p]] (when (= (:production/id p) (:production/id updated)) i))
                      (map-indexed vector items))]
      (cond-> db
        idx (assoc-in [:productions/list idx] updated)))))

;; ── Fetch production dashboard ────────────────────────────────────────────

(rf/reg-event-fx :productions/fetch-dashboard
  (fn [{:keys [db]} [_ production-id]]
    {:db         (assoc-in db [:productions/dashboard-loading production-id] true)
     :http-xhrio {:method          :get
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/dashboard")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:productions/fetch-dashboard-ok production-id]
                  :on-failure      [:productions/fetch-dashboard-err production-id]}}))

(rf/reg-event-db :productions/fetch-dashboard-ok
  (fn [db [_ production-id data]]
    (-> db
        (assoc-in [:productions/dashboards production-id] data)
        (assoc-in [:productions/dashboard-loading production-id] false))))

(rf/reg-event-db :productions/fetch-dashboard-err
  (fn [db [_ production-id]]
    (assoc-in db [:productions/dashboard-loading production-id] false)))

;; ── Step success — upsert production in the list ───────────────────────────

(rf/reg-event-db :productions/step-ok
  (fn [db [_ updated]]
    (let [items (:productions/list db)
          idx   (some (fn [[i p]] (when (= (:production/id p) (:production/id updated)) i))
                      (map-indexed vector items))]
      (assoc db :productions/list
             (if idx
               (assoc items idx updated)
               (conj items updated))))))
