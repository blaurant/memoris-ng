(ns domain.verification-token
  (:require [domain.id :as id]
            [malli.core :as m])
  (:import (java.time Instant)))

(def VerificationToken
  [:map
   [:verification-token/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:verification-token/user-id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:verification-token/token string?]
   [:verification-token/expires-at inst?]])

(defn build-verification-token
  "Validates and returns a verification token map."
  [attrs]
  (if (m/validate VerificationToken attrs)
    attrs
    (throw (ex-info "Invalid verification token" {:attrs attrs
                                                  :errors (m/explain VerificationToken attrs)}))))

(defn make-1d-token
      "Create a new verification token for a user, expiring in 24 hours."
      [id user-id token-string]
      (build-verification-token
        {:verification-token/id         id
         :verification-token/user-id    user-id
         :verification-token/token      token-string
         :verification-token/expires-at (java.util.Date.
                                          (+ (System/currentTimeMillis)
                                             (* 24 60 60 1000)))}))

(defn expired?
  "Returns true if the token has expired relative to now."
  [vt ^Instant now]
  (let [expires-at (:verification-token/expires-at vt)
        exp-instant (if (instance? Instant expires-at)
                      expires-at
                      (.toInstant ^java.util.Date expires-at))]
    (.isAfter now exp-instant)))

;; ── Repository protocol ───────────────────────────────────────────────────

(defprotocol VerificationTokenRepo
  (save! [repo token] "Persist a verification token.")
  (find-by-token [repo token-string] "Find a verification token by its token string.")
  (delete! [repo id] "Delete a verification token by id."))
