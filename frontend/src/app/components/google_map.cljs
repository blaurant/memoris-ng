(ns app.components.google-map
  (:require [app.utils.google-maps :as google-maps]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfee]))

(defn- draw-circles!
  "Removes previous circles and draws one per network on the map.
  Each circle is clickable and navigates to the network detail page."
  [gmap circles-atom networks]
  (google-maps/clear-overlays! circles-atom)
  (reset! circles-atom
          (mapv (fn [net]
                  (let [circle (google-maps/draw-circle!
                                gmap
                                {:center-lat (:network/center-lat net)
                                 :center-lng (:network/center-lng net)
                                 :radius-km  (:network/radius-km net)})]
                    (.addListener circle "click"
                      (fn [_]
                        (rfee/push-state :page/network-detail {:id (:network/id net)})))
                    circle))
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
        (google-maps/load-google-maps-script!
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
        (google-maps/clear-overlays! circles))

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
