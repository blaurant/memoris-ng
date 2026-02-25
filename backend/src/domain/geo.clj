(ns domain.geo)

(def ^:private earth-radius-km 6371.0)

(defn haversine-distance
  "Returns the distance in km between two geographic points
  expressed as (lat1, lng1) and (lat2, lng2) in decimal degrees."
  [lat1 lng1 lat2 lng2]
  (let [to-rad  #(* % (/ Math/PI 180.0))
        dlat    (to-rad (- lat2 lat1))
        dlng    (to-rad (- lng2 lng1))
        rlat1   (to-rad lat1)
        rlat2   (to-rad lat2)
        a       (+ (* (Math/sin (/ dlat 2)) (Math/sin (/ dlat 2)))
                   (* (Math/cos rlat1) (Math/cos rlat2)
                      (Math/sin (/ dlng 2)) (Math/sin (/ dlng 2))))
        c       (* 2 (Math/atan2 (Math/sqrt a) (Math/sqrt (- 1 a))))]
    (* earth-radius-km c)))

(defn within-network?
  "Returns true if the point (lat, lng) lies within the network's radius."
  [network lat lng]
  (let [dist (haversine-distance (:network/center-lat network)
                                 (:network/center-lng network)
                                 lat lng)]
    (<= dist (:network/radius-km network))))
