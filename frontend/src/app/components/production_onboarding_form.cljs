(ns app.components.production-onboarding-form
  (:require [app.productions.contract :as contract]
            [app.utils.google-maps :as google-maps]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def steps
  [{:key :producer-information :label "Adresse & Réseau" :number 1}
   {:key :installation-info    :label "Installation"     :number 2}
   {:key :payment-info         :label "Info. bancaire"   :number 3}
   {:key :contract-signature   :label "Contrat"          :number 4}])

(def ^:private step-order
  {:producer-information 0
   :installation-info    1
   :payment-info         2
   :contract-signature   3})

(defn- step-status [step-key current-step]
  (let [idx     (step-order step-key)
        current (step-order current-step)]
    (cond
      (< idx current) :completed
      (= idx current) :active
      :else           :pending)))

(defn- stepper [current-step]
  [:div.stepper
   (doall
     (for [{:keys [key label number]} steps]
       (let [status (step-status key current-step)]
         ^{:key key}
         [:div.stepper__item
          {:class (str "stepper__item--" (name status))}
          [:div.stepper__circle
           (if (= status :completed)
             [:svg {:width "14" :height "14" :viewBox "0 0 24 24"
                    :fill "none" :stroke "currentColor" :stroke-width "3"
                    :stroke-linecap "round" :stroke-linejoin "round"}
              [:polyline {:points "20 6 9 17 4 12"}]]
             (str number))]
          [:div.stepper__label label]])))])

;; ── Choose network modal ──────────────────────────────────────────────────

(defn- draw-network-circles!
  "Draw clickable circles for each network on the map."
  [gm circles-atom networks selected-network no-network? creating?]
  (doseq [c @circles-atom] (.setMap ^js c nil))
  (reset! circles-atom
    (mapv (fn [net]
            (let [circle (js/google.maps.Circle.
                           #js {:map          gm
                                :center       #js {:lat (:network/center-lat net)
                                                   :lng (:network/center-lng net)}
                                :radius       (* (:network/radius-km net) 1000)
                                :strokeColor  "#2e7d32"
                                :strokeWeight 2
                                :fillColor    "#4caf50"
                                :fillOpacity  0.2
                                :clickable    true})]
              (.addListener circle "click"
                (fn [_e]
                  (doseq [c @circles-atom]
                    (.setOptions ^js c #js {:fillOpacity 0.2 :strokeWeight 2}))
                  (.setOptions circle #js {:fillOpacity 0.5 :strokeWeight 3})
                  (reset! selected-network net)
                  (reset! no-network? false)
                  (reset! creating? false)))
              circle))
          networks)))

(defn- check-in-any-network?
  "Returns true if (lat, lng) falls inside at least one network circle."
  [lat lng networks]
  (some (fn [net]
          (let [dlat (- lat (:network/center-lat net))
                dlng (- lng (:network/center-lng net))
                cos-lat (js/Math.cos (* lat (/ js/Math.PI 180)))
                dist-km (js/Math.sqrt (+ (* dlat dlat 111.32 111.32)
                                          (* dlng dlng 111.32 111.32 cos-lat cos-lat)))]
            (<= dist-km (:network/radius-km net))))
        networks))

(defn- choose-network-modal
  "Modal with map centered on address. User clicks a network circle to select,
   or if no network covers their location, they can propose a new one."
  [address networks on-select-existing on-create-new on-cancel]
  (let [map-el           (atom nil)
        map-inst         (atom nil)
        circles          (atom [])
        selected-network (r/atom nil)
        addr-lat         (atom nil)
        addr-lng         (atom nil)
        no-network?      (r/atom false)
        creating?        (r/atom false)
        net-name         (r/atom "")
        geocoding?       (r/atom false)
        geo-error        (r/atom nil)]
    (r/create-class
     {:display-name "prod-choose-network-modal"

      :component-did-mount
      (fn [_this]
        (let [create-map!
              (fn [center zoom]
                (when @map-el
                  (let [gm (js/google.maps.Map.
                             @map-el
                             #js {:center center :zoom zoom :mapTypeId "roadmap"})]
                    (reset! map-inst gm)
                    (js/google.maps.Marker.
                      #js {:map gm :position center :title "Votre adresse"})
                    (draw-network-circles! gm circles networks
                                           selected-network no-network? creating?))))
              init-map!
              (fn []
                (let [geocoder (js/google.maps.Geocoder.)]
                  (if (seq address)
                    (-> (.geocode geocoder #js {:address address})
                        (.then (fn [^js response]
                                 (let [results     (.-results response)
                                       ^js first-r (when (and results (pos? (.-length results)))
                                                     (aget results 0))
                                       ^js loc     (some-> first-r .-geometry .-location)]
                                   (if loc
                                     (do
                                       (reset! addr-lat (.lat loc))
                                       (reset! addr-lng (.lng loc))
                                       (create-map! #js {:lat (.lat loc) :lng (.lng loc)} 10)
                                       (when-not (check-in-any-network? (.lat loc) (.lng loc) networks)
                                         (reset! no-network? true)))
                                     (create-map! #js {:lat 46.6 :lng 1.9} 6)))))
                        (.catch (fn [_err]
                                  (create-map! #js {:lat 46.6 :lng 1.9} 6))))
                    (create-map! #js {:lat 46.6 :lng 1.9} 6))))]
          (if (and (exists? js/google) (exists? js/google.maps))
            (init-map!)
            (google-maps/load-google-maps-script! init-map!))))

      :component-will-unmount
      (fn [_this]
        (doseq [c @circles] (.setMap ^js c nil)))

      :reagent-render
      (fn [_address _networks _on-select-existing on-create-new on-cancel]
        (let [sel @selected-network]
          [:div.modal-overlay {:on-click (fn [e]
                                           (when (= (.-target e) (.-currentTarget e))
                                             (on-cancel)))}
           [:div.modal {:style {:max-width "650px"}}
            [:div.modal__header
             [:span "Choisir un réseau"]
             [:button.btn.btn--small
              {:on-click on-cancel
               :style {:background "transparent" :color "var(--color-muted)"
                       :border "none" :font-size "1.2rem" :padding "0"}}
              "\u00D7"]]
            [:div.modal__map
             {:ref (fn [el] (when el (reset! map-el el)))}]
            ;; Selection feedback
            (cond
              sel
              [:div.modal__selection
               {:style {:color "var(--color-green)" :font-weight "600"}}
               (str "Réseau sélectionné : " (:network/name sel))]

              (and @no-network? (not @creating?))
              [:div.modal__selection
               {:style {:color "#e65100"}}
               "Pas de réseau dans votre zone."
               [:button.btn.btn--small.btn--outline
                {:on-click #(reset! creating? true)
                 :style    {:margin-left "0.75rem"}}
                "Proposer un nouveau réseau"]]

              @creating?
              [:div {:style {:padding "0.5rem 1rem"}}
               [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-right "8px"}}
                "Nom du nouveau réseau"]
               [:input.onboarding__input
                {:value       @net-name
                 :placeholder "Ex: Réseau Montpellier Sud"
                 :on-change   #(reset! net-name (.-value (.-target %)))
                 :style       {:margin-top "4px"}}]
               [:p {:style {:font-size "0.8rem" :color "var(--color-muted)" :margin-top "4px"}}
                "Le réseau sera centré sur votre adresse (rayon : 1 km) et soumis à validation. "
                "La taille et la localisation du réseau pourront être ajustées par l'administrateur. "
                "Pour les réseaux périurbains (rayon 10 km) et ruraux (rayon 20 km), une validation Enedis sera également requise."]
               (when @geo-error
                 [:p {:style {:font-size "0.85rem" :color "var(--color-error)" :margin-top "4px"}}
                  @geo-error])]

              :else
              [:div.modal__selection
               "Cliquez sur un réseau sur la carte pour le sélectionner."])
            ;; Actions
            [:div.modal__actions
             [:button.btn.btn--small {:on-click on-cancel} "Annuler"]
             (cond
               ;; Existing network selected
               sel
               [:button.btn.btn--small.btn--green
                {:on-click #(on-select-existing (:network/id sel))}
                "Valider"]

               ;; Creating new network — validate with name
               @creating?
               [:button.btn.btn--small.btn--green
                {:disabled (or (empty? @net-name) @geocoding?)
                 :on-click (fn []
                             (if (and @addr-lat @addr-lng)
                               (on-create-new {:network-name   @net-name
                                               :network-lat    @addr-lat
                                               :network-lng    @addr-lng
                                               :network-radius 1.0})
                               ;; Fallback geocode if lat/lng missing
                               (do
                                 (reset! geocoding? true)
                                 (reset! geo-error nil)
                                 (let [do-geocode!
                                       (fn []
                                         (let [geocoder (js/google.maps.Geocoder.)]
                                           (-> (.geocode geocoder #js {:address address})
                                               (.then
                                                 (fn [^js response]
                                                   (let [results     (.-results response)
                                                         ^js first-r (when (and results (pos? (.-length results)))
                                                                       (aget results 0))
                                                         ^js loc     (some-> first-r .-geometry .-location)]
                                                     (reset! geocoding? false)
                                                     (if loc
                                                       (on-create-new {:network-name   @net-name
                                                                       :network-lat    (.lat loc)
                                                                       :network-lng    (.lng loc)
                                                                       :network-radius 1.0})
                                                       (reset! geo-error "Impossible de géolocaliser cette adresse.")))))
                                               (.catch
                                                 (fn [_err]
                                                   (reset! geocoding? false)
                                                   (reset! geo-error "Erreur de géolocalisation."))))))]
                                   (if (and (exists? js/google) (exists? js/google.maps))
                                     (do-geocode!)
                                     (google-maps/load-google-maps-script! do-geocode!))))))}
                (if @geocoding? "Géolocalisation..." "Valider")])]]]))})))

;; ── Step 0: Producer information ──────────────────────────────────────────

(defn- step0-form [production-id production]
  (let [networks      @(rf/subscribe [:networks/list])
        init-nid      (:production/network-id production)
        address       (r/atom (or (:production/producer-address production) ""))
        selected-name (r/atom (:production/network-name production))
        selected-id   (r/atom init-nid)
        show-modal?   (r/atom false)]
    (fn []
      [:div.onboarding__form
       [:label "Adresse de production"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Votre adresse de production"
         :value       @address
         :on-change   (fn [e]
                        (reset! address (.. e -target -value))
                        (reset! selected-id nil)
                        (reset! selected-name nil))}]
       [:div {:style {:display "flex" :align-items "center" :gap "0.75rem" :margin-top "0.75rem"}}
        [:span {:style {:font-weight "600" :white-space "nowrap"}} "Réseau"]
        (if @selected-name
          [:span {:style {:color "var(--color-green)" :font-weight "600"}}
           @selected-name]
          [:span {:style {:color "var(--color-muted)" :font-style "italic"}}
           "Aucun réseau sélectionné"])
        [:button.btn.btn--small.btn--outline
         {:disabled (empty? @address)
          :on-click #(reset! show-modal? true)}
         "Choisir un réseau"]]
       [:div {:style {:display "flex" :justify-content "flex-end" :margin-top "0.75rem"}}
        [:button.btn.btn--green.btn--small
         {:disabled (or (empty? @address) (nil? @selected-id))
          :on-click #(rf/dispatch [:productions/submit-step0
                                    production-id @address
                                    {:network-id @selected-id}])}
         "Suivant"]]
       (when @show-modal?
         [choose-network-modal
          @address
          networks
          ;; on-select-existing
          (fn [nid]
            (reset! selected-id nid)
            (reset! selected-name
                    (:network/name (first (filter #(= nid (:network/id %)) networks))))
            (reset! show-modal? false))
          ;; on-create-new
          (fn [net-opts]
            (reset! show-modal? false)
            (rf/dispatch [:productions/submit-step0
                           production-id @address net-opts]))
          ;; on-cancel
          (fn []
            (reset! show-modal? false))])])))

;; ── Step 1: Installation info ──────────────────────────────────────────────

(def ^:private energy-types
  [{:value :solar         :label "Solaire"}
   {:value :wind          :label "Eolien"}
   {:value :hydro         :label "Hydraulique"}
   {:value :biomass       :label "Biomasse"}
   {:value :cogeneration  :label "Cogeneration"}])

(defn- step1-form [production-id production]
  (let [pdl-prm     (r/atom (or (:production/pdl-prm production) ""))
        power       (r/atom (if (:production/installed-power production)
                              (str (:production/installed-power production))
                              ""))
        energy-type (r/atom (if (:production/energy-type production)
                              (name (:production/energy-type production))
                              ""))
        linky-meter (r/atom (or (:production/linky-meter production) ""))]
    (fn []
      [:div.onboarding__form
       [:label "Numero PDL/PRM"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Ex: 12345678901234"
         :value       @pdl-prm
         :on-change   #(reset! pdl-prm (.. % -target -value))}]
       [:label "Puissance installee (kWc)"]
       [:input.onboarding__input
        {:type        "number"
         :step        "any"
         :placeholder "Ex: 9"
         :value       @power
         :on-change   #(reset! power (.. % -target -value))}]
       [:label "Type d'energie"]
       [:select.onboarding__select
        {:value     @energy-type
         :on-change #(reset! energy-type (.. % -target -value))}
        [:option {:value ""} "Choisir un type"]
        (doall
          (for [{:keys [value label]} energy-types]
            ^{:key value}
            [:option {:value (name value)} label]))]
       [:label "Numero de compteur Linky"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Ex: 09123456789012"
         :value       @linky-meter
         :on-change   #(reset! linky-meter (.. % -target -value))}]
       [:div {:style {:display "flex" :justify-content "space-between" :margin-top "0.75rem"}}
        [:button.btn.btn--small.btn--outline
         {:on-click #(rf/dispatch [:productions/go-back production-id])}
         "Précédent"]
        [:button.btn.btn--green.btn--small
         {:disabled (or (empty? @pdl-prm)
                        (empty? @power)
                        (empty? @energy-type)
                        (empty? @linky-meter))
          :on-click #(rf/dispatch [:productions/submit-step1
                                    production-id @pdl-prm @power
                                    (keyword @energy-type) @linky-meter])}
         "Suivant"]]])))

(defn- step2-form [production-id production]
  (let [iban (r/atom (or (:production/iban production) ""))]
    (fn []
      [:div.onboarding__form
       [:label "IBAN"]
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Ex: FR76 3000 6000 0112 3456 7890 189"
         :value       @iban
         :on-change   #(reset! iban (.. % -target -value))}]
       [:div {:style {:display "flex" :justify-content "space-between" :margin-top "0.75rem"}}
        [:button.btn.btn--small.btn--outline
         {:on-click #(rf/dispatch [:productions/go-back production-id])}
         "Précédent"]
        [:button.btn.btn--green.btn--small
         {:disabled (empty? @iban)
          :on-click #(rf/dispatch [:productions/submit-step2
                                    production-id @iban])}
         "Suivant"]]])))

(defn- contract-icon []
  [:svg {:width "24" :height "24" :viewBox "0 0 24 24"
         :fill "none" :stroke "currentColor" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

(defn- step3-form [production-id production]
  (let [show-contract? (r/atom false)]
    (fn [production-id _production]
      (let [user             @(rf/subscribe [:auth/user])
            adhesion-signed? (some? (:adhesion-signed-at user))]
        [:div.onboarding__form
         ;; Adhesion Elink-co — only if not yet signed by user
         (when-not adhesion-signed?
           [:div.contract-row
            [:div.contract-row__info
             [contract-icon]
             [:span.contract-row__label "Adhésion Elink-co"]]
            [:button.btn.btn--small.btn--outline
             {:on-click #(reset! show-contract? true)}
             "A signer"]])
         ;; Finalisation — only visible if adhesion is signed
         (when adhesion-signed?
           [:div.contract-row
            [:div.contract-row__info
             [contract-icon]
             [:span.contract-row__label "Adhésion Elink-co"]]
            [:span.contract-row__signed "Signé \u2713"]])
         ;; Navigation buttons
         [:div {:style {:display "flex" :justify-content "space-between" :margin-top "1rem"}}
          [:button.btn.btn--small.btn--outline
           {:on-click #(rf/dispatch [:productions/go-back production-id])}
           "Précédent"]
          [:button.btn.btn--green.btn--small
           {:disabled (not adhesion-signed?)
            :on-click #(rf/dispatch [:productions/submit-step3 production-id])}
           "Valider et soumettre"]]
         (when @show-contract?
           [:div.modal-overlay {:on-click (fn [e]
                                             (when (= (.-target e) (.-currentTarget e))
                                               (reset! show-contract? false)))}
            [:div.modal
             [:div.modal__header
              [:span "Adhésion Elink-co"]
              [:button.btn.btn--small
               {:on-click #(reset! show-contract? false)
                :style {:background "transparent" :color "var(--color-muted)"
                        :border "none" :font-size "1.2rem" :padding "0"}}
               "\u00D7"]]
             [:div.modal__body
              [:pre {:style {:white-space      "pre-wrap"
                             :font-size        "0.85rem"
                             :line-height      "1.5"
                             :background-color "var(--color-green-pale)"
                             :padding          "1rem"
                             :border-radius    "var(--radius)"
                             :max-height       "400px"
                             :overflow-y       "auto"}}
               contract/adhesion-contract-text]]
             [:div.modal__actions
              [:button.btn.btn--small.btn--outline
               {:on-click #(reset! show-contract? false)}
               "Annuler"]
              [:button.btn.btn--small.btn--green
               {:on-click (fn []
                            (reset! show-contract? false)
                            (rf/dispatch [:auth/sign-adhesion]))}
               "Signer"]]]])]))))

(defn production-onboarding-form [production]
  (let [lifecycle (keyword (:production/lifecycle production))
        pid       (:production/id production)]
    [:div.consumption-block.consumption-block--onboarding
     [:div.consumption-block__header
      [:span "Nouvelle Production"]
      [:button {:on-click #(when (js/confirm "Annuler cette production ?")
                              (rf/dispatch [:productions/abandon pid]))
               :title    "Annuler cette production"
               :style    {:background "transparent" :border "none" :cursor "pointer"
                          :color "var(--color-muted)" :font-size "1.3rem"
                          :padding "0 0.25rem" :line-height "1"}}
       "\u00D7"]]
     [stepper lifecycle]
     (case lifecycle
       :producer-information [step0-form pid production]
       :installation-info    [step1-form pid production]
       :payment-info         [step2-form pid production]
       :contract-signature   [step3-form pid production]
       nil)]))
