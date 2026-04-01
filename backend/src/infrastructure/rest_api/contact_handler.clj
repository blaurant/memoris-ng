(ns infrastructure.rest-api.contact-handler
  (:require [application.contact-scenarios :as contact]))

(defn- send-contact-handler [user-repo email-sender]
  (fn [request]
    (let [{:keys [name email subject message]} (:body-params request)]
      (if (or (clojure.string/blank? name)
              (clojure.string/blank? email)
              (clojure.string/blank? subject)
              (clojure.string/blank? message))
        {:status 400 :body {:error "Tous les champs sont requis."}}
        (do
          (contact/send-contact-message user-repo email-sender
                                        {:name name :email email :subject subject :message message})
          {:status 200 :body {:message "Message envoyé avec succès."}})))))

(defn routes [user-repo email-sender]
  [["/api/v1/contact"
    {:post (send-contact-handler user-repo email-sender)}]])
