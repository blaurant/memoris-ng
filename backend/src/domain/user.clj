(ns ^{:domain/type :entity} domain.user
    (:require [domain.id :as id]
      [malli.core :as m])
    (:import (java.time Instant LocalDate)))


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
   [:fn {:error/message "must be at least 8 characters"} #(>= (count %) 8)]
   [:fn {:error/message "must contain at least one special character"}
    #(re-find #"[^a-zA-Z0-9]" %)]])

(defn assert-valid-password
  "Validates a raw password against the password policy.
   Throws ex-info if invalid."
  [raw-password]
  (when-not (m/validate password? raw-password)
    (throw (ex-info "Password must be at least 8 characters with at least one special character"
                    {:errors (m/explain password? raw-password)}))))

(def non-blank-string?
  [:and string?
   [:fn {:error/message "must not be blank"} #(not (clojure.string/blank? %))]])

(def phone?
  [:and string?
   [:fn {:error/message "must contain only digits, spaces, +, -, ., () characters"}
    #(re-matches #"[0-9+\-.()\s]+" %)]
   [:fn {:error/message "must contain at least 10 digits"}
    #(>= (count (re-seq #"\d" %)) 10)]])

(def ^:private min-birth-year 1920)
(def ^:private min-age-years 17)

(def birth-date?
  [:and non-blank-string?
   [:fn {:error/message "must be a valid date, age between 17 and 105 years"}
    (fn [s]
      (try
        (let [d   (LocalDate/parse s)
              min (LocalDate/of min-birth-year 1 1)
              max (.minusYears (LocalDate/now) min-age-years)]
          (and (not (.isBefore d min))
               (not (.isAfter d max))))
        (catch Exception _ false)))]])

(def NaturalPerson
  [:map
   [:first-name non-blank-string?]
   [:last-name non-blank-string?]
   [:birth-date birth-date?]
   [:address non-blank-string?]
   [:postal-code [:and non-blank-string?
                  [:re {:error/message "must be a valid French postal code"}
                   #"^(0[1-9]|[1-8]\d|9[0-5]|97[1-6]|98[0-8])\d{3}$"]]]
   [:city non-blank-string?]
   [:phone phone?]])

(def LegalPerson
  [:map
   [:company-name non-blank-string?]
   [:siren [:and non-blank-string?
            [:fn {:error/message "must be a valid 9-digit SIREN with valid Luhn checksum"}
             (fn [s]
               (and (some? (re-matches #"^\d{9}$" s))
                    (let [digits (map #(Character/digit ^char % 10) s)]
                      (zero? (rem (reduce + (map-indexed
                                              (fn [i d]
                                                (let [v (if (even? i) d (* 2 d))]
                                                  (if (> v 9) (- v 9) v)))
                                              digits))
                                  10)))))]]]
   [:headquarters non-blank-string?]
   [:representative-first-name non-blank-string?]
   [:representative-last-name non-blank-string?]
   [:representative-role non-blank-string?]
   [:phone phone?]])

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
   [:user/email-verified? {:optional true} boolean?]
   [:user/adhesion-signed-at {:optional true} [:maybe string?]]
   [:user/docuseal-submission-id {:optional true} [:maybe int?]]
   [:user/natural-person {:optional true} [:maybe NaturalPerson]]
   [:user/legal-persons {:optional true} [:maybe [:vector LegalPerson]]]])


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

(defn adhesion-signed?
      "Returns true if the user has signed the adhesion."
      [user]
      (some? (:user/adhesion-signed-at user)))

(defn sign-adhesion
      "Sign the Elink-co adhesion on the user."
      ([user]
       (sign-adhesion user (str (Instant/now))))
      ([user signed-at]
       (assoc user :user/adhesion-signed-at signed-at)))

(defn update-natural-person
      "Update user's natural person profile.
       Validates against NaturalPerson Malli schema."
      [user person-info]
      (when-not (m/validate NaturalPerson person-info)
        (throw (ex-info "Invalid natural person data"
                        {:errors (m/explain NaturalPerson person-info)})))
      (assoc user :user/natural-person person-info))

(defn add-legal-person
      "Add a legal person to the user's list of legal persons.
       Validates against LegalPerson Malli schema."
      [user legal-info]
      (when-not (m/validate LegalPerson legal-info)
        (throw (ex-info "Invalid legal person data"
                        {:errors (m/explain LegalPerson legal-info)})))
      (update user :user/legal-persons (fnil conj []) legal-info))

(defn update-legal-person
      "Update a legal person at given index in the user's list.
       Validates against LegalPerson Malli schema."
      [user index legal-info]
      (when-not (m/validate LegalPerson legal-info)
        (throw (ex-info "Invalid legal person data"
                        {:errors (m/explain LegalPerson legal-info)})))
      (let [persons (or (:user/legal-persons user) [])]
        (when (>= index (count persons))
          (throw (ex-info "Legal person index out of bounds"
                          {:index index :count (count persons)})))
        (assoc user :user/legal-persons (assoc persons index legal-info))))

(defn remove-legal-person
      "Remove a legal person at given index from the user's list."
      [user index]
      (let [persons (or (:user/legal-persons user) [])]
        (when (>= index (count persons))
          (throw (ex-info "Legal person index out of bounds"
                          {:index index :count (count persons)})))
        (assoc user :user/legal-persons
               (into (subvec persons 0 index)
                     (subvec persons (inc index))))))

;; ── Repository protocol ───────────────────────────────────────────────────

(defn set-docuseal-submission-id
      "Store the DocuSeal submission ID on the user."
      [user submission-id]
      (assoc user :user/docuseal-submission-id submission-id))

(defprotocol UserRepo
  (find-by-id       [repo id]               "Find a user by ID.")
  (find-by-email    [repo provider email]    "Find a user by provider and email.")
  (find-by-provider [repo provider provider-subject-identifier] "Find a user by provider and provider-subject-identifier.")
  (find-by-docuseal-submission-id [repo submission-id] "Find a user by DocuSeal submission ID.")
  (find-all         [repo]                   "Returns all users.")
  (save!            [repo user]              "Persist a user (insert or update)."))
