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
  (fn [{:keys [db]} [_ production-id iban]]
    {:http-xhrio {:method          :put
                  :uri             (str config/API_BASE "/api/v1/productions/" production-id "/step/payment-info")
                  :headers         {"Authorization" (str "Bearer " (:auth/token db))}
                  :params          {:iban iban}
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
