(ns app.consumptions.utils)

(defn latest-monthly-kwh
  "Extract the most recent month's kWh from a consumption's monthly-history."
  [consumption]
  (when-let [h (seq (:consumption/monthly-history consumption))]
    (:kwh (first (sort-by (juxt :year :month) #(compare %2 %1) h)))))
