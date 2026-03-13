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
   :auth/register-success? false
   :auth/register-email    nil
   :auth/verification-status nil
   :auth/resend-success?   false

   :consumptions/list     []
   :consumptions/loading? false

   :productions/list     []
   :productions/loading? false

   :portal/active-section :dashboard

   :admin/productions          []
   :admin/productions-loading? false

   :admin/users           []
   :admin/users-loading?  false
   :admin/networks        []
   :admin/networks-loading? false
   :admin/eligibility-checks []
   :admin/eligibility-checks-loading? false
   :admin/active-tab      :users

   :alert/message  nil
   :alert/active?  false})
