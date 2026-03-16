(ns app.utils.google-maps
  (:require [app.config :as config]))

(defn load-google-maps-script!
  "Injects the Google Maps JS API <script> tag once into the document head.
  Calls on-ready once the API is fully loaded."
  [on-ready]
  (when (empty? config/GOOGLE_MAPS_API_KEY)
    (js/console.warn "GOOGLE_MAPS_API_KEY is not set — map will not load."))
  (cond
    (and (exists? js/google) (exists? js/google.maps))
    (on-ready)

    (.querySelector js/document "script[data-maps]")
    ;; Script already injected but API not ready yet — poll until available.
    (let [poll (fn poll []
                (if (and (exists? js/google) (exists? js/google.maps))
                  (on-ready)
                  (js/setTimeout poll 200)))]
      (poll))

    :else
    (let [script (.createElement js/document "script")]
      (set! js/initGoogleMap (fn []
                               (set! js/initGoogleMap js/undefined)
                               (on-ready)))
      (set! (.-src script)
            (str "https://maps.googleapis.com/maps/api/js?key="
                 config/GOOGLE_MAPS_API_KEY
                 "&callback=initGoogleMap"))
      (.setAttribute script "data-maps" "true")
      (set! (.-async script) true)
      (.appendChild (.-head js/document) script))))

(defn draw-circle!
  "Draws a single Google Maps circle and returns the Circle instance.
  `opts` is a map with keys :center-lat, :center-lng, :radius-km,
  and optional :stroke-color, :stroke-weight, :fill-color, :fill-opacity."
  [gmap {:keys [center-lat center-lng radius-km
                stroke-color stroke-weight fill-color fill-opacity]
         :or   {stroke-color  "#2e7d32"
                stroke-weight 2
                fill-color    "#4caf50"
                fill-opacity  0.2}}]
  (js/google.maps.Circle.
   #js {:map          gmap
        :center       #js {:lat center-lat :lng center-lng}
        :radius       (* radius-km 1000)
        :strokeColor  stroke-color
        :strokeWeight stroke-weight
        :fillColor    fill-color
        :fillOpacity  fill-opacity}))

(defn clear-overlays!
  "Removes all overlays from the map and resets the atom to []."
  [overlays-atom]
  (doseq [o @overlays-atom] (.setMap o nil))
  (reset! overlays-atom []))
