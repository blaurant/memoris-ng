(ns domain.consumption-repo)

(defprotocol ConsumptionRepo
  (find-by-id      [repo id]       "Find a consumption by ID.")
  (find-by-user-id [repo user-id]  "Find all consumptions for a user. Returns a vector.")
  (save!           [repo consumption] "Persist a consumption (insert or update)."))
