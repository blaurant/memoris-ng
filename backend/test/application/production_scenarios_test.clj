(ns application.production-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.production-scenarios :as scenarios]
            [domain.id :as id]
            [infrastructure.in-memory-repo.mem-production-repo :as mem-repo]))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-repo []
  (mem-repo/->InMemoryProductionRepo (atom {})))

;; ── Scenarios ────────────────────────────────────────────────────────────────

(defscenario "Create a new production"
  (GIVEN "a fresh repo and a user" [ctx]
    (assoc ctx
           :repo    (fresh-repo)
           :user-id (id/build-id)))
  (WHEN "the user creates a production" [ctx]
    (assoc ctx :production
           (scenarios/create-production (:repo ctx) (id/build-id) (:user-id ctx))))
  (THEN "the production is in :installation-info state" [ctx]
    (assert (= :installation-info (:production/lifecycle (:production ctx))))
    (assert (= (:user-id ctx) (:production/user-id (:production ctx))))))

(defscenario "Complete onboarding steps 1 to 3"
  (GIVEN "a production in :installation-info state" [ctx]
    (let [repo    (fresh-repo)
          user-id (id/build-id)
          p       (scenarios/create-production repo (id/build-id) user-id)]
      (assoc ctx :repo repo :user-id user-id :production p)))
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
    (assoc ctx :production
           (scenarios/sign-contract
             (:repo ctx) (:user-id ctx) (:production/id (:production ctx)))))
  (THEN "the production is in :pending state with adhesion signed" [ctx]
    (assert (= :pending (:production/lifecycle (:production ctx))))
    (assert (some? (:production/adhesion-signed-at (:production ctx))))))

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

  (WHEN "user B tries to submit step 1" [ctx]
    (try
      (scenarios/register-installation-info
        (:repo ctx) (:user-b ctx) (:production/id (:production ctx))
        "PRM-123" 9.0 :solar "LK-1")
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an ownership error is thrown" [ctx]
    (assert (= "Production does not belong to user" (:exception ctx)))))
