(ns domain.consumption-repo)

(defprotocol ConsumptionRepo
  (find-by-id      [repo id]                    "Find a consumption by ID.")
  (find-by-user-id [repo user-id]               "Find all consumptions for a user. Returns a vector.")
  (save!           [repo consumption]
                   [repo original updated]       "Persist a consumption. 2-arity: insert. 3-arity: optimistic update (fails if entity changed since read)."))
