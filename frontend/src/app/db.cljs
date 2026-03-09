(ns app.db)

(def default-db
  {:hello/message      nil
   :hello/loading?     false

   :networks/list      []
   :networks/loading?  false

   :eligibility/result   nil
   :eligibility/loading? false
   :eligibility/address  nil

   :router/current-page  :page/home

   :auth/token    nil
   :auth/user     nil
   :auth/loading? false
   :auth/error    nil

   :consumptions/list     []
   :consumptions/loading? false
   :portal/active-section :dashboard

   :admin/users           []
   :admin/users-loading?  false
   :admin/networks        []
   :admin/networks-loading? false
   :admin/active-tab      :users})
