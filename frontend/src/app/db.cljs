(ns app.db)

(def default-db
  {:hello/message      nil
   :hello/loading?     false

   :networks/list      []
   :networks/loading?  false

   :eligibility/result   nil
   :eligibility/loading? false
   :eligibility/address  nil

   :router/current-page  :page/home})
