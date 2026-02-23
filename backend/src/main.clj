(ns main
  (:gen-class)
  (:require [system]))

(defn -main
  "Entry point for the Memoris-NG backend application."
  [& _args]
  (system/start))
