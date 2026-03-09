(ns domain.network-repo)

(defprotocol NetworkRepo
  (find-all   [repo]         "Returns all networks.")
  (find-by-id [repo id]      "Find a network by ID.")
  (save!      [repo network] "Persist a network (insert or update)."))
