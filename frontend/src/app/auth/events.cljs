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

;; ── Login with email ────────────────────────────────────────────────────────

(rf/reg-event-fx :auth/login-with-email
  (fn [{:keys [db]} [_ email password]]
    {:db         (-> db
                     (assoc :auth/loading? true)
                     (dissoc :auth/error))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/login")
                  :params          {:provider "email" :email email :password password}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/login-ok]
                  :on-failure      [:auth/login-err]}}))

;; ── Register with email ─────────────────────────────────────────────────────

(rf/reg-event-fx :auth/register
  (fn [{:keys [db]} [_ name email password]]
    {:db         (-> db
                     (assoc :auth/loading? true)
                     (dissoc :auth/error)
                     (dissoc :auth/register-success?)
                     (assoc :auth/register-email email))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/register")
                  :params          {:id (str (random-uuid)) :email email :name name :password password}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/register-ok]
                  :on-failure      [:auth/register-err]}}))

(rf/reg-event-fx :auth/register-ok
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc :auth/loading? false)
                   (assoc :auth/register-success? true))
     :navigate :page/check-email}))

(rf/reg-event-db :auth/register-err
  (fn [db [_ response]]
    (-> db
        (assoc :auth/loading? false)
        (assoc :auth/error (or (get-in response [:response :error])
                               "Erreur lors de l'inscription")))))

;; ── Verify email ────────────────────────────────────────────────────────────

(rf/reg-event-fx :auth/verify-email
  (fn [{:keys [db]} [_ token]]
    {:db         (-> db
                     (assoc :auth/verification-status :loading)
                     (dissoc :auth/error))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/verify-email")
                  :params          {:token token}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/verify-email-ok]
                  :on-failure      [:auth/verify-email-err]}}))

(rf/reg-event-db :auth/verify-email-ok
  (fn [db _]
    (assoc db :auth/verification-status :success)))

(rf/reg-event-db :auth/verify-email-err
  (fn [db [_ response]]
    (-> db
        (assoc :auth/verification-status :error)
        (assoc :auth/error (or (get-in response [:response :error])
                               "Erreur de vérification")))))

;; ── Resend verification ─────────────────────────────────────────────────────

(rf/reg-event-fx :auth/resend-verification
  (fn [{:keys [db]} [_ email]]
    {:http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/resend-verification")
                  :params          {:email email}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/resend-ok]
                  :on-failure      [:auth/resend-err]}}))

(rf/reg-event-db :auth/resend-ok
  (fn [db _]
    (assoc db :auth/resend-success? true)))

(rf/reg-event-db :auth/resend-err
  (fn [db _]
    (assoc db :auth/error "Erreur lors du renvoi de l'email")))

;; ── Forgot password ─────────────────────────────────────────────────────────

(rf/reg-event-db :auth/set-forgot-email
  (fn [db [_ email]]
    (-> db
        (assoc :auth/forgot-email email)
        (dissoc :auth/forgot-password-sent?))))


(rf/reg-event-fx :auth/forgot-password
  (fn [{:keys [db]} [_ email]]
    {:db         (-> db
                     (assoc :auth/loading? true)
                     (dissoc :auth/error))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/forgot-password")
                  :params          {:email email}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/forgot-password-ok]
                  :on-failure      [:auth/forgot-password-ok]}}))

(rf/reg-event-fx :auth/forgot-password-ok
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc :auth/loading? false)
                   (assoc :auth/forgot-password-sent? true))
     :dispatch-later [{:ms 3000 :dispatch [:router/navigate :page/login]}]}))

;; ── Reset password ──────────────────────────────────────────────────────────

(rf/reg-event-fx :auth/reset-password
  (fn [{:keys [db]} [_ token password]]
    {:db         (-> db
                     (assoc :auth/loading? true)
                     (dissoc :auth/error))
     :http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/auth/reset-password")
                  :params          {:token token :password password}
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:auth/reset-password-ok]
                  :on-failure      [:auth/reset-password-err]}}))

(rf/reg-event-fx :auth/reset-password-ok
  (fn [{:keys [db]} _]
    {:db       (-> db
                   (assoc :auth/loading? false)
                   (assoc :auth/reset-password-success? true))
     :dispatch-later [{:ms 3000 :dispatch [:router/navigate :page/login]}]}))

(rf/reg-event-db :auth/reset-password-err
  (fn [db [_ response]]
    (-> db
        (assoc :auth/loading? false)
        (assoc :auth/error (or (get-in response [:response :error])
                               "Lien invalide ou expiré")))))

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
