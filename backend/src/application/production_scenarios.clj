(ns application.production-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.id :as id]
            [domain.network :as network]
            [domain.production :as production]))

(defn list-productions
      "List all productions for the given user."
      [production-repo user-id]
      (production/find-by-user-id production-repo user-id))

(defn create-production
      "Create a new production for the given user."
      [production-repo production-id user-id]
      (let [p (production/create-new-production production-id user-id)
            p (production/save! production-repo p)]
        (mu/log ::production-created :production-id production-id :user-id user-id)
        p))

(defn- find-and-check-ownership
       "Find a production by id and verify the user owns it."
       [production-repo user-id production-id]
       (let [p (production/find-by-id production-repo production-id)]
         (when-not p
           (throw (ex-info "Production not found" {:production-id production-id})))
         (when-not (= user-id (:production/user-id p))
           (throw (ex-info "Production does not belong to user"
                           {:user-id        user-id
                            :production-id production-id})))
         p))

(defn register-producer-information
      "Register step 0: address and network. If network-id is nil, create a new pending-validation network."
      [production-repo network-repo user-id production-id producer-address
       {:keys [network-id network-name network-lat network-lng network-radius]}]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            nid (if network-id
                  (id/build-id network-id)
                  (let [n (network/build-network
                            {:network/id         (id/build-id)
                             :network/name       network-name
                             :network/center-lat (double network-lat)
                             :network/center-lng (double network-lng)
                             :network/radius-km  (double (or network-radius 1.0))
                             :network/lifecycle  :pending-validation})]
                    (network/save! network-repo n)
                    (mu/log ::network-created-pending :network-id (:network/id n) :name network-name)
                    (:network/id n)))
            p' (production/register-producer-information p producer-address nid)
            p' (production/save! production-repo p p')]
        (mu/log ::producer-information-registered :production-id production-id)
        p'))

(defn register-installation-info
      "Register step 2: installation details."
      [production-repo user-id production-id pdl-prm installed-power energy-type linky-meter]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            p' (production/register-installation-info p pdl-prm installed-power energy-type linky-meter)
            p' (production/save! production-repo p p')]
        (mu/log ::installation-info-registered :production-id production-id)
        p'))

(defn submit-payment-info
      "Submit step 2: IBAN."
      [production-repo user-id production-id iban]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            p' (production/submit-payment-info p iban)
            p' (production/save! production-repo p p')]
        (mu/log ::payment-info-submitted :production-id production-id)
        p'))

(defn abandon-production
      "Abandon a production during onboarding."
      [production-repo user-id production-id]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            p' (production/abandon p)
            p' (production/save! production-repo p p')]
        (mu/log ::production-abandoned :production-id production-id)
        p'))

(defn go-back-production
      "Move a production back to the previous onboarding step."
      [production-repo user-id production-id]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            p' (production/go-back p)
            p' (production/save! production-repo p p')]
        (mu/log ::production-went-back :production-id production-id
                :from (:production/lifecycle p) :to (:production/lifecycle p'))
        p'))

(defn sign-contract
      "Sign the adhesion contract."
      [production-repo user-id production-id]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            p' (production/sign-contract p)
            p' (production/save! production-repo p p')]
        (mu/log ::contract-signed :production-id production-id)
        p'))
