(ns domain.user
    (:require [domain.id :as id]
      [malli.core :as m]))


(def email?
  [:and
   string?
   [:re {:error/message "must be a valid email address"}
    #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"]])

(def name?
  [:and
   string?
   [:fn {:error/message "must not be blank"} #(not (clojure.string/blank? %))]
   [:fn {:error/message "must be at most 100 characters"} #(<= (count %) 100)]])

(def User
  [:map
   [:user/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:user/email email?]
   [:user/name name?]
   [:user/provider [:enum :google :apple :facebook]]
   [:user/provider-subject-identifier string?]
   [:user/role [:enum :customer :admin]]
   [:user/lifecycle [:enum :alive :suspended :deactivated]]])


(defn build-user
      "Builds a new user by merging attrs over default-user.
      Validates against the User schema. Throws ex-info if the result is invalid."
      [attrs]
      (if (m/validate User attrs)
        attrs
        (throw (ex-info "Invalid user" {:attrs  attrs
                                        :errors (m/explain User attrs)}))))

(defn create-new-user
      "Create a new user with provider id"
      [provider-subject-id provider email name]
      (build-user {:user/id                          (id/build-id)
                   :user/email                       email
                   :user/name                        name
                   :user/provider                    provider
                   :user/provider-subject-identifier provider-subject-id
                   :user/role                        :customer
                   :user/lifecycle                   :alive}))

(defn alive?
      "Returns true if the user's lifecycle is :alive."
      [user]
      (= :alive (:user/lifecycle user)))

(defn check-alive
      "throw an exception is user is not alive"
      [user]
      (if (alive? user)
        user
        (throw (ex-info "User account is not alive"
                        {:lifecycle (:user/lifecycle user)}))))

(defn suspend
      "Transitions a user from :alive to :suspended.
      Throws if user is not alive."
      [user]
      (when-not (alive? user)
                (throw (ex-info "Can only suspend an alive user"
                                {:lifecycle (:user/lifecycle user)})))
      (assoc user :user/lifecycle :suspended))

(defn deactivate
      "Transitions a user from :alive or :suspended to :deactivated.
      Throws if user is already deactivated."
      [user]
      (when (= :deactivated (:user/lifecycle user))
            (throw (ex-info "User is already deactivated"
                            {:lifecycle (:user/lifecycle user)})))
      (assoc user :user/lifecycle :deactivated))

(defn reactivate
      "Transitions a user from :suspended to :alive.
      Throws if user is not suspended."
      [user]
      (when-not (= :suspended (:user/lifecycle user))
                (throw (ex-info "Can only reactivate a suspended user"
                                {:lifecycle (:user/lifecycle user)})))
      (assoc user :user/lifecycle :alive))

