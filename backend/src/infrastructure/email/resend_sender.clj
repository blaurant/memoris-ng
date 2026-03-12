(ns infrastructure.email.resend-sender
  (:require [clj-http.client :as http]
            [clojure.data.json :as json]
            [com.brunobonacci.mulog :as mu]
            [domain.email-sender :as email-sender]
            [integrant.core :as ig]))

(defrecord ResendSender [api-key from-email app-base-url]
  email-sender/EmailSender

  (send-verification-email! [_ email token]
    (let [verification-url (str app-base-url "/verify-email?token=" token)
          body {:from    from-email
                :to      email
                :subject "ProxyWatt — Vérifiez votre adresse email"
                :html    (str "<h2>Bienvenue sur ProxyWatt !</h2>"
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
                          {:to email :cause (.getMessage e)})))))))

(defmethod ig/init-key :email/resend-sender [_ {:keys [api-key from-email app-base-url]}]
  (->ResendSender api-key from-email app-base-url))

(defmethod ig/halt-key! :email/resend-sender [_ _] nil)
