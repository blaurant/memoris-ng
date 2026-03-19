(ns application.production-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.production-scenarios :as scenarios]
            [domain.id :as id]
            [domain.network :as network]
            [domain.user :as user]
            [infrastructure.in-memory-repo.mem-network-repo :as mem-net-repo]
            [infrastructure.in-memory-repo.mem-production-repo :as mem-repo]
            [infrastructure.in-memory-repo.mem-user-repo :as mem-user-repo]))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-repo []
  (mem-repo/->InMemoryProductionRepo (atom {})))

(defn- fresh-network-repo []
  (let [repo (mem-net-repo/->InMemoryNetworkRepo (atom {}))]
    ;; Seed a public network
    (network/save! repo (network/build-network
                          {:network/id         (id/build-id)
                           :network/name       "Réseau Test"
                           :network/center-lat 48.85
                           :network/center-lng 2.35
                           :network/radius-km  10.0
                           :network/lifecycle  :public}))
    repo))

(defn- first-network-id [net-repo]
  (:network/id (first (network/find-all net-repo))))

;; ── Scenarios ────────────────────────────────────────────────────────────────

(defscenario "Create a new production"
  (GIVEN "a fresh repo and a user" [ctx]
    (assoc ctx
           :repo    (fresh-repo)
           :user-id (id/build-id)))
  (WHEN "the user creates a production" [ctx]
    (assoc ctx :production
           (scenarios/create-production (:repo ctx) (id/build-id) (:user-id ctx))))
  (THEN "the production is in :producer-information state" [ctx]
    (assert (= :producer-information (:production/lifecycle (:production ctx))))
    (assert (= (:user-id ctx) (:production/user-id (:production ctx))))))

(defscenario "Complete onboarding steps 0 to 3"
  (GIVEN "a production in :producer-information state" [ctx]
    (let [repo     (fresh-repo)
          net-repo (fresh-network-repo)
          user-id  (id/build-id)
          p        (scenarios/create-production repo (id/build-id) user-id)]
      (assoc ctx :repo repo :net-repo net-repo :user-id user-id :production p)))
  (WHEN "the user submits step 0 (producer information with existing network)" [ctx]
    (let [nid (first-network-id (:net-repo ctx))
          p'  (scenarios/register-producer-information
                (:repo ctx) (:net-repo ctx) (:user-id ctx)
                (:production/id (:production ctx))
                "12 rue de la Paix, Paris"
                {:network-id (str nid)})]
      (assoc ctx :production p')))
  (THEN "the production is in :installation-info state" [ctx]
    (assert (= :installation-info (:production/lifecycle (:production ctx))))
    (assert (= "12 rue de la Paix, Paris" (:production/producer-address (:production ctx)))))
  (WHEN "the user submits step 1 (installation info)" [ctx]
    (let [p' (scenarios/register-installation-info
               (:repo ctx) (:user-id ctx) (:production/id (:production ctx))
               "PRM-123456" 9.0 :solar "LINKY-789")]
      (assoc ctx :production p')))
  (THEN "the production is in :payment-info state" [ctx]
    (assert (= :payment-info (:production/lifecycle (:production ctx)))))
  (WHEN "the user submits step 2 (payment info)" [ctx]
    (assoc ctx :production
           (scenarios/submit-payment-info
             (:repo ctx) (:user-id ctx) (:production/id (:production ctx))
             "FR76 3000 6000 0112 3456 7890 189")))
  (THEN "the production is in :contract-signature state" [ctx]
    (assert (= :contract-signature (:production/lifecycle (:production ctx)))))
  (WHEN "the user signs the adhesion contract" [ctx]
    (let [user-repo (mem-user-repo/->InMemoryUserRepo (atom {}))
          u (user/build-user {:user/id                          (:user-id ctx)
                              :user/email                       "test@example.com"
                              :user/name                        "Test"
                              :user/role                        :customer
                              :user/lifecycle                   :alive
                              :user/provider                    :email
                              :user/provider-subject-identifier "test"
                              :user/password-hash               "hash"
                              :user/email-verified?             true})
          u (user/sign-adhesion u)]
      (user/save! user-repo u)
      (assoc ctx :production
             (scenarios/sign-contract
               (:repo ctx) user-repo (:user-id ctx) (:production/id (:production ctx))))))
  (THEN "the production is in :pending state" [ctx]
    (assert (= :pending (:production/lifecycle (:production ctx))))))

(defscenario "Create a new network when submitting producer information"
  (GIVEN "a production and an empty network repo" [ctx]
    (let [repo     (fresh-repo)
          net-repo (mem-net-repo/->InMemoryNetworkRepo (atom {}))
          user-id  (id/build-id)
          p        (scenarios/create-production repo (id/build-id) user-id)]
      (assoc ctx :repo repo :net-repo net-repo :user-id user-id :production p)))
  (WHEN "the user submits with new network details" [ctx]
    (let [p' (scenarios/register-producer-information
               (:repo ctx) (:net-repo ctx) (:user-id ctx)
               (:production/id (:production ctx))
               "Zone industrielle, Montpellier"
               {:network-name "Réseau Montpellier"
                :network-lat  43.61
                :network-lng  3.87
                :network-radius 15.0})]
      (assoc ctx :production p')))
  (THEN "the production has a network-id and a pending-validation network is created" [ctx]
    (assert (some? (:production/network-id (:production ctx))))
    (let [networks (network/find-all (:net-repo ctx))
          n (first networks)]
      (assert (= 1 (count networks)))
      (assert (= :pending-validation (:network/lifecycle n)))
      (assert (= "Réseau Montpellier" (:network/name n))))))

(defscenario "List productions for a user"
  (GIVEN "two productions for user A and one for user B" [ctx]
    (let [repo     (fresh-repo)
          user-a   (id/build-id)
          user-b   (id/build-id)]
      (scenarios/create-production repo (id/build-id) user-a)
      (scenarios/create-production repo (id/build-id) user-a)
      (scenarios/create-production repo (id/build-id) user-b)
      (assoc ctx :repo repo :user-a user-a :user-b user-b)))

  (WHEN "we list productions for user A" [ctx]
    (assoc ctx :list-a (scenarios/list-productions (:repo ctx) (:user-a ctx))))

  (THEN "we get 2 productions" [ctx]
    (assert (= 2 (count (:list-a ctx)))))

  (WHEN "we list productions for user B" [ctx]
    (assoc ctx :list-b (scenarios/list-productions (:repo ctx) (:user-b ctx))))

  (THEN "we get 1 production" [ctx]
    (assert (= 1 (count (:list-b ctx))))))

(defscenario "Ownership check — user B cannot modify user A production"
  (GIVEN "a production belonging to user A" [ctx]
    (let [repo   (fresh-repo)
          user-a (id/build-id)
          user-b (id/build-id)
          p      (scenarios/create-production repo (id/build-id) user-a)]
      (assoc ctx :repo repo :user-a user-a :user-b user-b :production p)))

  (WHEN "user B tries to submit step 0" [ctx]
    (try
      (scenarios/register-producer-information
        (:repo ctx) (mem-net-repo/->InMemoryNetworkRepo (atom {}))
        (:user-b ctx) (:production/id (:production ctx))
        "addr" {:network-name "X" :network-lat 48.0 :network-lng 2.0 :network-radius 10.0})
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an ownership error is thrown" [ctx]
    (assert (= "Production does not belong to user" (:exception ctx)))))
