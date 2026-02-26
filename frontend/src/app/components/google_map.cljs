(ns app.components.google-map
  (:require [app.config :as config]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- load-google-maps-script!
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

(defn- draw-circles!
  "Removes previous circles and draws one per network on the map."
  [gmap circles-atom networks]
  (doseq [c @circles-atom] (.setMap c nil))
  (reset! circles-atom
          (mapv (fn [net]
                  (js/google.maps.Circle.
                   #js {:map          gmap
                        :center       #js {:lat (:network/center-lat net)
                                          :lng (:network/center-lng net)}
                        :radius       (* (:network/radius-km net) 1000)
                        :strokeColor  "#2e7d32"
                        :strokeWeight 2
                        :fillColor    "#4caf50"
                        :fillOpacity  0.2}))
                networks)))

(defn network-map
  "Reagent Form-3 component displaying a Google Map with one circle per network.
  The map div has NO React children — Google Maps owns that DOM entirely."
  []
  (let [map-el       (atom nil)
        map-inst     (r/atom nil)
        circles      (atom [])
        networks-sub (rf/subscribe [:networks/list])
        loading-sub  (rf/subscribe [:networks/loading?])]
    (r/create-class
     {:display-name "network-map"

      :component-did-mount
      (fn [_this]
        (load-google-maps-script!
         (fn []
           (try
             (let [gmap (js/google.maps.Map.
                          @map-el
                          #js {:center    #js {:lat 46.603354 :lng 1.888334}
                               :zoom      6
                               :mapTypeId "roadmap"})]
               (reset! map-inst gmap)
               (draw-circles! gmap circles @networks-sub))
             (catch :default e
               (js/console.error "Google Maps init error:" e))))))

      :component-did-update
      (fn [_this _prev-argv]
        (when-let [gmap @map-inst]
          (draw-circles! gmap circles @networks-sub)))

      :component-will-unmount
      (fn [_this]
        (doseq [c @circles] (.setMap c nil)))

      :reagent-render
      (fn []
        (let [loading? @loading-sub
              networks @networks-sub
              ready?   (some? @map-inst)]
          ;; Map div is always first child so React never recreates it.
          ;; Placeholder comes after, so removing it does not shift the map div.
          [:div
           [:div.map-container
            {:ref   (fn [el] (when el (reset! map-el el)))
             :style {:width "100%" :aspect-ratio "4/3"
                     :min-height "300px" :max-height "640px"}}]
           (when (and (not ready?) (not loading?))
             [:p.map-placeholder "Chargement de la carte…"])]))})))
