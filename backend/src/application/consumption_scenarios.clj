(ns application.consumption-scenarios
  (:require [com.brunobonacci.mulog :as mu]
            [domain.adhesion-html :as adhesion-html]
            [domain.consumption :as consumption]
            [domain.document-signer :as document-signer]
            [domain.network :as network]
            [domain.production :as production]
            [domain.user :as user]))


(defn list-consumptions
      "List all consumptions for the given user."
      [consumption-repo user-id]
      (consumption/find-by-user-id consumption-repo user-id))

(defn get-consumption-dashboard
  "Build a dashboard view for a consumption: network info, producers on the network."
  [consumption-repo production-repo network-repo user-id consumption-id]
  (let [c   (let [found (consumption/find-by-id consumption-repo consumption-id)]
              (when-not found
                (throw (ex-info "Consumption not found" {:consumption-id consumption-id})))
              (when-not (= user-id (:consumption/user-id found))
                (throw (ex-info "Consumption does not belong to user" {})))
              found)
        nid (:consumption/network-id c)
        net (when nid (network/find-by-id network-repo nid))
        ;; Producers on the network
        all-prods   (when nid (production/find-by-network-id production-repo nid))
        active-prods (filterv #(= :active (:production/lifecycle %)) all-prods)]
    {:consumption c
     :network     (when net
                    (select-keys net [:network/id :network/name :network/center-lat
                                      :network/center-lng :network/radius-km
                                      :network/price-per-kwh]))
     :producers   (mapv (fn [p]
                           {:energy-type      (name (:production/energy-type p))
                            :installed-power  (:production/installed-power p)
                            :producer-address (:production/producer-address p)})
                        active-prods)}))

(defn update-consumer-address
  "Update the consumer address on an active or pending consumption."
  [consumption-repo user-id consumption-id new-address]
  (let [c (let [found (consumption/find-by-id consumption-repo consumption-id)]
            (when-not found (throw (ex-info "Consumption not found" {})))
            (when-not (= user-id (:consumption/user-id found)) (throw (ex-info "Consumption does not belong to user" {})))
            found)]
    (when-not (#{:active :pending} (:consumption/lifecycle c))
      (throw (ex-info "Can only update address on active or pending consumptions" {})))
    (let [c' (assoc c :consumption/consumer-address new-address)
          c' (consumption/save! consumption-repo c c')]
      (mu/log ::consumer-address-updated :consumption-id consumption-id)
      c')))

(defn update-billing-address
  "Update the billing address on an active or pending consumption."
  [consumption-repo user-id consumption-id new-address]
  (let [c (let [found (consumption/find-by-id consumption-repo consumption-id)]
            (when-not found (throw (ex-info "Consumption not found" {})))
            (when-not (= user-id (:consumption/user-id found)) (throw (ex-info "Consumption does not belong to user" {})))
            found)]
    (when-not (#{:active :pending} (:consumption/lifecycle c))
      (throw (ex-info "Can only update billing address on active or pending consumptions" {})))
    (let [c' (assoc c :consumption/billing-address new-address)
          c' (consumption/save! consumption-repo c c')]
      (mu/log ::billing-address-updated :consumption-id consumption-id)
      c')))

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

(defn delete-consumption
  "Delete a consumption owned by the user."
  [consumption-repo user-id consumption-id]
  (let [c (find-and-check-ownership consumption-repo user-id consumption-id)]
    (consumption/delete! consumption-repo consumption-id)
    (mu/log ::consumption-deleted :consumption-id consumption-id :user-id user-id)
    c))

(defn sign-adhesion
      "Initiate the Elink-co adhesion signing via DocuSeal.
       Returns {:signing-url ...} for the frontend to embed.
       The adhesion is NOT marked as signed until the webhook confirms it."
      [user-repo document-signer user-id]
      (let [u (user/find-by-id user-repo user-id)]
        (when-not u
          (throw (ex-info "User not found" {:user-id user-id})))
        (when (user/adhesion-signed? u)
          (throw (ex-info "Adhesion already signed" {:user-id user-id})))
        (let [html   (adhesion-html/render-adhesion-html
                       (:user/name u) (:user/email u))
              result (document-signer/create-html-signature-request
                       document-signer html (:user/email u))
              u'     (-> u
                         (user/set-docuseal-submission-id (:submission-id result)))]
          (user/save! user-repo u')
          (mu/log ::adhesion-signature-requested
                  :user-id       user-id
                  :submission-id (:submission-id result))
          {:signing-url (:signing-url result)})))

(defn complete-adhesion-webhook
      "Called by the DocuSeal webhook when adhesion is signed.
       Finds the user by submission-id and marks adhesion as signed."
      [user-repo submission-id]
      (let [u (user/find-by-docuseal-submission-id user-repo submission-id)]
        (when-not u
          (throw (ex-info "User not found for submission" {:submission-id submission-id})))
        (when-not (user/adhesion-signed? u)
          (let [u' (user/sign-adhesion u)]
            (user/save! user-repo u')
            (mu/log ::adhesion-signed :user-id (:user/id u)
                    :submission-id submission-id)))
        :ok))

(defn check-adhesion-status
      "Check with DocuSeal if the adhesion has been signed.
       If completed, marks the user's adhesion as signed."
      [user-repo document-signer user-id]
      (let [u (user/find-by-id user-repo user-id)]
        (when-not u
          (throw (ex-info "User not found" {:user-id user-id})))
        (if (user/adhesion-signed? u)
          {:signed true}
          (if-not (:user/docuseal-submission-id u)
            {:signed false}
            (let [completed? (document-signer/submission-completed?
                               document-signer (:user/docuseal-submission-id u))]
              (when completed?
                (let [u' (user/sign-adhesion u)]
                  (user/save! user-repo u')
                  (mu/log ::adhesion-confirmed-via-poll
                          :user-id user-id
                          :submission-id (:user/docuseal-submission-id u))))
              {:signed completed?})))))

(defn get-adhesion-document-url
      "Retrieve the signed adhesion document URL from DocuSeal."
      [user-repo document-signer user-id]
      (let [u (user/find-by-id user-repo user-id)]
        (when-not u
          (throw (ex-info "User not found" {:user-id user-id})))
        (when-not (:user/docuseal-submission-id u)
          (throw (ex-info "No adhesion submission found" {:user-id user-id})))
        (let [url (document-signer/get-signed-document-url
                    document-signer (:user/docuseal-submission-id u))]
          (when-not url
            (throw (ex-info "Document not yet available" {:user-id user-id})))
          {:document-url url})))

(defn sign-contract
      "Sign one contract (contract-type = :producer | :sepa).
       Checks user adhesion to determine if consumption can transition to :pending."
      [consumption-repo user-repo user-id consumption-id contract-type]
      (let [c  (find-and-check-ownership consumption-repo user-id consumption-id)
            u  (user/find-by-id user-repo user-id)
            c' (consumption/sign-contract c contract-type (user/adhesion-signed? u))
            c' (consumption/save! consumption-repo c c')]
        (mu/log ::contract-signed :consumption-id consumption-id :contract-type contract-type)
        c'))
