(ns application.consumption-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.consumption-scenarios :as scenarios]
            [domain.id :as id]
            [infrastructure.in-memory-repo.mem-consumption-repo :as mem-repo]))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-repo []
  (mem-repo/->InMemoryConsumptionRepo (atom {})))

;; ── Scenarios ────────────────────────────────────────────────────────────────

(defscenario "Create a new consumption"
  (GIVEN "a fresh repo and a user" [ctx]
    (assoc ctx
           :repo    (fresh-repo)
           :user-id (id/build-id)))
  (WHEN "the user creates a consumption" [ctx]
    (assoc ctx :consumption
           (scenarios/create-consumption (:repo ctx) (id/build-id) (:user-id ctx))))
  (THEN "the consumption is in :consumer-information state" [ctx]
    (assert (= :consumer-information (:consumption/lifecycle (:consumption ctx))))
    (assert (= (:user-id ctx) (:consumption/user-id (:consumption ctx))))))

(defscenario "Complete onboarding steps 1 to 4"
  (GIVEN "a consumption in :consumer-information state" [ctx]
    (let [repo    (fresh-repo)
          user-id (id/build-id)
          c       (scenarios/create-consumption repo (id/build-id) user-id)]
      (assoc ctx :repo repo :user-id user-id :consumption c)))
  (WHEN "the user submits step 1 (consumer informations)" [ctx]
    (let [network-id (id/build-id)
          c' (scenarios/register-consumer-information
               (:repo ctx) (:user-id ctx) (:consumption/id (:consumption ctx))
               "10 rue de Paris" network-id)]
      (assoc ctx :consumption c' :network-id network-id)))
  (THEN "the consumption is in :linky-reference state" [ctx]
    (assert (= :linky-reference (:consumption/lifecycle (:consumption ctx)))))
  (WHEN "the user submits step 2 (linky reference)" [ctx]
    (assoc ctx :consumption
           (scenarios/associate-linky-reference
             (:repo ctx) (:user-id ctx) (:consumption/id (:consumption ctx))
             "LINKY-98765")))
  (THEN "the consumption is in :billing-address state" [ctx]
    (assert (= :billing-address (:consumption/lifecycle (:consumption ctx)))))
  (WHEN "the user submits step 3 (billing address)" [ctx]
    (assoc ctx :consumption
           (scenarios/complete-billing-address
             (:repo ctx) (:user-id ctx) (:consumption/id (:consumption ctx))
             "20 avenue de Lyon")))
  (THEN "the consumption is in :contract-signature state" [ctx]
    (assert (= :contract-signature (:consumption/lifecycle (:consumption ctx)))))

  (WHEN "the user signs the Elinkco contract" [ctx]
    (assoc ctx :consumption
           (scenarios/sign-contract
             (:repo ctx) (:user-id ctx) (:consumption/id (:consumption ctx))
             :elinkco)))

  (THEN "the consumption is still in :contract-signature state" [ctx]
    (assert (= :contract-signature (:consumption/lifecycle (:consumption ctx)))))

  (WHEN "the user signs the producer contract" [ctx]
    (assoc ctx :consumption
           (scenarios/sign-contract
             (:repo ctx) (:user-id ctx) (:consumption/id (:consumption ctx))
             :producer)))

  (THEN "the consumption is still in :contract-signature state (2/3)" [ctx]
    (assert (= :contract-signature (:consumption/lifecycle (:consumption ctx)))))

  (WHEN "the user signs the SEPA mandate" [ctx]
    (assoc ctx :consumption
           (scenarios/sign-contract
             (:repo ctx) (:user-id ctx) (:consumption/id (:consumption ctx))
             :sepa)))

  (THEN "the consumption is in :pending state with all 3 signatures" [ctx]
    (assert (= :pending (:consumption/lifecycle (:consumption ctx))))
    (assert (some? (:consumption/contract-signed-at (:consumption ctx))))
    (assert (some? (:consumption/producer-contract-signed-at (:consumption ctx))))
    (assert (some? (:consumption/sepa-mandate-signed-at (:consumption ctx))))))

(defscenario "List consumptions for a user"
  (GIVEN "two consumptions for user A and one for user B" [ctx]
    (let [repo     (fresh-repo)
          user-a   (id/build-id)
          user-b   (id/build-id)]
      (scenarios/create-consumption repo (id/build-id) user-a)
      (scenarios/create-consumption repo (id/build-id) user-a)
      (scenarios/create-consumption repo (id/build-id) user-b)
      (assoc ctx :repo repo :user-a user-a :user-b user-b)))

  (WHEN "we list consumptions for user A" [ctx]
    (assoc ctx :list-a (scenarios/list-consumptions (:repo ctx) (:user-a ctx))))

  (THEN "we get 2 consumptions" [ctx]
    (assert (= 2 (count (:list-a ctx)))))

  (WHEN "we list consumptions for user B" [ctx]
    (assoc ctx :list-b (scenarios/list-consumptions (:repo ctx) (:user-b ctx))))

  (THEN "we get 1 consumption" [ctx]
    (assert (= 1 (count (:list-b ctx))))))

(defscenario "Ownership check — user B cannot modify user A consumption"
  (GIVEN "a consumption belonging to user A" [ctx]
    (let [repo   (fresh-repo)
          user-a (id/build-id)
          user-b (id/build-id)
          c      (scenarios/create-consumption repo (id/build-id) user-a)]
      (assoc ctx :repo repo :user-a user-a :user-b user-b :consumption c)))

  (WHEN "user B tries to submit step 1" [ctx]
    (try
      (scenarios/register-consumer-information
        (:repo ctx) (:user-b ctx) (:consumption/id (:consumption ctx))
        "addr" (id/build-id))
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an ownership error is thrown" [ctx]
    (assert (= "Consumption does not belong to user" (:exception ctx)))))
