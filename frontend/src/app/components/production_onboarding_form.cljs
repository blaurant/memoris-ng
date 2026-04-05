(ns app.components.production-onboarding-form
  (:require [app.components.onboarding-form :as conso-onboarding]
            [app.productions.contract :as contract]
            [app.utils.google-maps :as google-maps]
            [clojure.string]
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
                "Le r\u00e9seau sera centr\u00e9 sur votre adresse (diam\u00e8tre : 2 km) et soumis \u00e0 validation. "
                "La taille et la localisation du r\u00e9seau pourront \u00eatre ajust\u00e9es par l'administrateur. "
                "Pour les r\u00e9seaux p\u00e9riurbains (diam\u00e8tre 20 km) et ruraux (diam\u00e8tre 40 km), une validation Enedis sera \u00e9galement requise."]
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
  (let [init-nid       (:production/network-id production)
        address        (r/atom (or (:production/producer-address production) ""))
        selected-name  (r/atom (:production/network-name production))
        selected-id    (r/atom init-nid)
        show-modal?    (r/atom false)
        show-identity? (r/atom false)]
    (fn []
      (let [networks @(rf/subscribe [:networks/list])
            user     @(rf/subscribe [:auth/user])
            natural  (:natural-person user)]
      [:div.onboarding__form
       (if-not (and natural
                    (seq (:first-name natural))
                    (seq (:last-name natural)))
         ;; No identity filled — show message + modal to fill it
         [:div {:style {:background "#fff8e1" :border "1px solid #ffe082"
                        :border-radius "var(--radius)" :padding "1.25rem"
                        :text-align "center"}}
          [:p {:style {:font-size "0.9rem" :color "var(--color-muted)" :margin-bottom "1rem"}}
           "Pour continuer, nous avons besoin de vos informations personnelles "
           "(nom, prénom, adresse, etc.)."]
          [:button.btn.btn--green.btn--small
           {:on-click #(reset! show-identity? true)}
           "Renseigner mon identité"]
          (when @show-identity?
            [conso-onboarding/identity-modal #(reset! show-identity? false)])]

         ;; Identity filled — show address form
         [:<>
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
            (reset! show-modal? false))])])]))))

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
         :placeholder "Numéro à 14 chiffres"
         :maxLength   14
         :value       @pdl-prm
         :on-change   #(reset! pdl-prm (clojure.string/replace (.. % -target -value) #"[^\d]" ""))
         :style (when (and (seq @pdl-prm) (not (re-matches #"^\d{14}$" @pdl-prm)))
                  {:border-color "#d32f2f"})}]
       (when (and (seq @pdl-prm) (not (re-matches #"^\d{14}$" @pdl-prm)))
         [:span {:style {:font-size "0.8rem" :color "#d32f2f"}}
          "Le numéro PDL/PRM doit contenir exactement 14 chiffres"])
       [:div {:style {:display "flex" :gap "1rem" :align-items "flex-start" :margin-top "0.25rem"}}
        [:p {:style {:font-size "0.8rem" :color "var(--color-muted)" :line-height "1.4" :flex "1"}}
         "Vous pouvez trouver le numéro PDL du compteur Linky sur votre facture."
         [:br]
         "Ce numéro peut aussi être trouvé directement sur votre compteur Linky. "
         "Pour cela, faites défiler les affichages du compteur (appui sur la touche +) "
         "jusqu'à lire la valeur du « numéro de PRM ». "
         "Le numéro de PRM est le nom donné au PDL sur le compteur Enedis Linky. "
         "Il s'agit d'une suite de 14 chiffres qui identifie le logement sur le réseau électrique."]
        [:img {:src "/img/linky-prm.png"
               :alt "Compteur Linky affichant le numéro de PRM"
               :style {:width "120px" :border-radius "var(--radius)"
                       :box-shadow "0 1px 4px rgba(0,0,0,0.15)" :flex-shrink "0"}}]]
       [:label "Puissance installee (kWh)"]
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
         :placeholder "Ex: LIN123456"
         :value       @linky-meter
         :on-change   #(reset! linky-meter (.. % -target -value))}]
       [:p {:style {:font-size "0.8rem" :color "var(--color-muted)" :margin-top "0.25rem" :line-height "1.4"}}
        "Votre numéro de compteur Linky figure " [:strong "sur le capot de votre appareil"] ". "
        "D'un simple coup d'œil, vérifiez qu'il correspond bien à celui qui figure sur votre facture."]
       [:div {:style {:display "flex" :justify-content "space-between" :margin-top "0.75rem"}}
        [:button.btn.btn--small.btn--outline
         {:on-click #(rf/dispatch [:productions/go-back production-id])}
         "Précédent"]
        [:button.btn.btn--green.btn--small
         {:disabled (or (not (re-matches #"^\d{14}$" (or @pdl-prm "")))
                        (empty? @power)
                        (empty? @energy-type)
                        (empty? @linky-meter))
          :on-click #(rf/dispatch [:productions/submit-step1
                                    production-id @pdl-prm @power
                                    (keyword @energy-type) @linky-meter])}
         "Suivant"]]])))

(defn- step2-form [production-id production]
  (let [producer-address (:production/producer-address production)
        use-same?        (r/atom (or (nil? (:production/payment-address production))
                                     (= (:production/payment-address production) producer-address)))
        payment-addr     (r/atom (or (:production/payment-address production) ""))
        iban-holder      (r/atom (or (:production/iban-holder production) ""))
        iban             (r/atom (or (:production/iban production) ""))
        bic              (r/atom (or (:production/bic production) ""))]
    (fn []
      (let [same?          @use-same?
            effective-addr (if same? producer-address @payment-addr)]
        [:div.onboarding__form
         ;; Payment address
         [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-bottom "0.25rem" :display "block"}}
          "Adresse de paiement"]
         [:div.onboarding__radio-group
          [:label.onboarding__radio-label
           [:input {:type      "radio"
                    :name      "payment-addr-choice"
                    :checked   same?
                    :on-change #(reset! use-same? true)}]
           "Utiliser la même adresse que l'adresse de production"]
          [:label {:class (str "onboarding__radio-label"
                               (when same? " onboarding__radio-label--disabled"))}
           [:input {:type      "radio"
                    :name      "payment-addr-choice"
                    :checked   (not same?)
                    :on-change #(reset! use-same? false)}]
           "Utiliser une adresse différente"]]
         [:input.onboarding__input
          {:type        "text"
           :placeholder "Adresse de paiement"
           :value       (if same? producer-address @payment-addr)
           :disabled    same?
           :on-change   #(reset! payment-addr (.. % -target -value))}]

         ;; IBAN holder / IBAN / BIC
         [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-top "1rem"
                          :margin-bottom "0.25rem" :display "block"}}
          "Nom ou raison sociale du titulaire " [:span {:style {:color "#d32f2f"}} "*"]]
         [:input.onboarding__input
          {:type        "text"
           :placeholder "Ex: Jean Dupont ou SCI Les Oliviers"
           :value       @iban-holder
           :on-change   #(reset! iban-holder (.. % -target -value))}]

         ;; IBAN
         [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-top "0.5rem"
                          :margin-bottom "0.25rem" :display "block"}}
          "IBAN " [:span {:style {:color "#d32f2f"}} "*"]]
         (let [iban-clean (clojure.string/replace (clojure.string/upper-case @iban) #"\s" "")
               iban-valid? (and (seq iban-clean)
                                (re-matches #"^[A-Z]{2}\d{2}[A-Z0-9]{11,30}$" iban-clean))]
           [:<>
            [:input.onboarding__input
             {:type        "text"
              :placeholder "Ex: FR76 3000 6000 0112 3456 7890 189"
              :value       @iban
              :on-change   #(reset! iban (.. % -target -value))
              :style (when (and (seq @iban) (not iban-valid?))
                       {:border-color "#d32f2f"})}]
            (when (and (seq @iban) (not iban-valid?))
              [:span {:style {:font-size "0.8rem" :color "#d32f2f"}}
               "IBAN invalide (2 lettres + 2 chiffres + 11 à 30 caractères alphanumériques)"])

            ;; BIC
            [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-top "0.5rem"
                             :margin-bottom "0.25rem" :display "block"}}
             "BIC " [:span {:style {:color "var(--color-muted)" :font-weight "400" :font-size "0.8rem"}}
                     "(recommandé)"]]
            [:input.onboarding__input
             {:type        "text"
              :placeholder "Ex: BNPAFRPP"
              :value       @bic
              :on-change   #(reset! bic (.. % -target -value))}]

            [:div {:style {:display "flex" :justify-content "space-between" :margin-top "0.75rem"}}
             [:button.btn.btn--small.btn--outline
              {:on-click #(rf/dispatch [:productions/go-back production-id])}
              "Précédent"]
             [:button.btn.btn--green.btn--small
              {:disabled (or (empty? effective-addr) (empty? @iban-holder) (not iban-valid?))
               :on-click #(rf/dispatch [:productions/submit-step2
                                         production-id @iban-holder @iban @bic effective-addr])}
              "Suivant"]]])]))))


(defn- contract-icon []
  [:svg {:width "24" :height "24" :viewBox "0 0 24 24"
         :fill "none" :stroke "currentColor" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

(defn- docuseal-signing-watcher []
  (let [visibility-handler (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (let [handler (fn []
                         (when (= "visible" (.-visibilityState js/document))
                           (rf/dispatch [:auth/check-adhesion-status])
                           (js/setTimeout
                             #(rf/dispatch [:auth/docuseal-signing-complete])
                             2000)))]
           (reset! visibility-handler handler)
           (.addEventListener js/document "visibilitychange" handler)))
       :component-will-unmount
       (fn [_]
         (when-let [handler @visibility-handler]
           (.removeEventListener js/document "visibilitychange" handler)))
       :reagent-render
       (fn []
         (let [signing-url @(rf/subscribe [:auth/docuseal-signing-url])]
           (when signing-url
             [:div.modal-overlay {:on-click (fn [e]
                                              (when (= (.-target e) (.-currentTarget e))
                                                (rf/dispatch [:auth/docuseal-signing-complete])))}
              [:div.modal {:style {:max-width "500px" :text-align "center"}}
               [:div.modal__header
                [:span "Signature de l'adhésion"]
                [:button.btn.btn--small
                 {:on-click #(rf/dispatch [:auth/docuseal-signing-complete])
                  :style {:background "transparent" :color "var(--color-muted)"
                          :border "none" :font-size "1.2rem" :padding "0"}}
                 "\u00D7"]]
               [:div.modal__body {:style {:padding "2rem"}}
                [:p {:style {:font-size "1rem" :line-height "1.6" :margin-bottom "1.5rem"}}
                 "Le document d'adhésion Elink-co a été ouvert dans un nouvel onglet. "
                 "Signez-le puis revenez ici."]
                [:p {:style {:font-size "0.85rem" :color "var(--color-muted)" :margin-bottom "1.5rem"}}
                 "La fenêtre se fermera automatiquement quand vous reviendrez."]
                [:a.btn.btn--green
                 {:href signing-url :target "_blank" :rel "noopener"}
                 "Ouvrir le document à signer"]]]])))})))

(defn- step3-form [production-id _production]
  (let [user              @(rf/subscribe [:auth/user])
        adhesion-signed?  (some? (:adhesion-signed-at user))
        adhesion-loading? @(rf/subscribe [:auth/adhesion-loading?])]
    [:div.onboarding__form
     ;; Adhesion Elink-co
     [:div.contract-row
      [:div.contract-row__info
       [contract-icon]
       [:span.contract-row__label "Adhésion Elink-co"]]
      (if adhesion-signed?
        [:span.contract-row__signed "Signé \u2713"]
        (if adhesion-loading?
          [:span {:style {:color "var(--color-muted)" :font-size "0.85rem"}}
           "Chargement..."]
          [:button.btn.btn--small.btn--outline
           {:on-click #(rf/dispatch [:auth/sign-adhesion])}
           "A signer"]))]
     ;; DocuSeal signing watcher
     [docuseal-signing-watcher]
     ;; Navigation buttons
     [:div {:style {:display "flex" :justify-content "space-between" :margin-top "1rem"}}
      [:button.btn.btn--small.btn--outline
       {:on-click #(rf/dispatch [:productions/go-back production-id])}
       "Précédent"]
      [:button.btn.btn--green.btn--small
       {:disabled (not adhesion-signed?)
        :on-click #(rf/dispatch [:productions/submit-step3 production-id])}
       "Valider et soumettre"]]]))

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
