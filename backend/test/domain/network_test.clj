(ns domain.network-test
  (:require [clojure.test :refer :all]
            [domain.id :as id]
            [domain.network :as network]))

(deftest build-network-test
  (testing "valid network with defaults"
    (let [n (network/build-network {:network/id         (id/build-id)
                                    :network/name       "Test"
                                    :network/center-lat 48.0
                                    :network/center-lng 2.0})]
      (is (= 1.0 (:network/radius-km n)))
      (is (= :private (:network/lifecycle n)))))

  (testing "valid network with explicit lifecycle"
    (let [n (network/build-network {:network/id         (id/build-id)
                                    :network/name       "Test"
                                    :network/center-lat 48.0
                                    :network/center-lng 2.0
                                    :network/lifecycle  :public})]
      (is (= :public (:network/lifecycle n)))))

  (testing "invalid network throws"
    (is (thrown? clojure.lang.ExceptionInfo
                (network/build-network {:network/id (id/build-id)})))))

(deftest publish-test
  (testing "private -> public"
    (let [n (network/build-network {:network/id         (id/build-id)
                                    :network/name       "Test"
                                    :network/center-lat 48.0
                                    :network/center-lng 2.0})
          published (network/publish n)]
      (is (= :public (:network/lifecycle published)))))

  (testing "already public throws"
    (let [n (network/build-network {:network/id         (id/build-id)
                                    :network/name       "Test"
                                    :network/center-lat 48.0
                                    :network/center-lng 2.0
                                    :network/lifecycle  :public})]
      (is (thrown? clojure.lang.ExceptionInfo (network/publish n))))))

(deftest unpublish-test
  (testing "public -> private"
    (let [n (network/build-network {:network/id         (id/build-id)
                                    :network/name       "Test"
                                    :network/center-lat 48.0
                                    :network/center-lng 2.0
                                    :network/lifecycle  :public})
          unpublished (network/unpublish n)]
      (is (= :private (:network/lifecycle unpublished)))))

  (testing "already private throws"
    (let [n (network/build-network {:network/id         (id/build-id)
                                    :network/name       "Test"
                                    :network/center-lat 48.0
                                    :network/center-lng 2.0})]
      (is (thrown? clojure.lang.ExceptionInfo (network/unpublish n))))))
