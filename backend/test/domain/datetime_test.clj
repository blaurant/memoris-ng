(ns domain.datetime-test
  (:require [clojure.test :refer :all]
            [domain.datetime :as dt]
            [tick.core :as t]))

(deftest datetime?-test
  (testing "tick date-time is recognized"
    (is (true? (dt/datetime? (t/date-time "2026-03-12T10:00:00")))))
  (testing "string is not a date-time"
    (is (false? (dt/datetime? "2026-03-12T10:00:00"))))
  (testing "nil is not a date-time"
    (is (false? (dt/datetime? nil)))))

(deftest now-test
  (testing "now returns a date-time"
    (is (dt/datetime? (dt/now)))))

(deftest build-datetime-test
  (testing "build from string"
    (let [dt (dt/build-datetime "2026-03-12T10:30:00")]
      (is (dt/datetime? dt))
      (is (= "2026-03-12T10:30" (str dt)))))
  (testing "build without args returns a date-time"
    (is (dt/datetime? (dt/build-datetime)))))

(deftest between-abs-test
  (testing "absolute duration between two date-times"
    (let [dt1 (dt/build-datetime "2026-03-12T10:00:00")
          dt2 (dt/build-datetime "2026-03-12T10:30:00")]
      (is (= (t/of-minutes 30) (dt/between-abs dt1 dt2)))
      (is (= (t/of-minutes 30) (dt/between-abs dt2 dt1))))))

(deftest within-x-minutes?-test
  (let [dt1 (dt/build-datetime "2026-03-12T10:00:00")
        dt2 (dt/build-datetime "2026-03-12T10:05:00")]
    (testing "within 10 minutes"
      (is (true? (dt/within-x-minutes? dt1 dt2 10))))
    (testing "not within 3 minutes"
      (is (false? (dt/within-x-minutes? dt1 dt2 3))))))

(deftest after-x-minutes?-test
  (let [dt1 (dt/build-datetime "2026-03-12T10:00:00")
        dt2 (dt/build-datetime "2026-03-12T10:30:00")]
    (testing "after 20 minutes"
      (is (true? (dt/after-x-minutes? dt1 dt2 20))))
    (testing "not after 40 minutes"
      (is (false? (dt/after-x-minutes? dt1 dt2 40))))))

(deftest date?-test
  (testing "tick date is recognized"
    (is (true? (dt/date? (t/date "2026-03-12")))))
  (testing "string is not a date"
    (is (false? (dt/date? "2026-03-12")))))

(deftest today-test
  (testing "today returns a date"
    (is (dt/date? (dt/today)))))

(deftest build-date-test
  (testing "build from string"
    (let [d (dt/build-date "2026-03-12")]
      (is (dt/date? d))
      (is (= "2026-03-12" (str d)))))
  (testing "build without args returns a date"
    (is (dt/date? (dt/build-date)))))

(deftest date-fr-test
  (testing "parse French date format"
    (let [d (dt/date-fr "12/03/2026")]
      (is (= "2026-03-12" (str d))))))
