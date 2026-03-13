(ns application.production-scenarios
  (:require [com.brunobonacci.mulog :as mu]
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

(defn register-installation-info
      "Register step 1: installation details."
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

(defn sign-contract
      "Sign the adhesion contract."
      [production-repo user-id production-id]
      (let [p  (find-and-check-ownership production-repo user-id production-id)
            p' (production/sign-contract p)
            p' (production/save! production-repo p p')]
        (mu/log ::contract-signed :production-id production-id)
        p'))
