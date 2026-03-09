(ns infrastructure.rest-api.admin-middleware)

(defn wrap-admin-only
  "Ring middleware that checks the authenticated user has the :admin role.
   Must be composed after wrap-jwt-auth (expects :identity in request).
   Returns 403 if not admin."
  [handler]
  (fn [request]
    (if (= :admin (get-in request [:identity :role]))
      (handler request)
      {:status 403
       :body   {:error "Admin access required"}})))
