(ns application.contact-scenarios
  (:require [clojure.string :as str]
            [com.brunobonacci.mulog :as mu]
            [domain.email-sender :as email-sender]
            [domain.user :as user]))

(defn send-contact-message
  "Send a contact form message to all admin users."
  [user-repo email-sender {:keys [name email subject message]}]
  (let [all-users    (user/find-all user-repo)
        admin-emails (keep (fn [u] (when (= :admin (:user/role u)) (:user/email u))) all-users)]
    (when (seq admin-emails)
      (email-sender/send-admin-notification!
        email-sender
        admin-emails
        (str "Message de contact : " subject)
        (str "<h2>Nouveau message de contact</h2>"
             "<p><strong>De :</strong> " name " (&lt;" email "&gt;)</p>"
             "<p><strong>Sujet :</strong> " subject "</p>"
             "<hr>"
             "<p>" (str/replace message #"\n" "<br>") "</p>")))
    (mu/log ::contact-message-sent :from email :subject subject)))
