(ns domain.production-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.production :as production]
            [infrastructure.in-memory-repo.mem-production-repo :as mem-repo]))

(def user-id (id/build-id))
(def network-id (id/build-id))

(def valid-attrs
  {:production/id        (id/build-id)
   :production/user-id   user-id
   :production/lifecycle :producer-information})

;; ── build-production ──────────────────────────────────────────────────────

(deftest build-production-valid
  (testing "builds a production with valid minimal attributes"
    (let [p (production/build-production valid-attrs)]
      (is (= :producer-information (:production/lifecycle p)))
      (is (= user-id (:production/user-id p))))))

(deftest build-production-invalid
  (testing "throws when id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (production/build-production (dissoc valid-attrs :production/id)))))

  (testing "throws when user-id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (production/build-production (dissoc valid-attrs :production/user-id)))))

  (testing "throws when lifecycle is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (production/build-production (dissoc valid-attrs :production/lifecycle)))))

  (testing "throws on invalid lifecycle"
    (is (thrown? clojure.lang.ExceptionInfo
                (production/build-production (assoc valid-attrs :production/lifecycle :invalid))))))

;; ── create-new-production ──────────────────────────────────────────────────

(deftest create-new-production-test
  (testing "creates a production in :producer-information state"
    (let [pid (id/build-id)
          p   (production/create-new-production pid user-id)]
      (is (= pid (:production/id p)))
      (is (= user-id (:production/user-id p)))
      (is (= :producer-information (:production/lifecycle p)))))

  (testing "throws when id is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid production"
                         (production/create-new-production "not-a-uuid" user-id))))

  (testing "throws when user-id is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid production"
                         (production/create-new-production (id/build-id) "not-a-uuid"))))

  (testing "throws when id is nil"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid production"
                         (production/create-new-production nil user-id))))

  (testing "throws when user-id is nil"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid production"
                         (production/create-new-production (id/build-id) nil)))))

;; ── onboarding? ────────────────────────────────────────────────────────────

(deftest onboarding?-test
  (testing "returns true for producer-information"
    (is (production/onboarding? (production/build-production valid-attrs))))

  (testing "returns true for installation-info"
    (is (production/onboarding?
          (production/build-production (assoc valid-attrs :production/lifecycle :installation-info)))))

  (testing "returns true for payment-info"
    (is (production/onboarding?
          (production/build-production (assoc valid-attrs :production/lifecycle :payment-info)))))

  (testing "returns true for contract-signature"
    (is (production/onboarding?
          (production/build-production (assoc valid-attrs :production/lifecycle :contract-signature)))))

  (testing "returns false for pending"
    (is (not (production/onboarding?
               (production/build-production (assoc valid-attrs :production/lifecycle :pending))))))

  (testing "returns false for active"
    (is (not (production/onboarding?
               (production/build-production (assoc valid-attrs :production/lifecycle :active)))))))

;; ── register-producer-information ────────────────────────────────────────

