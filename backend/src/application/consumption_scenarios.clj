(ns application.consumption-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.consumption :as consumption]))


(defn list-consumptions
      "List all consumptions for the given user."
      [consumption-repo user-id]
      (consumption/find-by-user-id consumption-repo user-id))

(defn create-consumption
      "Create a new consumption for the given user."
      [consumption-repo consumption-id user-id]
      (let [c (consumption/create-new-consumption consumption-id user-id)
            c (consumption/save! consumption-repo c)]
        (mu/log ::consumption-created :consumption-id consumption-id :user-id user-id)
        c))

(defn- find-and-check-ownership
       "Find a consumption by id and verify the user owns it."
       [consumption-repo user-id consumption-id]
       (let [c (consumption/find-by-id consumption-repo consumption-id)]
         (when-not c
           (throw (ex-info "Consumption not found" {:consumption-id consumption-id})))
         (when-not (= user-id (:consumption/user-id c))
           (throw (ex-info "Consumption does not belong to user"
                           {:user-id        user-id
                            :consumption-id consumption-id})))
         c))

(defn register-consumer-information
      "Register step 1: consumer address and network-id."
      [consumption-repo user-id consumption-id address network-id]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/register-consumer-information c address network-id)
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::consumer-information-registered :consumption-id consumption-id)
        c'))

(defn associate-linky-reference
      "Associate step 2: linky reference number."
      [consumption-repo user-id consumption-id linky-ref]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/associate-linky-reference c linky-ref)
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::linky-reference-associated :consumption-id consumption-id)
        c'))

(defn complete-billing-address
      "Complete step 3: billing address."
      [consumption-repo user-id consumption-id billing-addr]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/complete-billing-address c billing-addr)
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::billing-address-completed :consumption-id consumption-id)
        c'))

(defn go-back-consumption
      "Move a consumption back to the previous onboarding step."
      [consumption-repo user-id consumption-id]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/go-back c)
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::consumption-went-back :consumption-id consumption-id
                :from (:consumption/lifecycle c) :to (:consumption/lifecycle c'))
        c'))

(defn abandon-consumption
      "Abandon a consumption during onboarding."
      [consumption-repo user-id consumption-id]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/abandon c)
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::consumption-abandoned :consumption-id consumption-id)
        c'))

(defn sign-contract
      "Sign one contract (contract-type = :elinkco | :producer | :sepa)."
      [consumption-repo user-id consumption-id contract-type]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/sign-contract c contract-type)
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::contract-signed :consumption-id consumption-id :contract-type contract-type)
        c'))
