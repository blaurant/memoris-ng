(ns domain.user-repo)

(defprotocol UserRepo
  (find-by-id       [repo id]               "Find a user by UUID.")
  (find-by-provider [repo provider provider-subject-identifier] "Find a user by provider and provider-subject-identifier.")
  (save!            [repo user]              "Persist a user (insert or update)."))
