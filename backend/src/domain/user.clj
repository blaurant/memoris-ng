(ns ^{:domain/type :entity} domain.user
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

(def password?
  [:and
   string?
   [:fn {:error/message "must be at least 8 characters"} #(>= (count %) 8)]])

(def User
  [:map
   [:user/id [:fn {:error/message "must be a valid ID"} id/id?]]
   [:user/email email?]
   [:user/name name?]
   [:user/provider [:enum :google :apple :facebook :email]]
   [:user/provider-subject-identifier string?]
   [:user/role [:enum :customer :admin]]
   [:user/lifecycle [:enum :alive :suspended :deactivated]]
   [:user/password-hash {:optional true} [:maybe string?]]
   [:user/email-verified? {:optional true} boolean?]])


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

(defn create-email-user
      "Create a new user with email/password authentication."
      [id email name password-hash]
      (build-user {:user/id                          id
                   :user/email                       email
                   :user/name                        name
                   :user/provider                    :email
                   :user/provider-subject-identifier email
                   :user/password-hash               password-hash
                   :user/email-verified?             false
                   :user/role                        :customer
                   :user/lifecycle                   :alive}))

(defn email-verified?
      "Returns true if the user's email is verified.
       OAuth users (no :user/email-verified? key) are considered verified."
      [user]
      (get user :user/email-verified? true))

(defn verify-email
      "Marks the user's email as verified."
      [user]
      (assoc user :user/email-verified? true))

(defn assert-email-verified
      "Throws if the user's email is not verified."
      [user]
      (when-not (email-verified? user)
        (throw (ex-info "Email not verified" {:user-id (:user/id user)})))
      user)

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

