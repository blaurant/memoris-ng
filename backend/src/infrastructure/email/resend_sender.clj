(ns infrastructure.email.resend-sender
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [com.brunobonacci.mulog :as mu]
            [domain.email-sender :as email-sender]
            [integrant.core :as ig]))

(defrecord ResendSender [api-key from-email noreply-email app-base-url]
  email-sender/EmailSender

  (send-verification-email! [_ email token]
    (let [verification-url (str app-base-url "/verify-email?token=" token)
          body {:from    from-email
                :to      email
                :subject "Elink-co — Vérifiez votre adresse email"
                :html    (str "<h2>Bienvenue sur Elink-co !</h2>"
                              "<p>Cliquez sur le lien ci-dessous pour vérifier votre adresse email :</p>"
                              "<p><a href=\"" verification-url "\">Vérifier mon email</a></p>"
                              "<p>Ce lien expire dans 24 heures.</p>"
                              "<p>Si vous n'avez pas créé de compte, ignorez cet email.</p>")}]
      (try
        (http/post "https://api.resend.com/emails"
                   {:headers      {"Authorization" (str "Bearer " api-key)
                                   "Content-Type"  "application/json"}
                    :body         (json/write-str body)
                    :as           :json
                    :content-type :json})
        (mu/log ::email-sent :to email)
        (catch clojure.lang.ExceptionInfo e
          (let [data    (ex-data e)
                status  (:status data)
                resp    (:body data)]
            (mu/log ::email-send-failed
                    :to email
                    :status status
                    :response-body resp
                    :error (.getMessage e)))
          (throw (ex-info "Failed to send verification email"
                          {:to email :cause (.getMessage e)}))))))

  (send-password-reset-email! [_ email token]
    (let [reset-url (str app-base-url "/reset-password?token=" token)
          body {:from    from-email
                :to      email
                :subject "Elink-co — Réinitialisation de votre mot de passe"
                :html    (str "<h2>Réinitialisation de mot de passe</h2>"
                              "<p>Vous avez demandé la réinitialisation de votre mot de passe.</p>"
                              "<p><a href=\"" reset-url "\">Cliquez ici pour choisir un nouveau mot de passe</a></p>"
                              "<p>Ce lien expire dans 24 heures.</p>"
                              "<p>Si vous n'avez pas fait cette demande, ignorez cet email.</p>")}]
      (try
        (http/post "https://api.resend.com/emails"
                   {:headers      {"Authorization" (str "Bearer " api-key)
                                   "Content-Type"  "application/json"}
                    :body         (json/write-str body)
                    :as           :json
                    :content-type :json})
        (mu/log ::password-reset-email-sent :to email)
        (catch clojure.lang.ExceptionInfo e
          (mu/log ::password-reset-email-send-failed :to email :error (.getMessage e))))))

  (send-welcome-email! [_ email name]
    (let [body {:from    from-email
                :to      email
                :subject "Bienvenue sur Elink-co !"
                :html    (str "<h2>Bienvenue " name " !</h2>"
                              "<p>Votre compte Elink-co est maintenant actif.</p>"
                              "<p>Vous pouvez dès à présent vous connecter et rejoindre "
                              "un réseau d'énergie locale partagée près de chez vous.</p>"
                              "<p><a href=\"" app-base-url "/login\">Accéder à mon espace</a></p>"
                              "<p>À bientôt,<br>L'équipe Elink-co</p>")}]
      (try
        (http/post "https://api.resend.com/emails"
                   {:headers      {"Authorization" (str "Bearer " api-key)
                                   "Content-Type"  "application/json"}
                    :body         (json/write-str body)
                    :as           :json
                    :content-type :json})
        (mu/log ::welcome-email-sent :to email)
        (catch clojure.lang.ExceptionInfo e
          (mu/log ::welcome-email-send-failed :to email :error (.getMessage e))))))

  (send-admin-notification! [_ admin-emails subject html-body]
    (doseq [email admin-emails]
      (let [body {:from    (or noreply-email from-email)
                  :to      email
                  :subject (str "Elink-co — " subject)
                  :html    html-body}]
        (try
          (http/post "https://api.resend.com/emails"
                     {:headers      {"Authorization" (str "Bearer " api-key)
                                     "Content-Type"  "application/json"}
                      :body         (json/write-str body)
                      :as           :json
                      :content-type :json})
          (mu/log ::admin-notification-sent :to email :subject subject)
          (catch clojure.lang.ExceptionInfo e
            (mu/log ::admin-notification-failed :to email :error (.getMessage e))))))))

(defmethod ig/init-key :email/resend-sender [_ {:keys [api-key from-email noreply-email app-base-url]}]
  (->ResendSender api-key from-email noreply-email app-base-url))

(defmethod ig/halt-key! :email/resend-sender [_ _] nil)
