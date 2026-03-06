(ns application.consumption-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.consumption :as consumption]
            [domain.consumption-repo :as repo]))


(defn list-consumptions
      "List all consumptions for the given user."
      [consumption-repo user-id]
      (repo/find-by-user-id consumption-repo user-id))

(defn create-consumption
      "Create a new consumption for the given user."
      [consumption-repo consumption-id user-id]
      (let [c (consumption/create-new-consumption consumption-id user-id)
            c (repo/save! consumption-repo c)]
        (mu/log ::consumption-created :consumption-id consumption-id :user-id user-id)
        c))

(defn- find-and-check-ownership
       "Find a consumption by id and verify the user owns it."
       [consumption-repo user-id consumption-id]
       (let [c (repo/find-by-id consumption-repo consumption-id)]
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
            c' (repo/save! consumption-repo c c')]
        (mu/log ::consumer-information-registered :consumption-id consumption-id)
        c'))

(defn associate-linky-reference
      "Associate step 2: linky reference number."
      [consumption-repo user-id consumption-id linky-ref]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/associate-linky-reference c linky-ref)
            c' (repo/save! consumption-repo c c')]
        (mu/log ::linky-reference-associated :consumption-id consumption-id)
        c'))

(defn complete-billing-address
      "Complete step 3: billing address."
      [consumption-repo user-id consumption-id billing-addr]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/complete-billing-address c billing-addr)
            c' (repo/save! consumption-repo c c')]
        (mu/log ::billing-address-completed :consumption-id consumption-id)
        c'))

(defn sign-contract
      "Sign one contract (contract-type = :proxywatt | :producer | :sepa)."
      [consumption-repo user-id consumption-id contract-type]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            c' (consumption/sign-contract c contract-type)
            c' (repo/save! consumption-repo c c')]
        (mu/log ::contract-signed :consumption-id consumption-id :contract-type contract-type)
        c'))
