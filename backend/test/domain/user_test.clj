(ns domain.user-test
  (:require [clojure.test :refer [deftest is testing]]
            [domain.id :as id]
            [domain.user :as user]))

(def valid-attrs
  {:user/id           (id/build-id)
   :user/email        "alice@example.com"
   :user/name         "Alice"
   :user/provider     :google
   :user/provider-subject-identifier "google-12345"
   :user/role         :customer
   :user/lifecycle    :alive})

(deftest build-user-valid
  (testing "builds a user with valid attributes"
    (let [u (user/build-user valid-attrs)]
      (is (= "alice@example.com" (:user/email u)))
      (is (= "Alice" (:user/name u)))
      (is (= :google (:user/provider u))))))

(deftest build-user-validates-all-fields
  (testing "throws when role is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (dissoc valid-attrs :user/role)))))

  (testing "throws when lifecycle is missing"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/build-user (dissoc valid-attrs :user/lifecycle))))))

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

(deftest create-email-user-test
  (testing "creates an email user with email-verified? false"
    (let [uid (id/build-id)
          u   (user/create-email-user uid "bob@example.com" "Bob" "$2a$hash")]
      (is (= :email (:user/provider u)))
      (is (= false (:user/email-verified? u)))
      (is (= "$2a$hash" (:user/password-hash u)))
      (is (= :customer (:user/role u)))))

  (testing "throws on invalid email"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/create-email-user (id/build-id) "bad" "Bob" "hash"))))

  (testing "throws on blank name"
    (is (thrown? clojure.lang.ExceptionInfo
                (user/create-email-user (id/build-id) "bob@example.com" "" "hash")))))

(deftest email-verified?-test
  (testing "returns true for OAuth users (no email-verified? key)"
    (is (user/email-verified? (user/build-user valid-attrs))))

  (testing "returns false for email user not yet verified"
    (let [u (user/create-email-user (id/build-id) "bob@example.com" "Bob" "hash")]
      (is (not (user/email-verified? u))))))

(deftest verify-email-test
  (testing "marks user as verified"
    (let [u  (user/create-email-user (id/build-id) "bob@example.com" "Bob" "hash")
          u' (user/verify-email u)]
      (is (true? (:user/email-verified? u'))))))

(deftest assert-email-verified-test
  (testing "returns user if verified"
    (let [u (user/verify-email (user/create-email-user (id/build-id) "bob@example.com" "Bob" "hash"))]
      (is (= u (user/assert-email-verified u)))))

  (testing "throws if not verified"
    (let [u (user/create-email-user (id/build-id) "bob@example.com" "Bob" "hash")]
      (is (thrown? clojure.lang.ExceptionInfo
                  (user/assert-email-verified u))))))

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