(deftest register-producer-information-test
  (testing "transitions from :producer-information to :installation-info"
    (let [p  (production/create-new-production (id/build-id) user-id)
          p' (production/register-producer-information p "12 rue de la Paix, Paris" network-id)]
      (is (= :installation-info (:production/lifecycle p')))
      (is (= "12 rue de la Paix, Paris" (:production/producer-address p')))
      (is (= network-id (:production/network-id p')))))

  (testing "throws when not in :producer-information state"
    (let [p (production/build-production (assoc valid-attrs :production/lifecycle :installation-info))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (production/register-producer-information p "addr" network-id))))))

;; ── register-installation-info ────────────────────────────────────────────

(deftest register-installation-info-test
  (testing "transitions from :installation-info to :payment-info"
    (let [p  (-> (production/create-new-production (id/build-id) user-id)
                 (production/register-producer-information "12 rue de la Paix" network-id))
          p' (production/register-installation-info p "PRM-123456" 9.0 :solar "LINKY-789")]
      (is (= :payment-info (:production/lifecycle p')))
      (is (= "PRM-123456" (:production/pdl-prm p')))
      (is (= 9.0 (:production/installed-power p')))
      (is (= :solar (:production/energy-type p')))
      (is (= "LINKY-789" (:production/linky-meter p')))))

  (testing "throws when not in :installation-info state"
    (let [p (production/build-production (assoc valid-attrs :production/lifecycle :payment-info))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (production/register-installation-info p "PRM-123" 9.0 :solar "LK-1"))))))

;; ── submit-payment-info ────────────────────────────────────────────────────

(deftest submit-payment-info-test
  (testing "transitions from :payment-info to :contract-signature"
    (let [p  (-> (production/create-new-production (id/build-id) user-id)
                 (production/register-producer-information "addr" network-id)
                 (production/register-installation-info "PRM-123" 9.0 :solar "LK-1"))
          p' (production/submit-payment-info p "FR76 3000 6000 0112 3456 7890 189")]
      (is (= :contract-signature (:production/lifecycle p')))
      (is (= "FR76 3000 6000 0112 3456 7890 189" (:production/iban p')))))

  (testing "throws when not in :payment-info state"
    (let [p (production/create-new-production (id/build-id) user-id)]
      (is (thrown? clojure.lang.ExceptionInfo
                  (production/submit-payment-info p "FR76 ..."))))))

;; ── sign-contract ──────────────────────────────────────────────────────────

(deftest sign-contract-test
  (testing "signing the adhesion contract transitions to :pending"
    (let [p  (-> (production/create-new-production (id/build-id) user-id)
                 (production/register-producer-information "addr" network-id)
                 (production/register-installation-info "PRM-123" 9.0 :solar "LK-1")
                 (production/submit-payment-info "FR76 3000 6000 0112 3456 7890 189"))
          p' (production/sign-contract p "2026-03-13T10:00:00Z")]
      (is (= :pending (:production/lifecycle p')))
      (is (= "2026-03-13T10:00:00Z" (:production/adhesion-signed-at p')))))

  (testing "throws when not in :contract-signature state"
    (let [p (production/create-new-production (id/build-id) user-id)]
      (is (thrown? clojure.lang.ExceptionInfo
                  (production/sign-contract p "2026-03-13T10:00:00Z"))))))

;; ── find-by-network-id (repository) ──────────────────────────────────────

(deftest ^:phase-1 find-by-network-id-test
  (let [repo       (mem-repo/->InMemoryProductionRepo (atom {}))
        net-id-a   (id/build-id)
        net-id-b   (id/build-id)
        ;; Create two active productions for network A
        prod-a1    (-> (production/create-new-production (id/build-id) user-id)
                       (production/register-producer-information "addr A1" net-id-a)
                       (production/register-installation-info "PRM-A1" 3.0 :solar "LK-A1")
                       (production/submit-payment-info "FR76 0000 0000 0000 0000 0000 001")
                       (production/sign-contract "2026-01-01T00:00:00Z"))
        prod-a2    (-> (production/create-new-production (id/build-id) user-id)
                       (production/register-producer-information "addr A2" net-id-a)
                       (production/register-installation-info "PRM-A2" 5.0 :wind "LK-A2")
                       (production/submit-payment-info "FR76 0000 0000 0000 0000 0000 002")
                       (production/sign-contract "2026-01-02T00:00:00Z"))
        ;; One production for network B
        prod-b1    (-> (production/create-new-production (id/build-id) user-id)
                       (production/register-producer-information "addr B1" net-id-b)
                       (production/register-installation-info "PRM-B1" 2.0 :hydro "LK-B1")
                       (production/submit-payment-info "FR76 0000 0000 0000 0000 0000 003")
                       (production/sign-contract "2026-01-03T00:00:00Z"))]
    ;; Save all productions
    (production/save! repo prod-a1)
    (production/save! repo prod-a2)
    (production/save! repo prod-b1)

    (testing "returns all productions for network A"
      (let [results (production/find-by-network-id repo net-id-a)]
        (is (= 2 (count results)))
        (is (every? #(= net-id-a (:production/network-id %)) results))))

    (testing "returns all productions for network B"
      (let [results (production/find-by-network-id repo net-id-b)]
        (is (= 1 (count results)))
        (is (= net-id-b (:production/network-id (first results))))))

    (testing "returns empty vector for unknown network"
      (let [results (production/find-by-network-id repo (id/build-id))]
        (is (vector? results))
        (is (empty? results))))))
