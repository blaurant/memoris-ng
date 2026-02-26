(ns app.components.eligibility-form
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- draw-network-circles!
  "Draws a circle for each network on the given map."
  [gmap circles-atom networks]
  (doseq [c @circles-atom] (.setMap ^js c nil))
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

(defn- eligibility-mini-map
  "Form-3 component: small Google Map centered on the checked address,
  with network circles overlaid."
  [lat lng _networks]
  (let [map-el   (atom nil)
        map-inst (atom nil)
        marker   (atom nil)
        circles  (atom [])]
    (r/create-class
     {:display-name "eligibility-mini-map"

      :component-did-mount
      (fn [this]
        (when (and (exists? js/google) @map-el)
          (let [[_ _ _ networks] (r/argv this)
                center #js {:lat lat :lng lng}
                gmap   (js/google.maps.Map.
                         @map-el
                         #js {:center center :zoom 9 :mapTypeId "roadmap"})]
            (reset! map-inst gmap)
            (reset! marker (js/google.maps.Marker. #js {:map gmap :position center}))
            (draw-network-circles! gmap circles networks))))

      :component-did-update
      (fn [this _]
        (let [[_ new-lat new-lng networks] (r/argv this)
              center #js {:lat new-lat :lng new-lng}]
          (when-let [gmap @map-inst]
            (.setCenter gmap center)
            (when @marker (.setMap ^js @marker nil))
            (reset! marker (js/google.maps.Marker. #js {:map gmap :position center}))
            (draw-network-circles! gmap circles networks))))

      :component-will-unmount
      (fn [_]
        (doseq [c @circles] (.setMap ^js c nil)))

      :reagent-render
      (fn [_ _ _]
        [:div.map-container
         {:ref   (fn [el] (when el (reset! map-el el)))
          :style {:margin-top "1rem" :width "100%" :aspect-ratio "4/3"
                  :min-height "250px" :max-height "450px"}}])})))

(defn- geocode-address!
  "Uses the Google Maps Geocoder to convert address string to lat/lng,
  then dispatches :eligibility/check with the result."
  [address on-error]
  (if-not (exists? js/google)
    (on-error "Google Maps n'est pas encore chargé. Veuillez patienter.")
    (let [geocoder (js/google.maps.Geocoder.)]
      (-> (.geocode geocoder #js {:address address})
          (.then (fn [^js response]
                   (let [results (.-results response)
                         ^js first-result (when (and results (pos? (.-length results)))
                                           (aget results 0))
                         ^js loc (some-> first-result .-geometry .-location)]
                     (if loc
                       (rf/dispatch [:eligibility/check {:lat (.lat loc) :lng (.lng loc) :address address}])
                       (on-error "Aucun résultat pour cette adresse.")))))
          (.catch (fn [_err]
                    (on-error "Adresse introuvable.")))))))

(defn eligibility-form
  "Form-2 component that geocodes an address and checks eligibility."
  []
  (let [input-val (r/atom "")
        geo-error (r/atom nil)]
    (fn []
      (let [result   @(rf/subscribe [:eligibility/result])
            loading? @(rf/subscribe [:eligibility/loading?])
            address  @(rf/subscribe [:eligibility/address])
            lat      @(rf/subscribe [:eligibility/lat])
            lng      @(rf/subscribe [:eligibility/lng])
            networks @(rf/subscribe [:networks/list])]
        [:div.eligibility-form
         [:div.eligibility-form__group
          [:input.eligibility-form__input
           {:type        "text"
            :placeholder "Entrez votre adresse…"
            :value       @input-val
            :on-change   #(do (reset! input-val (-> % .-target .-value))
                              (reset! geo-error nil))
            :on-key-down #(when (= (.-key %) "Enter")
                            (geocode-address! @input-val (fn [err] (reset! geo-error err))))}]
          [:button.btn.btn--green
           {:on-click #(geocode-address! @input-val (fn [err] (reset! geo-error err)))
            :disabled loading?}
           (if loading? "Vérification…" "Vérifier")]]

         (when @geo-error
           [:p {:style {:color "var(--color-error)" :font-size "0.9rem"}} @geo-error])

         (when result
           (let [eligible? (:eligible? result)
                 network   (:network result)]
             [:<>
              [:div {:class (if eligible?
                              "eligibility-result eligibility-result--ok"
                              "eligibility-result eligibility-result--ko")}
               (if eligible?
                 [:span "Bonne nouvelle — " address " est éligible au réseau "
                  [:strong (:network/name network)] " !"]
                 [:span address " n'est pas dans la zone d'un réseau Wattprox pour le moment."])]
              (when (and lat lng)
                [eligibility-mini-map lat lng networks])]))]))))
