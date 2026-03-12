(ns domain.consumption-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.consumption :as consumption]))

(def user-id (id/build-id))

(def valid-attrs
  {:consumption/id        (id/build-id)
   :consumption/user-id   user-id
   :consumption/lifecycle :consumer-information})

;; ── build-consumption ──────────────────────────────────────────────────────

(deftest build-consumption-valid
  (testing "builds a consumption with valid minimal attributes"
    (let [c (consumption/build-consumption valid-attrs)]
      (is (= :consumer-information (:consumption/lifecycle c)))
      (is (= user-id (:consumption/user-id c))))))

(deftest build-consumption-invalid
  (testing "throws when id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (dissoc valid-attrs :consumption/id)))))

  (testing "throws when user-id is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (dissoc valid-attrs :consumption/user-id)))))

  (testing "throws when lifecycle is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (dissoc valid-attrs :consumption/lifecycle)))))

  (testing "throws on invalid lifecycle"
    (is (thrown? clojure.lang.ExceptionInfo
                (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :invalid))))))

;; ── create-new-consumption ──────────────────────────────────────────────────

(deftest create-new-consumption-test
  (testing "creates a consumption in :consumer-information state"
    (let [cid (id/build-id)
          c   (consumption/create-new-consumption cid user-id)]
      (is (= cid (:consumption/id c)))
      (is (= user-id (:consumption/user-id c)))
      (is (= :consumer-information (:consumption/lifecycle c)))))

  (testing "throws when id is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid consumption"
                         (consumption/create-new-consumption "not-a-uuid" user-id))))

  (testing "throws when user-id is invalid"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid consumption"
                         (consumption/create-new-consumption (id/build-id) "not-a-uuid"))))

  (testing "throws when id is nil"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid consumption"
                         (consumption/create-new-consumption nil user-id))))

  (testing "throws when user-id is nil"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Invalid consumption"
                         (consumption/create-new-consumption (id/build-id) nil)))))

;; ── onboarding? ────────────────────────────────────────────────────────────

(deftest onboarding?-test
  (testing "returns true for consumer-information"
    (is (consumption/onboarding? (consumption/build-consumption valid-attrs))))

  (testing "returns true for linky-reference"
    (is (consumption/onboarding?
          (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :linky-reference)))))

  (testing "returns true for billing-address"
    (is (consumption/onboarding?
          (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :billing-address)))))

  (testing "returns true for contract-signature"
    (is (consumption/onboarding?
          (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :contract-signature)))))

  (testing "returns false for pending"
    (is (not (consumption/onboarding?
               (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :pending))))))

  (testing "returns false for active"
    (is (not (consumption/onboarding?
               (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :active)))))))

;; ── register-consumer-information ──────────────────────────────────────────

(deftest register-consumer-information-test
  (testing "transitions from :consumer-information to :linky-reference"
    (let [c       (consumption/create-new-consumption (id/build-id) user-id)
          network (id/build-id)
          c'      (consumption/register-consumer-information c "123 rue de Paris" network)]
      (is (= :linky-reference (:consumption/lifecycle c')))
      (is (= "123 rue de Paris" (:consumption/consumer-address c')))
      (is (= network (:consumption/network-id c')))))

  (testing "throws when not in :consumer-information state"
    (let [c (consumption/build-consumption (assoc valid-attrs :consumption/lifecycle :linky-reference))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (consumption/register-consumer-information c "addr" (id/build-id)))))))

;; ── associate-linky-reference ───────────────────────────────────────────────

(deftest associate-linky-reference-test
  (testing "transitions from :linky-reference to :billing-address"
    (let [c  (-> (consumption/create-new-consumption (id/build-id) user-id)
                 (consumption/register-consumer-information "addr" (id/build-id)))
          c' (consumption/associate-linky-reference c "LINKY-12345")]
      (is (= :billing-address (:consumption/lifecycle c')))
      (is (= "LINKY-12345" (:consumption/linky-reference c')))))

  (testing "throws when not in :linky-reference state"
    (let [c (consumption/create-new-consumption (id/build-id) user-id)]
      (is (thrown? clojure.lang.ExceptionInfo
                  (consumption/associate-linky-reference c "LINKY-12345"))))))

;; ── complete-billing-address ────────────────────────────────────────────────

(deftest complete-billing-address-test
  (testing "transitions from :billing-address to :contract-signature"
    (let [c  (-> (consumption/create-new-consumption (id/build-id) user-id)
                 (consumption/register-consumer-information "addr" (id/build-id))
                 (consumption/associate-linky-reference "LINKY-12345"))
          c' (consumption/complete-billing-address c "456 avenue de Lyon")]
      (is (= :contract-signature (:consumption/lifecycle c')))
      (is (= "456 avenue de Lyon" (:consumption/billing-address c')))))

  (testing "throws when not in :billing-address state"
    (let [c (consumption/create-new-consumption (id/build-id) user-id)]
      (is (thrown? clojure.lang.ExceptionInfo
                  (consumption/complete-billing-address c "456 avenue de Lyon"))))))

;; ── sign-contract ─────────────────────────────────────────────────────────

(deftest sign-contract-test
  (testing "signing first contract stays in :contract-signature"
    (let [c  (-> (consumption/create-new-consumption (id/build-id) user-id)
                 (consumption/register-consumer-information "addr" (id/build-id))
                 (consumption/associate-linky-reference "LINKY-12345")
                 (consumption/complete-billing-address "456 avenue de Lyon"))
          c' (consumption/sign-contract c :elinkco "2026-03-04T10:00:00Z")]
      (is (= :contract-signature (:consumption/lifecycle c')))
      (is (= "2026-03-04T10:00:00Z" (:consumption/contract-signed-at c')))))

  (testing "signing second contract stays in :contract-signature"
    (let [c  (-> (consumption/create-new-consumption (id/build-id) user-id)
                 (consumption/register-consumer-information "addr" (id/build-id))
                 (consumption/associate-linky-reference "LINKY-12345")
                 (consumption/complete-billing-address "456 avenue de Lyon"))
          c' (-> c
                 (consumption/sign-contract :elinkco "2026-03-04T10:00:00Z")
                 (consumption/sign-contract :producer "2026-03-04T10:01:00Z"))]
      (is (= :contract-signature (:consumption/lifecycle c')))
      (is (= "2026-03-04T10:01:00Z" (:consumption/producer-contract-signed-at c')))))

  (testing "signing all 3 contracts transitions to :pending"
    (let [c  (-> (consumption/create-new-consumption (id/build-id) user-id)
                 (consumption/register-consumer-information "addr" (id/build-id))
                 (consumption/associate-linky-reference "LINKY-12345")
                 (consumption/complete-billing-address "456 avenue de Lyon"))
          c' (-> c
                 (consumption/sign-contract :elinkco "2026-03-04T10:00:00Z")
                 (consumption/sign-contract :producer "2026-03-04T10:01:00Z")
                 (consumption/sign-contract :sepa "2026-03-04T10:02:00Z"))]
      (is (= :pending (:consumption/lifecycle c')))
      (is (= "2026-03-04T10:00:00Z" (:consumption/contract-signed-at c')))
      (is (= "2026-03-04T10:01:00Z" (:consumption/producer-contract-signed-at c')))
      (is (= "2026-03-04T10:02:00Z" (:consumption/sepa-mandate-signed-at c')))))

  (testing "throws when not in :contract-signature state"
    (let [c (consumption/create-new-consumption (id/build-id) user-id)]
      (is (thrown? clojure.lang.ExceptionInfo
                  (consumption/sign-contract c :elinkco "2026-03-04T10:00:00Z"))))))
