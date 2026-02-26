(ns domain.id
  (:require
    [clj-uuid :as uuid]))

; identity
; This is id implementation

(defn build-id
  "Generate a new random UUID or parse one from a string."
  ([] (uuid/v4))
  ([uuid-str] (uuid/as-uuid uuid-str)))

(defn id-str
  "Generate a new random UUID as a string."
  [] (str (build-id)))

(defn id?
  "Check if x is a valid UUID or can be parsed as one."
  [x] (uuid/uuidable? x))
