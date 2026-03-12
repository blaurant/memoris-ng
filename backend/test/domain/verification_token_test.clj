(ns domain.verification-token-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.verification-token :as vt])
  (:import (java.time Instant)))

(def valid-attrs
  {:verification-token/id         (id/build-id)
   :verification-token/user-id    (id/build-id)
   :verification-token/token      "abc123"
   :verification-token/expires-at (java.util.Date.)})

(deftest build-verification-token-test
  (testing "builds a valid token"
    (let [t (vt/build-verification-token valid-attrs)]
      (is (= "abc123" (:verification-token/token t)))))

  (testing "throws on missing fields"
    (is (thrown? clojure.lang.ExceptionInfo
                (vt/build-verification-token (dissoc valid-attrs :verification-token/token))))))

(deftest expired?-test
  (testing "returns false for future expiration"
    (let [future-date (java.util.Date. (+ (System/currentTimeMillis) 100000))
          t (vt/build-verification-token (assoc valid-attrs :verification-token/expires-at future-date))]
      (is (not (vt/expired? t (Instant/now))))))

  (testing "returns true for past expiration"
    (let [past-date (java.util.Date. (- (System/currentTimeMillis) 100000))
          t (vt/build-verification-token (assoc valid-attrs :verification-token/expires-at past-date))]
      (is (vt/expired? t (Instant/now))))))

(deftest make-token-test
  (testing "creates a token with 24h expiration"
    (let [t (vt/make-1d-token (id/build-id) (id/build-id) "token-str")]
      (is (= "token-str" (:verification-token/token t)))
      (is (not (vt/expired? t (Instant/now)))))))
