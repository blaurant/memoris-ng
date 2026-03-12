(ns domain.eligibility-check-repo)

(defprotocol EligibilityCheckRepo
  (save! [repo check] "Persist an eligibility check.")
  (find-all [repo] "Return all eligibility checks."))
