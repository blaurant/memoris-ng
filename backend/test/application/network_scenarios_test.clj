(ns application.network-scenarios-test
  (:require [bdd.test-gwt :refer [defscenario GIVEN WHEN THEN]]
            [application.network-scenarios :as scenarios]
            [domain.consumption :as consumption]
            [domain.id :as id]
            [domain.network :as network]
            [domain.production :as production]
            [infrastructure.in-memory-repo.mem-consumption-repo :as mem-cons-repo]
            [infrastructure.in-memory-repo.mem-network-repo :as mem-net-repo]
            [infrastructure.in-memory-repo.mem-production-repo :as mem-prod-repo]))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn- fresh-network-repo []
  (mem-net-repo/->InMemoryNetworkRepo (atom {})))

(defn- fresh-production-repo []
  (mem-prod-repo/->InMemoryProductionRepo (atom {})))

(defn- fresh-consumption-repo []
  (mem-cons-repo/->InMemoryConsumptionRepo (atom {})))

(defn- make-active-production
  "Build a fully-onboarded active production for the given network."
  [network-id energy-type installed-power address]
  (let [user-id (id/build-id)]
    (-> (production/create-new-production (id/build-id) user-id)
        (production/register-producer-information address network-id)
        (production/register-installation-info (format "%014d" (rand-int 99999999)) installed-power energy-type (str "LK-" (rand-int 999999)))
        (production/submit-payment-info "Jean Dupont" "FR7630006000011234567890189" nil "10 rue de Paris")
        (production/sign-contract true)
        ;; sign-contract puts it in :pending, we need :active
        (assoc :production/lifecycle :active))))

(defn- make-consumption
  "Build a minimal valid consumption for a network."
  [network-id]
  (let [user-id (id/build-id)
        cons-id (id/build-id)]
    {:consumption/id         cons-id
     :consumption/user-id    user-id
     :consumption/lifecycle  :active
     :consumption/network-id network-id}))

;; ── Scenarios ────────────────────────────────────────────────────────────────

(defscenario "Public network with active productions returns correct aggregation"
  (GIVEN "a public network with 2 active productions (solar 3kWc, wind 5kWc) and 3 consumers" [ctx]
    (let [net-repo  (fresh-network-repo)
          prod-repo (fresh-production-repo)
          cons-repo (fresh-consumption-repo)
          net-id    (id/build-id)
          net       (network/build-network {:network/id net-id
                                            :network/name "Reseau Soleil"
                                            :network/center-lat 43.6
                                            :network/center-lng 3.87
                                            :network/radius-km 5.0
                                            :network/lifecycle :public})]
      (network/save! net-repo net)
      ;; Two active productions
      (production/save! prod-repo (make-active-production net-id :solar 3.0 "12 rue A"))
      (production/save! prod-repo (make-active-production net-id :wind 5.0 "34 rue B"))
      ;; Three consumers
      (consumption/save! cons-repo (make-consumption net-id))
      (consumption/save! cons-repo (make-consumption net-id))
      (consumption/save! cons-repo (make-consumption net-id))
      (assoc ctx :net-repo net-repo :prod-repo prod-repo :cons-repo cons-repo :net-id net-id)))

  (WHEN "get-network-detail is called" [ctx]
    (assoc ctx :result
           (scenarios/get-network-detail (:net-repo ctx) (:prod-repo ctx) (:cons-repo ctx) (:net-id ctx))))

  (THEN "returns correct aggregation with total-capacity-kwc=8, energy-mix percentages, consumer-count=3" [ctx]
    (let [r (:result ctx)]
      (assert (= 8.0 (:total-capacity-kwc r)))
      (assert (= 3 (:consumer-count r)))
      (assert (= 2 (count (:productions r))))
      (assert (= {:solar 38 :wind 63} (:energy-mix r)))
      (assert (some? (:network r))))))

