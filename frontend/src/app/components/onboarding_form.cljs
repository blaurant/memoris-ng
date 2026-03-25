(ns app.components.onboarding-form
  (:require [app.components.legal-person-modal :as lpm]
            [app.consumptions.contract :as contract]
            [app.utils.google-maps :as google-maps]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfee]
            [reagent.core :as r]))

(def steps
  [{:key :consumer-information :label "Adresse & Réseau"        :number 1}
   {:key :linky-reference       :label "Référence Linky"        :number 2}
   {:key :billing-address       :label "Info financières"       :number 3}
   {:key :contract-signature    :label "Signature du contrat"   :number 4}])

(def ^:private step-order
  {:consumer-information 0
   :linky-reference      1
   :billing-address      2
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

(defn- network-map-modal
  "Form-3 modal with a Google Map for selecting a network by clicking its circle."
  [address networks on-select on-cancel]
  (let [map-el           (atom nil)
        map-inst         (atom nil)
        circles          (atom [])
        selected-network (r/atom nil)]
    (r/create-class
     {:display-name "network-map-modal"

      :component-did-mount
      (fn [_this]
        (let [init-map!
              (fn []
                (let [geocoder (js/google.maps.Geocoder.)
                      create-map!
                      (fn [center zoom]
                        (when @map-el
                          (let [gmap (js/google.maps.Map.
                                      @map-el
                                      #js {:center center :zoom zoom :mapTypeId "roadmap"})]
                            (reset! map-inst gmap)
                            ;; Draw circles with click listeners
                            (doseq [c @circles] (.setMap ^js c nil))
                            (reset! circles
                                    (mapv (fn [net]
                                            (let [circle (js/google.maps.Circle.
                                                          #js {:map          gmap
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
                                                  ;; Reset all circles to default style
                                                  (doseq [c @circles]
                                                    (.setOptions ^js c #js {:fillOpacity  0.2
                                                                           :strokeWeight 2}))
                                                  ;; Highlight selected circle
                                                  (.setOptions circle #js {:fillOpacity  0.5
                                                                          :strokeWeight 3})
                                                  (reset! selected-network net)))
                                              circle))
                                          networks)))))]
                  (if (seq address)
                    (-> (.geocode geocoder #js {:address address})
                        (.then (fn [^js response]
                                 (let [results      (.-results response)
                                       ^js first-r  (when (and results (pos? (.-length results)))
                                                      (aget results 0))
                                       ^js loc      (some-> first-r .-geometry .-location)]
                                   (if loc
                                     (create-map! #js {:lat (.lat loc) :lng (.lng loc)} 10)
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
      (fn [_address _networks _on-select on-cancel]
        (let [sel @selected-network]
          [:div.modal-overlay {:on-click (fn [e]
                                           (when (= (.-target e) (.-currentTarget e))
                                             (on-cancel)))}
           [:div.modal
            [:div.modal__header
             [:span "Sélectionner un réseau sur la carte"]
             [:button.btn.btn--small
              {:on-click on-cancel
               :style {:background "transparent" :color "var(--color-muted)"
                       :border "none" :font-size "1.2rem" :padding "0"}}
              "\u00D7"]]
            [:div.modal__map
             {:ref (fn [el] (when el (reset! map-el el)))}]
            [:div.modal__selection
             (if sel
               (str "Réseau sélectionné : " (:network/name sel))
               "Aucun réseau sélectionné")]
            [:div.modal__actions
             [:button.btn.btn--small.btn--outline
              {:on-click on-cancel}
              "Annuler"]
             [:button.btn.btn--small.btn--green
              {:disabled (nil? sel)
               :on-click #(on-select (:network/id sel))}
              "Valider"]]]]))})))

(defn- identity-selector
  "Lets the user pick which identity to use: natural person or one of their legal persons.
   Updates address atom when selection changes."
  [user selected-identity address]
  (let [show-modal? (r/atom false)]
    (fn [user selected-identity address]
      (let [natural    (:natural-person user)
            legals     (or (:legal-persons user) [])
            nat-label  (str (:first-name natural) " " (:last-name natural))]
        [:div {:style {:margin-bottom "1rem"}}
         [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-bottom "0.5rem" :display "block"}}
          "Identité utilisée pour cette consommation"]
         [:div.onboarding__radio-group
          [:label.onboarding__radio-label
           [:input {:type      "radio"
                    :name      "identity-choice"
                    :checked   (= @selected-identity :natural)
                    :on-change (fn []
                                 (reset! selected-identity :natural)
                                 (reset! address (or (:postal-address natural) "")))}]
           (str nat-label " (personne physique)")]
          (doall
            (for [[idx lp] (map-indexed vector legals)]
              ^{:key idx}
              [:label.onboarding__radio-label
               [:input {:type      "radio"
                        :name      "identity-choice"
                        :checked   (= @selected-identity [:legal idx])
                        :on-change (fn []
                                     (reset! selected-identity [:legal idx])
                                     (reset! address (or (:headquarters lp) "")))}]
               (str (:company-name lp) " — " (:siren lp))]))]
         [:button.btn.btn--small.btn--outline
          {:on-click #(reset! show-modal? true)
           :style {:margin-top "0.5rem" :font-size "0.8rem"}}
          "+ Créer une personne morale"]
         (when @show-modal?
           [lpm/new-legal-person-modal #(reset! show-modal? false)])]))))

(defn- step1-form [consumption-id]
  (let [address           (r/atom nil)
        network-id        (r/atom "")
        show-map?         (r/atom false)
        selected-identity (r/atom :natural)
        prefilled?        (r/atom false)
        networks          @(rf/subscribe [:networks/list])]
    (fn []
      (let [user    @(rf/subscribe [:auth/user])
            natural (:natural-person user)]
        ;; Pre-fill address from natural person on first render
        (when (and (not @prefilled?)
                   natural
                   (seq (:postal-address natural)))
          (reset! address (:postal-address natural))
          (reset! prefilled? true))
        (when (nil? @address)
          (reset! address ""))
        [:div.onboarding__form
         (if-not (and natural
                      (seq (:first-name natural))
                      (seq (:last-name natural)))
           ;; No identity filled — show message + link to profile
           [:div {:style {:background "#fff8e1" :border "1px solid #ffe082"
                          :border-radius "var(--radius)" :padding "1.25rem"
                          :text-align "center"}}
            [:p {:style {:font-weight "600" :margin-bottom "0.5rem"}}
             "Veuillez d'abord renseigner votre identité"]
            [:p {:style {:font-size "0.9rem" :color "var(--color-muted)" :margin-bottom "1rem"}}
             "Pour créer une consommation, nous avons besoin de vos informations personnelles "
             "(nom, prénom, adresse, etc.)."]
            [:button.btn.btn--green.btn--small
             {:on-click #(rf/dispatch [:portal/set-section :profile])}
             "Compléter mon profil"]]

           ;; Identity filled — show selector + address form
           [:<>
            [identity-selector user selected-identity address]
            [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-bottom "0.25rem" :display "block"}}
             "Adresse de la consommation"]
            [:input.onboarding__input
             {:type        "text"
              :placeholder "Votre adresse de consommation"
              :value       @address
              :on-change   #(reset! address (.. % -target -value))}]
            [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-bottom "0.25rem" :display "block"}}
             "Sélectionnez votre réseau"]
            [:div.onboarding__network-row
             [:select.onboarding__select
              {:value     @network-id
               :on-change #(reset! network-id (.. % -target -value))}
              [:option {:value ""} "Choisir un réseau"]
              (doall
                (for [n networks]
                  ^{:key (:network/id n)}
                  [:option {:value (:network/id n)} (:network/name n)]))]
             [:button.btn.btn--small.btn--outline
              {:on-click #(reset! show-map? true)}
              "Sélectionner sur la carte"]]
            [:button.btn.btn--green.btn--small
             {:disabled (or (empty? @address) (empty? @network-id))
              :on-click #(rf/dispatch [:consumptions/submit-step1
                                       consumption-id @address @network-id])}
             "Suivant"]
            (when @show-map?
              [network-map-modal
               @address
               networks
               (fn [selected-id]
                 (reset! network-id selected-id)
                 (reset! show-map? false))
               (fn []
                 (reset! show-map? false))])])]))))

(defn- step2-form [consumption-id]
  (let [linky-ref (r/atom "")]
    (fn []
      [:div.onboarding__form
       [:input.onboarding__input
        {:type        "text"
         :placeholder "Référence Linky (ex: PRM 12345678901234)"
         :value       @linky-ref
         :on-change   #(reset! linky-ref (.. % -target -value))}]
       [:div {:style {:display "flex" :justify-content "space-between" :margin-top "0.75rem"}}
        [:button.btn.btn--small.btn--outline
         {:on-click #(rf/dispatch [:consumptions/go-back consumption-id])}
         "Précédent"]
        [:button.btn.btn--green.btn--small
         {:disabled (empty? @linky-ref)
          :on-click #(rf/dispatch [:consumptions/submit-step2
                                   consumption-id @linky-ref])}
         "Suivant"]]])))

(defn- step3-form [consumption-id consumer-address]
  (let [use-same?    (r/atom true)
        billing-addr (r/atom "")
        iban         (r/atom "")
        bic          (r/atom "")]
    (fn []
      (let [user    @(rf/subscribe [:auth/user])
            natural (:natural-person user)
            legals  (or (:legal-persons user) [])
            same?   @use-same?
            effective-addr (if same? consumer-address @billing-addr)]
        [:div.onboarding__form
         ;; Identity display (read-only)
         [:div {:style {:background "var(--color-green-pale)" :border-radius "var(--radius)"
                        :padding "0.75rem 1rem" :margin-bottom "1rem"}}
          [:span {:style {:font-weight "600" :font-size "0.9rem"}}
           (if (seq legals)
             ;; TODO: show selected legal person if applicable
             (str (:first-name natural) " " (:last-name natural))
             (str (:first-name natural) " " (:last-name natural)))]]

         ;; Billing address
         [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-bottom "0.25rem" :display "block"}}
          "Adresse de facturation"]
         [:div.onboarding__radio-group
          [:label.onboarding__radio-label
           [:input {:type      "radio"
                    :name      "billing-choice"
                    :checked   same?
                    :on-change #(reset! use-same? true)}]
           "Utiliser la même adresse que l'adresse de consommation"]
          [:label {:class (str "onboarding__radio-label"
                               (when same? " onboarding__radio-label--disabled"))}
           [:input {:type      "radio"
                    :name      "billing-choice"
                    :checked   (not same?)
                    :on-change #(reset! use-same? false)}]
           "Utiliser une adresse différente"]]
         [:input.onboarding__input
          {:type        "text"
           :placeholder "Adresse de facturation"
           :value       (if same? consumer-address @billing-addr)
           :disabled    same?
           :on-change   #(reset! billing-addr (.. % -target -value))}]

         ;; IBAN / BIC
         [:label {:style {:font-weight "600" :font-size "0.9rem" :margin-top "1rem"
                          :margin-bottom "0.25rem" :display "block"}}
          "IBAN " [:span {:style {:color "#d32f2f"}} "*"]]
         [:input.onboarding__input
          {:type        "text"
           :placeholder "Ex: FR76 3000 6000 0112 3456 7890 189"
           :value       @iban
           :on-change   #(reset! iban (.. % -target -value))}]
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
           {:on-click #(rf/dispatch [:consumptions/go-back consumption-id])}
           "Précédent"]
          [:button.btn.btn--green.btn--small
           {:disabled (or (empty? effective-addr) (empty? @iban))
            :on-click #(rf/dispatch [:consumptions/submit-step3
                                     consumption-id effective-addr])}
           "Suivant"]]]))))

(def ^:private contract-configs
  [{:type     :producer
    :label    "Contrat d'achat d'électricité"
    :text     contract/producer-contract-text
    :signed-key :consumption/producer-contract-signed-at}
   {:type     :sepa
    :label    "Mandat de virement SEPA"
    :text     contract/sepa-mandate-text
    :signed-key :consumption/sepa-mandate-signed-at}])

(defn- contract-icon []
  [:svg {:width "24" :height "24" :viewBox "0 0 24 24"
         :fill "none" :stroke "currentColor" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

(defn- docuseal-signing-watcher
  "Shows a modal while DocuSeal signing is in progress.
   Closes automatically when the user returns to the tab after signing."
  []
  (let [visibility-handler (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (let [handler (fn []
                         (when (= "visible" (.-visibilityState js/document))
                           ;; User came back to our tab — refresh user and close
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

(defn- contract-signing-watcher
  "Shows a modal while any DocuSeal contract signing is in progress.
   Closes when the user returns to the tab."
  [consumption-id]
  (let [visibility-handler (atom nil)]
    (r/create-class
      {:component-did-mount
       (fn [_]
         (let [handler (fn []
                         (when (= "visible" (.-visibilityState js/document))
                           (let [ct @(rf/subscribe [:consumptions/contract-signing-type])]
                             (when ct
                               (rf/dispatch [:consumptions/contract-signing-complete
                                             consumption-id ct])))
                           (js/setTimeout #(rf/dispatch [:consumptions/fetch]) 2000)))]
           (reset! visibility-handler handler)
           (.addEventListener js/document "visibilitychange" handler)))
       :component-will-unmount
       (fn [_]
         (when-let [handler @visibility-handler]
           (.removeEventListener js/document "visibilitychange" handler)))
       :reagent-render
       (fn [_consumption-id]
         (let [signing-url @(rf/subscribe [:consumptions/contract-signing-url])]
           (when signing-url
             [:div.modal-overlay {:on-click (fn [e]
                                              (when (= (.-target e) (.-currentTarget e))
                                                (let [ct @(rf/subscribe [:consumptions/contract-signing-type])]
                                                  (rf/dispatch [:consumptions/contract-signing-complete
                                                                consumption-id ct]))))}
              [:div.modal {:style {:max-width "500px" :text-align "center"}}
               [:div.modal__header
                [:span "Signature du contrat"]
                [:button.btn.btn--small
                 {:on-click #(let [ct @(rf/subscribe [:consumptions/contract-signing-type])]
                               (rf/dispatch [:consumptions/contract-signing-complete
                                             consumption-id ct]))
                  :style {:background "transparent" :color "var(--color-muted)"
                          :border "none" :font-size "1.2rem" :padding "0"}}
                 "\u00D7"]]
               [:div.modal__body {:style {:padding "2rem"}}
                [:p {:style {:font-size "1rem" :line-height "1.6" :margin-bottom "1.5rem"}}
                 "Le document a été ouvert dans un nouvel onglet. "
                 "Signez-le puis revenez ici."]
                [:p {:style {:font-size "0.85rem" :color "var(--color-muted)" :margin-bottom "1.5rem"}}
                 "La fenêtre se fermera automatiquement quand vous reviendrez."]
                [:a.btn.btn--green
                 {:href signing-url :target "_blank" :rel "noopener"}
                 "Ouvrir le document à signer"]]]])))})))

(defn- step4-form [consumption-id consumption]
  (fn [consumption-id consumption]
    (let [user              @(rf/subscribe [:auth/user])
          adhesion-signed?  (some? (:adhesion-signed-at user))
          adhesion-loading? @(rf/subscribe [:auth/adhesion-loading?])
          contract-loading? @(rf/subscribe [:consumptions/contract-loading?])
          all-configs       (into [{:type       :adhesion
                                      :label      "Adhésion à l'association elink-co"
                                      :signed-key nil}]
                                    contract-configs)]
      [:div.onboarding__form
       (doall
         (for [{:keys [type label signed-key]} all-configs]
           (let [signed? (if (= type :adhesion)
                           adhesion-signed?
                           (some? (get consumption signed-key)))
                 loading? (or (and (= type :adhesion) adhesion-loading?)
                              (= contract-loading? type))]
             ^{:key type}
             [:div.contract-row
              [:div.contract-row__info
               [contract-icon]
               [:span.contract-row__label label]]
              (if signed?
                [:span.contract-row__signed "Sign\u00e9 \u2713"]
                (if loading?
                  [:span {:style {:color "var(--color-muted)" :font-size "0.85rem"}}
                   "Chargement..."]
                  [:button.btn.btn--small.btn--outline
                   {:on-click (fn []
                                (if (= type :adhesion)
                                  (rf/dispatch [:auth/sign-adhesion])
                                  (rf/dispatch [:consumptions/submit-step4 consumption-id type])))}
                   "A signer"]))])))
       ;; DocuSeal signing watchers
       [docuseal-signing-watcher]
       [contract-signing-watcher consumption-id]
       [:div {:style {:margin-top "0.75rem"}}
        [:button.btn.btn--small.btn--outline
         {:on-click #(rf/dispatch [:consumptions/go-back consumption-id])}
         "Précédent"]]])))


(defn onboarding-form [consumption]
  (let [lifecycle (keyword (:consumption/lifecycle consumption))
        cid       (:consumption/id consumption)]
    [:div.consumption-block.consumption-block--onboarding
     [:div.consumption-block__header
      [:span "Nouvelle Consommation"]
      [:button {:on-click #(when (js/confirm "Annuler cette consommation ?")
                              (rf/dispatch [:consumptions/abandon cid]))
               :title    "Annuler cette consommation"
               :style    {:background "transparent" :border "none" :cursor "pointer"
                          :color "var(--color-muted)" :font-size "1.3rem"
                          :padding "0 0.25rem" :line-height "1"}}
       "\u00D7"]]
     [stepper lifecycle]
     (case lifecycle
       :consumer-information  [step1-form cid]
       :linky-reference       [step2-form cid]
       :billing-address       [step3-form cid (:consumption/consumer-address consumption)]
       :contract-signature    [step4-form cid consumption]
       nil)]))
