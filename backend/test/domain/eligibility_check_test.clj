(ns domain.eligibility-check-test
  (:require [clojure.test :refer :all]
            [domain.datetime :as dt]
            [domain.eligibility-check :as ec]
            [domain.id :as id]))

(deftest build-eligibility-check-test
  (testing "valid eligible check"
    (let [check (ec/build-eligibility-check
                  {:eligibility-check/id           (id/build-id)
                   :eligibility-check/address      "10 rue de Paris"
                   :eligibility-check/lat          48.8566
                   :eligibility-check/lng          2.3522
                   :eligibility-check/eligible?    true
                   :eligibility-check/network-name "Réseau Solaire"
                   :eligibility-check/checked-at   (dt/now)})]
      (is (true? (:eligibility-check/eligible? check)))
      (is (= "Réseau Solaire" (:eligibility-check/network-name check)))))

  (testing "valid non-eligible check"
    (let [check (ec/build-eligibility-check
                  {:eligibility-check/id           (id/build-id)
                   :eligibility-check/address      "nowhere"
                   :eligibility-check/lat          0.0
                   :eligibility-check/lng          0.0
                   :eligibility-check/eligible?    false
                   :eligibility-check/network-name nil
                   :eligibility-check/checked-at   (dt/now)})]
      (is (false? (:eligibility-check/eligible? check)))))

  (testing "invalid check throws"
    (is (thrown? clojure.lang.ExceptionInfo
                (ec/build-eligibility-check {:eligibility-check/id (id/build-id)})))))