(defscenario "Non-public network returns ex-info"
  (GIVEN "a private (non-public) network" [ctx]
    (let [net-repo  (fresh-network-repo)
          prod-repo (fresh-production-repo)
          cons-repo (fresh-consumption-repo)
          net-id    (id/build-id)
          net       (network/build-network {:network/id net-id
                                            :network/name "Reseau Prive"
                                            :network/center-lat 48.85
                                            :network/center-lng 2.35
                                            :network/lifecycle :private})]
      (network/save! net-repo net)
      (assoc ctx :net-repo net-repo :prod-repo prod-repo :cons-repo cons-repo :net-id net-id)))

  (WHEN "get-network-detail is called" [ctx]
    (try
      (scenarios/get-network-detail (:net-repo ctx) (:prod-repo ctx) (:cons-repo ctx) (:net-id ctx))
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an ex-info is thrown" [ctx]
    (assert (= "Network not found" (:exception ctx)))))

(defscenario "Non-existent network returns ex-info"
  (GIVEN "a non-existent network-id" [ctx]
    (assoc ctx
           :net-repo (fresh-network-repo)
           :prod-repo (fresh-production-repo)
           :cons-repo (fresh-consumption-repo)
           :net-id (id/build-id)))

  (WHEN "get-network-detail is called" [ctx]
    (try
      (scenarios/get-network-detail (:net-repo ctx) (:prod-repo ctx) (:cons-repo ctx) (:net-id ctx))
      (assoc ctx :exception nil)
      (catch clojure.lang.ExceptionInfo e
        (assoc ctx :exception (.getMessage e)))))

  (THEN "an ex-info is thrown" [ctx]
    (assert (= "Network not found" (:exception ctx)))))

(defscenario "Public network with zero active productions returns empty stats"
  (GIVEN "a public network with zero active productions" [ctx]
    (let [net-repo  (fresh-network-repo)
          prod-repo (fresh-production-repo)
          cons-repo (fresh-consumption-repo)
          net-id    (id/build-id)
          net       (network/build-network {:network/id net-id
                                            :network/name "Reseau Vide"
                                            :network/center-lat 45.0
                                            :network/center-lng 1.0
                                            :network/lifecycle :public})]
      (network/save! net-repo net)
      (assoc ctx :net-repo net-repo :prod-repo prod-repo :cons-repo cons-repo :net-id net-id)))

  (WHEN "get-network-detail is called" [ctx]
    (assoc ctx :result
           (scenarios/get-network-detail (:net-repo ctx) (:prod-repo ctx) (:cons-repo ctx) (:net-id ctx))))

  (THEN "returns total-capacity-kwc 0 and energy-mix {} and empty productions" [ctx]
    (let [r (:result ctx)]
      (assert (= 0 (:total-capacity-kwc r)))
      (assert (= {} (:energy-mix r)))
      (assert (empty? (:productions r))))))

(defscenario "Serialized productions contain no sensitive fields"
  (GIVEN "a public network with productions" [ctx]
    (let [net-repo  (fresh-network-repo)
          prod-repo (fresh-production-repo)
          cons-repo (fresh-consumption-repo)
          net-id    (id/build-id)
          net       (network/build-network {:network/id net-id
                                            :network/name "Reseau Securise"
                                            :network/center-lat 44.0
                                            :network/center-lng 2.0
                                            :network/lifecycle :public})]
      (network/save! net-repo net)
      (production/save! prod-repo (make-active-production net-id :solar 3.0 "12 rue Secure"))
      (assoc ctx :net-repo net-repo :prod-repo prod-repo :cons-repo cons-repo :net-id net-id)))

  (WHEN "get-network-detail is called" [ctx]
    (assoc ctx :result
           (scenarios/get-network-detail (:net-repo ctx) (:prod-repo ctx) (:cons-repo ctx) (:net-id ctx))))

  (THEN "response contains no :production/iban, :production/pdl-prm, :production/user-id" [ctx]
    (let [prods (:productions (:result ctx))
          forbidden-keys #{:production/iban :production/pdl-prm :production/user-id}]
      (assert (seq prods))
      (assert (every? (fn [p]
                        (empty? (clojure.set/intersection (set (keys p)) forbidden-keys)))
                      prods)))))
