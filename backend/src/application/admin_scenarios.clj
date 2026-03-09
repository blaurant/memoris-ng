(ns application.admin-scenarios
  (:require [domain.user-repo :as user-repo]))

(defn list-users
  "Returns all users."
  [user-repo]
  (user-repo/find-all user-repo))
