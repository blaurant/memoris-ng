(ns domain.email-sender)

(defprotocol EmailSender
  (send-verification-email! [sender email token] "Send a verification email with the given token.")
  (send-welcome-email! [sender email name] "Send a welcome email after account activation."))
