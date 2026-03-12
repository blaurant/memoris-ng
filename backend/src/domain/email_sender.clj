(ns domain.email-sender)

(defprotocol EmailSender
  (send-verification-email! [sender email token] "Send a verification email with the given token."))
