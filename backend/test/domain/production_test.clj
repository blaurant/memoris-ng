(ns domain.production-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.production :as production]))

(def user-id (id/build-id))

(def valid-attrs
  {:production/id        (id/build-id)
   :production/user-id   user-id
   :production/lifecycle :installation-info})

;; ── build-production ──────────────────────────────────────────────────────

(deftest build-production-valid
  (testing "builds a production with valid minimal attributes"
    (let [p (production/build-production valid-attrs)]
      (is (= :installation-info (:production/lifecycle p)))
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
  (testing "creates a production in :installation-info state"
    (let [pid (id/build-id)
          p   (production/create-new-production pid user-id)]
      (is (= pid (:production/id p)))
      (is (= user-id (:production/user-id p)))
      (is (= :installation-info (:production/lifecycle p)))))

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
  (testing "returns true for installation-info"
    (is (production/onboarding? (production/build-production valid-attrs))))

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

;; ── register-installation-info ────────────────────────────────────────────

(deftest register-installation-info-test
  (testing "transitions from :installation-info to :payment-info"
    (let [p  (production/create-new-production (id/build-id) user-id)
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
                 (production/register-installation-info "PRM-123" 9.0 :solar "LK-1")
                 (production/submit-payment-info "FR76 3000 6000 0112 3456 7890 189"))
          p' (production/sign-contract p "2026-03-13T10:00:00Z")]
      (is (= :pending (:production/lifecycle p')))
      (is (= "2026-03-13T10:00:00Z" (:production/adhesion-signed-at p')))))

  (testing "throws when not in :contract-signature state"
    (let [p (production/create-new-production (id/build-id) user-id)]
      (is (thrown? clojure.lang.ExceptionInfo
                  (production/sign-contract p "2026-03-13T10:00:00Z"))))))
