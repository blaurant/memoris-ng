(ns app.auth.events
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [re-frame.core :as rf]))

;; ── Login with provider ──────────────────────────────────────────────────────

(rf/reg-event-fx :auth/login-with-provider
  (fn [{:keys [db]} [_ provider id-token]]
    {:db         (-> db
                     (assoc :auth/loading? true)
                     (dissoc :auth/error))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/login")
                  :params          {:provider (name provider) :id-token id-token}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/login-ok]
                  :on-failure      [:auth/login-err]}}))

(rf/reg-event-fx :auth/login-ok
  (fn [{:keys [db]} [_ response]]
    (let [token (:token response)
          user  (:user response)]
      (.setItem js/localStorage "auth-token" token)
      (.setItem js/localStorage "auth-user" (js/JSON.stringify (clj->js user)))
      {:db       (-> db
                     (assoc :auth/token token)
                     (assoc :auth/user user)
                     (assoc :auth/loading? false)
                     (dissoc :auth/error))
       :navigate :page/portal})))

(rf/reg-event-db :auth/login-err
  (fn [db [_ response]]
    (-> db
        (assoc :auth/loading? false)
        (assoc :auth/error (or (get-in response [:response :error])
                               "Erreur de connexion")))))

;; ── Logout ───────────────────────────────────────────────────────────────────

(rf/reg-event-fx :auth/logout
  (fn [{:keys [db]} _]
    (.removeItem js/localStorage "auth-token")
    (.removeItem js/localStorage "auth-user")
    {:db       (-> db
                   (dissoc :auth/token)
                   (dissoc :auth/user)
                   (assoc :auth/loading? false)
                   (dissoc :auth/error))
     :navigate :page/home}))

;; ── Restore session ──────────────────────────────────────────────────────────

(rf/reg-event-db :auth/restore-session
  (fn [db _]
    (let [token (.getItem js/localStorage "auth-token")
          user-json (.getItem js/localStorage "auth-user")
          user  (when user-json
                  (js->clj (js/JSON.parse user-json) :keywordize-keys true))]
      (if (and token user)
        (-> db
            (assoc :auth/token token)
            (assoc :auth/user user))
        db))))
