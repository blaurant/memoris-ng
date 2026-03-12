(ns infrastructure.email.console-sender
  (:require [domain.email-sender :as email-sender]
            [integrant.core :as ig]))

(defrecord ConsoleSender [app-base-url]
  email-sender/EmailSender

  (send-verification-email! [_ email token]
    (let [verification-url (str app-base-url "/verify-email?token=" token)]
      (println (str "\n══════════════════════════════════════════════"
                    "\n  VERIFICATION EMAIL (dev console)"
                    "\n  To:    " email
                    "\n  Link:  " verification-url
                    "\n══════════════════════════════════════════════\n")))))

(defmethod ig/init-key :email/console-sender [_ {:keys [app-base-url]}]
  (->ConsoleSender app-base-url))

(defmethod ig/halt-key! :email/console-sender [_ _] nil)
