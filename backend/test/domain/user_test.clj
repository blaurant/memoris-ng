(ns domain.user-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.user :as user]))

(def valid-attrs
  {:user/id           (id/build-id)
   :user/email        "alice@example.com"
   :user/name         "Alice"
   :user/provider     :google
   :user/provider-subject-identifier "google-12345"})

(deftest build-user-valid
  (testing "builds a user with valid attributes"
    (let [u (user/build-user valid-attrs)]
      (is (= "alice@example.com" (:user/email u)))
      (is (= "Alice" (:user/name u)))
      (is (= :google (:user/provider u))))))

(deftest build-user-defaults
  (testing "applies default values"
    (let [u (user/build-user valid-attrs)]
      (is (= :customer (:user/role u)))
      (is (= :alive (:user/lifecycle u)))))

  (testing "audit trail has a :created entry"
    (let [u (user/build-user valid-attrs)]
      (is (= 1 (count (:user/audit-trail u))))
      (is (= :created (-> u :user/audit-trail first :audit/action)))
      (is (inst? (-> u :user/audit-trail first :audit/timestamp))))))

(deftest build-user-invalid
  (testing "throws on missing required fields"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user {:user/id (id/build-id)}))))

  (testing "throws on invalid provider"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/provider :twitter)))))

  (testing "throws on invalid email"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/email "not-an-email"))))
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/email ""))))
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/email "missing@tld")))))

  (testing "throws on invalid name"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/name ""))))
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/name "   "))))
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (assoc valid-attrs :user/name (apply str (repeat 101 "a"))))))))

(deftest alive?-test
  (testing "returns true for alive user"
    (is (user/alive? (user/build-user valid-attrs))))

  (testing "returns false for suspended user"
    (is (not (user/alive? (user/suspend (user/build-user valid-attrs)))))))

(deftest suspend-test
  (testing "suspends an alive user"
    (let [u (user/suspend (user/build-user valid-attrs))]
      (is (= :suspended (:user/lifecycle u)))))

  (testing "throws when suspending a non-alive user"
    (let [suspended (user/suspend (user/build-user valid-attrs))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (user/suspend suspended))))))

(deftest deactivate-test
  (testing "deactivates an alive user"
    (let [u (user/deactivate (user/build-user valid-attrs))]
      (is (= :deactivated (:user/lifecycle u)))))

  (testing "deactivates a suspended user"
    (let [u (-> (user/build-user valid-attrs)
                user/suspend
                user/deactivate)]
      (is (= :deactivated (:user/lifecycle u)))))

  (testing "throws when deactivating an already deactivated user"
    (let [deactivated (user/deactivate (user/build-user valid-attrs))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (user/deactivate deactivated))))))

(deftest reactivate-test
  (testing "reactivates a suspended user"
    (let [u (-> (user/build-user valid-attrs)
                user/suspend
                user/reactivate)]
      (is (= :alive (:user/lifecycle u)))))

  (testing "throws when reactivating an alive user"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/reactivate (user/build-user valid-attrs)))))

  (testing "throws when reactivating a deactivated user"
    (let [deactivated (user/deactivate (user/build-user valid-attrs))]
      (is (thrown? clojure.lang.ExceptionInfo
                  (user/reactivate deactivated))))))

(deftest record-login-test
  (testing "appends a login entry to audit trail"
    (let [u (-> (user/build-user valid-attrs)
                (user/record-login :google))]
      (is (= 2 (count (:user/audit-trail u))))
      (is (= :created (-> u :user/audit-trail first :audit/action)))
      (is (= :login   (-> u :user/audit-trail second :audit/action)))
      (is (= :google  (-> u :user/audit-trail second :audit/info :provider)))
      (is (inst? (-> u :user/audit-trail second :audit/timestamp)))))

  (testing "appends multiple login entries"
    (let [u (-> (user/build-user valid-attrs)
                (user/record-login :google)
                (user/record-login :apple))]
      (is (= 3 (count (:user/audit-trail u))))
      (is (= :apple (-> u :user/audit-trail last :audit/info :provider))))))
