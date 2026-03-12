(ns domain.verification-token-repo)

(defprotocol VerificationTokenRepo
  (save! [repo token] "Persist a verification token.")
  (find-by-token [repo token-string] "Find a verification token by its token string.")
  (delete! [repo id] "Delete a verification token by id."))
