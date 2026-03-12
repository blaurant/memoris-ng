(ns domain.datetime
  (:require [tick.core :as t])
  (:import [java.time.format DateTimeFormatter]
           [java.util Locale]
           [java.time LocalDate]))

;; All datetime operations work in UTC.

;; ── Datetime ────────────────────────────────────────────────────────────────────

(defn datetime?
  "Returns true if x is a tick date-time."
  [x]
  (t/date-time? x))

(defn now
  "Returns the current UTC date-time."
  []
  (-> (t/instant)
      (t/in "UTC")
      (t/date-time)))

(defn build-datetime
  "Build a date-time from nothing (now) or from a string."
  ([] (t/date-time))
  ([s] (t/date-time s)))

(defn between-abs
  "Returns the absolute duration between two date-times."
  [dt1 dt2]
  (if (t/> dt2 dt1)
    (t/between dt1 dt2)
    (t/between dt2 dt1)))

(defn within-x-minutes?
  "Returns true if dt1 and dt2 are within the given number of minutes."
  [dt1 dt2 duration]
  (t/<= (between-abs dt1 dt2) (t/of-minutes duration)))

(defn after-x-minutes?
  "Returns true if dt1 and dt2 are more than the given number of minutes apart."
  [dt1 dt2 duration]
  (t/> (between-abs dt1 dt2) (t/of-minutes duration)))

;; ── Date ────────────────────────────────────────────────────────────────────────

(defn date?
  "Returns true if x is a tick date."
  [x]
  (t/date? x))

(defn today
  "Returns the current UTC date."
  []
  (-> (t/instant)
      (t/in "UTC")
      (t/date)))

(defn build-date
  "Build a date from nothing (today) or from a string."
  ([] (t/date))
  ([s] (t/date s)))

(def french-date-formatter
  (DateTimeFormatter/ofPattern "dd/MM/yyyy" (Locale/forLanguageTag "fr")))

(defn date-fr
  "Parse a French-formatted date string (dd/MM/yyyy)."
  [s]
  (LocalDate/parse s french-date-formatter))
