(ns app.pages.admin
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; ── Alert tab ──────────────────────────────────────────────────────────────

(defn alert-tab []
  (let [msg     @(rf/subscribe [:admin/alert-message])
        active? @(rf/subscribe [:admin/alert-active?])
        draft   (r/atom {:message (or msg "") :active (boolean active?)})]
    (fn []
      (let [{:keys [message active]} @draft]
        [:div
         [:h2.admin__tab-title "Bandeau d'alerte"]
         [:div.onboarding__form {:style {:max-width "600px"}}
          [:label {:style {:font-weight "600" :margin-bottom "4px"}} "Message du bandeau"]
          [:textarea.onboarding__input
           {:rows        3
            :placeholder "Message d'alerte à afficher..."
            :value       message
            :on-change   #(swap! draft assoc :message (.. % -target -value))
            :style       {:resize "vertical" :min-height "80px"}}]
          [:button.btn.btn--green.btn--small
           {:style    {:margin-top "16px"}
            :on-click #(rf/dispatch [:admin/update-alert
                                      {:message message :active active}])}
           "Enregistrer"]
          [:label.onboarding__radio-label
           {:style {:display "flex" :align-items "center" :gap "8px" :margin-top "16px"}}
           [:input {:type      "checkbox"
                    :checked   active
                    :on-change (fn [e]
                                 (let [new-active (.. e -target -checked)]
                                   (swap! draft assoc :active new-active)
                                   (rf/dispatch [:admin/toggle-alert new-active])))}]
           "Afficher le bandeau sur le site"]
          (when active
            [:div {:style {:background "#d32f2f" :color "#fff" :padding "10px 16px"
                           :border-radius "var(--radius)" :margin-top "12px"
                           :display "flex" :align-items "center" :gap "8px"
                           :font-weight "600"}}
             [:svg {:width "20" :height "20" :viewBox "0 0 24 24"
                    :fill "none" :stroke "currentColor" :stroke-width "2"
                    :stroke-linecap "round" :stroke-linejoin "round"}
              [:path {:d "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"}]
              [:line {:x1 "12" :y1 "9" :x2 "12" :y2 "13"}]
              [:line {:x1 "12" :y1 "17" :x2 "12.01" :y2 "17"}]]
             [:span (if (seq message) message "(aperçu vide)")]])]]))))

;; ── Users table ───────────────────────────────────────────────────────────────

(defn- export-users-csv [users]
  (let [header "ID;Nom;Email;Fournisseur;Role;Statut"
        rows   (map (fn [u]
                      (str/join ";" [(:user/id u)
                                     (or (:user/name u) "")
                                     (or (:user/email u) "")
                                     (or (:user/provider u) "")
                                     (or (:user/role u) "")
                                     (or (:user/lifecycle u) "")]))
                    users)
        csv    (str/join "\n" (cons header rows))
        blob   (js/Blob. #js [csv] #js {:type "text/csv;charset=utf-8;"})
        url    (.createObjectURL js/URL blob)
        a      (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "utilisateurs.csv")
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn users-tab []
  (let [users    @(rf/subscribe [:admin/users])
        loading? @(rf/subscribe [:admin/users-loading?])]
    [:div
     [:div.consumptions__header
      [:h2.admin__tab-title "Utilisateurs"]
      [:button.btn.btn--small
       {:on-click #(export-users-csv users)
        :disabled (empty? users)
        :title    "Exporter en CSV"}
       [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
              :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
              :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"
              :style {:vertical-align "middle" :margin-right "4px"}}
        [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
        [:polyline {:points "7 10 12 15 17 10"}]
        [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
       "Exporter"]]
     (cond
       loading?
       [:p.loading "Chargement..."]

       (empty? users)
       [:p.admin__empty "Aucun utilisateur."]

       :else
       [:table.admin-table
        [:thead
         [:tr
          [:th "Nom"]
          [:th "Email"]
          [:th "Fournisseur"]
          [:th "Role"]
          [:th "Statut"]]]
        [:tbody
         (for [u users]
           ^{:key (:user/id u)}
           [:tr
            [:td (:user/name u)]
            [:td (:user/email u)]
            [:td (:user/provider u)]
            [:td {:class (when (= "admin" (:user/role u)) "admin-table__role--admin")}
             (:user/role u)]
            [:td (:user/lifecycle u)]])]])]))

;; ── Create network modal ──────────────────────────────────────────────────────

(defn- create-network-modal [on-close]
  (let [form (r/atom {:name "" :center-lat "" :center-lng "" :radius-km "1"})]
    (fn [on-close]
      (let [{:keys [name center-lat center-lng radius-km]} @form
            valid? (and (seq name)
                        (seq center-lat)
                        (seq center-lng)
                        (seq radius-km))]
        [:div.modal-overlay {:on-click on-close}
         [:div.modal {:on-click #(.stopPropagation %)}
          [:div.modal__header
           [:span "Ajouter un réseau"]
           [:button.btn.btn--small {:on-click on-close} "X"]]
          [:div.modal__body
           [:div.onboarding__form
            [:label "Nom"]
            [:input.onboarding__input
             {:value       name
              :placeholder "Nom du réseau"
              :on-change   #(swap! form assoc :name (.-value (.-target %)))}]
            [:label "Latitude du centre"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       center-lat
              :placeholder "ex: 48.8566"
              :on-change   #(swap! form assoc :center-lat (.-value (.-target %)))}]
            [:label "Longitude du centre"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       center-lng
              :placeholder "ex: 2.3522"
              :on-change   #(swap! form assoc :center-lng (.-value (.-target %)))}]
            [:label "Rayon (km)"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       radius-km
              :placeholder "10"
              :on-change   #(swap! form assoc :radius-km (.-value (.-target %)))}]]]
          [:div.modal__actions
           [:button.btn.btn--small {:on-click on-close} "Annuler"]
           [:button.btn.btn--green.btn--small
            {:disabled (not valid?)
             :on-click (fn []
                         (rf/dispatch [:admin/create-network
                                       {:name       name
                                        :center-lat (js/parseFloat center-lat)
                                        :center-lng (js/parseFloat center-lng)
                                        :radius-km  (js/parseFloat radius-km)}])
                         (on-close))}
            "Créer"]]]]))))

;; ── Networks export ──────────────────────────────────────────────────────────

(defn- export-networks-csv [networks]
  (let [header "ID;Nom;Latitude;Longitude;Rayon (km);Statut"
        rows   (map (fn [n]
                      (str/join ";" [(:network/id n)
                                     (:network/name n)
                                     (:network/center-lat n)
                                     (:network/center-lng n)
                                     (:network/radius-km n)
                                     (or (:network/lifecycle n) "private")]))
                    networks)
        csv    (str/join "\n" (cons header rows))
        blob   (js/Blob. #js [csv] #js {:type "text/csv;charset=utf-8;"})
        url    (.createObjectURL js/URL blob)
        a      (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "reseaux.csv")
    (.click a)
    (.revokeObjectURL js/URL url)))

;; ── Networks table ────────────────────────────────────────────────────────────

(defn- confirm-unpublish-modal [network on-confirm on-cancel]
  [:div.modal-overlay {:on-click on-cancel}
   [:div.modal {:on-click #(.stopPropagation %)}
    [:div.modal__header
     [:span "Rendre le réseau privé"]
     [:button.btn.btn--small {:on-click on-cancel
                              :style {:background "transparent" :color "var(--color-muted)"
                                      :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:p {:style {:margin-bottom "0.5rem"}}
      (str "Le réseau \"" (:network/name network) "\" contient "
           (:network/consumption-count network)
           " consommation(s) en cours.")]
     [:p {:style {:margin-bottom "0.5rem"}}
      "En le rendant privé, il n'apparaîtra plus sur la carte et ne sera plus visible pour les nouveaux visiteurs."]
     [:p {:style {:font-weight "600"}}
      "Les abonnements et contrats des consommateurs existants ne sont pas affectés."]]
    [:div.modal__actions
     [:button.btn.btn--small {:on-click on-cancel} "Annuler"]
     [:button.btn.btn--small {:on-click on-confirm
                              :style {:background "#d32f2f" :color "#fff"}}
      "Confirmer"]]]])

(defn- network-status-label [lifecycle]
  (case lifecycle
    "public"              "Public"
    "private"             "Privé"
    "pending-validation"  "À valider"
    lifecycle))

(defn- network-status-class [lifecycle]
  (case lifecycle
    "public"              "admin-table__status--public"
    "pending-validation"  "admin-table__status--pending"
    "admin-table__status--private"))

(defn- pending-networks-table [pending-networks]
  (when (seq pending-networks)
    [:div {:style {:margin-bottom "2rem"}}
     [:h3 {:style {:font-size "1rem" :font-weight "600" :margin-bottom "0.5rem"
                    :color "#e65100"}}
      "Réseaux à valider (" (count pending-networks) ")"]
     [:table.admin-table
      [:thead
       [:tr
        [:th "Nom"]
        [:th "Latitude"]
        [:th "Longitude"]
        [:th "Rayon (km)"]
        [:th ""]]]
      [:tbody
       (for [n pending-networks]
         ^{:key (:network/id n)}
         [:tr.admin-table__row--pending
          [:td (:network/name n)]
          [:td (:network/center-lat n)]
          [:td (:network/center-lng n)]
          [:td (:network/radius-km n)]
          [:td
           [:button.btn.btn--small.btn--green
            {:on-click #(rf/dispatch [:admin/validate-network (:network/id n)])}
            "Valider"]]])]]]))

(defn networks-tab []
  (let [show-modal?       (r/atom false)
        confirm-network   (r/atom nil)]
    (fn []
      (let [all-networks @(rf/subscribe [:admin/networks])
            loading?     @(rf/subscribe [:admin/networks-loading?])
            pending      (filterv #(= "pending-validation" (:network/lifecycle %)) all-networks)
            networks     (filterv #(not= "pending-validation" (:network/lifecycle %)) all-networks)]
        [:div
         [:div.consumptions__header
          [:h2.admin__tab-title "Réseaux"]
          [:div {:style {:display "flex" :gap "0.5rem"}}
           [:button.btn.btn--small
            {:on-click #(export-networks-csv all-networks)
             :disabled (empty? all-networks)
             :title    "Exporter en CSV"}
            [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
                   :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
                   :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"
                   :style {:vertical-align "middle" :margin-right "4px"}}
             [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
             [:polyline {:points "7 10 12 15 17 10"}]
             [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
            "Exporter"]
           [:button.btn.btn--green.btn--small
            {:on-click #(reset! show-modal? true)}
            [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
                   :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
                   :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"
                   :style {:vertical-align "middle" :margin-right "4px"}}
             [:circle {:cx "12" :cy "12" :r "10"}]
             [:line {:x1 "12" :y1 "8" :x2 "12" :y2 "16"}]
             [:line {:x1 "8" :y1 "12" :x2 "16" :y2 "12"}]]
            "Ajouter un réseau"]]]
         (cond
           loading?
           [:p.loading "Chargement..."]

           (and (empty? pending) (empty? networks))
           [:p.admin__empty "Aucun réseau."]

           :else
           [:<>
            [pending-networks-table pending]
            (when (seq networks)
              [:table.admin-table
               [:thead
                [:tr
                 [:th "Nom"]
                 [:th "Latitude"]
                 [:th "Longitude"]
                 [:th "Rayon (km)"]
                 [:th "Statut"]
                 [:th ""]]]
               [:tbody
                (for [n networks]
                  ^{:key (:network/id n)}
                  [:tr {:class (when (not= "public" (:network/lifecycle n)) "admin-table__row--disabled")}
                   [:td (:network/name n)]
                   [:td (:network/center-lat n)]
                   [:td (:network/center-lng n)]
                   [:td (:network/radius-km n)]
                   [:td {:class (network-status-class (:network/lifecycle n))}
                    (network-status-label (:network/lifecycle n))]
                   [:td
                    [:button.btn.btn--small
                     {:on-click (fn []
                                  (if (and (= "public" (:network/lifecycle n))
                                           (pos? (or (:network/consumption-count n) 0)))
                                    (reset! confirm-network n)
                                    (rf/dispatch [:admin/toggle-network-visibility (:network/id n)])))}
                     (if (= "public" (:network/lifecycle n))
                       "Rendre privé"
                       "Rendre public")]]])]])])
         (when @show-modal?
           [create-network-modal #(reset! show-modal? false)])
         (when-let [net @confirm-network]
           [confirm-unpublish-modal net
            (fn []
              (rf/dispatch [:admin/toggle-network-visibility (:network/id net)])
              (reset! confirm-network nil))
            #(reset! confirm-network nil)])]))))

;; ── Productions table ────────────────────────────────────────────────────────

(def ^:private energy-type-labels
  {"solar"        "Solaire"
   "wind"         "Eolien"
   "hydro"        "Hydraulique"
   "biomass"      "Biomasse"
   "cogeneration" "Cogeneration"})

(defn- export-productions-csv [productions]
  (let [header "ID;Utilisateur;Adresse;Réseau;PDL/PRM;Puissance (kWc);Type;Compteur Linky;IBAN;Statut"
        rows   (map (fn [p]
                      (str/join ";" [(:production/id p)
                                     (:production/user-id p)
                                     (or (:production/producer-address p) "")
                                     (or (:production/network-id p) "")
                                     (or (:production/pdl-prm p) "")
                                     (or (:production/installed-power p) "")
                                     (or (get energy-type-labels (:production/energy-type p)) (:production/energy-type p))
                                     (or (:production/linky-meter p) "")
                                     (or (:production/iban p) "")
                                     (or (:production/lifecycle p) "")]))
                    productions)
        csv    (str/join "\n" (cons header rows))
        blob   (js/Blob. #js [csv] #js {:type "text/csv;charset=utf-8;"})
        url    (.createObjectURL js/URL blob)
        a      (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "productions.csv")
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn productions-tab []
  (let [productions @(rf/subscribe [:admin/productions])
        loading?    @(rf/subscribe [:admin/productions-loading?])]
    [:div
     [:div.consumptions__header
      [:h2.admin__tab-title "Productions"]
      [:button.btn.btn--small
       {:on-click #(export-productions-csv productions)
        :disabled (empty? productions)
        :title    "Exporter en CSV"}
       [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
              :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
              :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"
              :style {:vertical-align "middle" :margin-right "4px"}}
        [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
        [:polyline {:points "7 10 12 15 17 10"}]
        [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
       "Exporter"]]
     (cond
       loading?
       [:p.loading "Chargement..."]

       (empty? productions)
       [:p.admin__empty "Aucune production."]

       :else
       [:table.admin-table
        [:thead
         [:tr
          [:th "Utilisateur"]
          [:th "Adresse"]
          [:th "PDL/PRM"]
          [:th "Puissance"]
          [:th "Type"]
          [:th "Compteur"]
          [:th "Statut"]]]
        [:tbody
         (for [p productions]
           ^{:key (:production/id p)}
           [:tr
            [:td (subs (str (:production/user-id p)) 0 8)]
            [:td (or (:production/producer-address p) "-")]
            [:td (or (:production/pdl-prm p) "-")]
            [:td (when-let [pw (:production/installed-power p)] (str pw " kWc"))]
            [:td (get energy-type-labels (:production/energy-type p) "-")]
            [:td (or (:production/linky-meter p) "-")]
            [:td (:production/lifecycle p)]])]])]))

;; ── Eligibility checks table ────────────────────────────────────────────────

(defn- format-checked-at [s]
  (when s
    (let [parts (str/split s #"T")]
      (if (>= (count parts) 2)
        (str (first parts) " " (subs (second parts) 0 (min 8 (count (second parts)))))
        s))))

(defn- export-eligibility-checks-csv [checks]
  (let [header "ID;Date;Adresse;Lat;Lng;Éligible;Réseau;Email notification"
        rows   (map (fn [c]
                      (str/join ";" [(:eligibility-check/id c)
                                     (or (:eligibility-check/checked-at c) "")
                                     (or (:eligibility-check/address c) "")
                                     (:eligibility-check/lat c)
                                     (:eligibility-check/lng c)
                                     (if (:eligibility-check/eligible? c) "Oui" "Non")
                                     (or (:eligibility-check/network-name c) "")
                                     (or (:eligibility-check/notification-email c) "")]))
                    checks)
        csv    (str/join "\n" (cons header rows))
        blob   (js/Blob. #js [csv] #js {:type "text/csv;charset=utf-8;"})
        url    (.createObjectURL js/URL blob)
        a      (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "visiteurs.csv")
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn eligibility-checks-tab []
  (let [checks   @(rf/subscribe [:admin/eligibility-checks])
        loading? @(rf/subscribe [:admin/eligibility-checks-loading?])]
    [:div
     [:div.consumptions__header
      [:h2.admin__tab-title "Vérifications d'éligibilité"]
      [:button.btn.btn--small
       {:on-click #(export-eligibility-checks-csv checks)
        :disabled (empty? checks)
        :title    "Exporter en CSV"}
       [:svg {:xmlns "http://www.w3.org/2000/svg" :width "16" :height "16"
              :viewBox "0 0 24 24" :fill "none" :stroke "currentColor"
              :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"
              :style {:vertical-align "middle" :margin-right "4px"}}
        [:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
        [:polyline {:points "7 10 12 15 17 10"}]
        [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]
       "Exporter"]]
     (cond
       loading?
       [:p.loading "Chargement..."]

       (empty? checks)
       [:p.admin__empty "Aucune vérification."]

       :else
       [:table.admin-table
        [:thead
         [:tr
          [:th "Date"]
          [:th "Adresse"]
          [:th "Lat"]
          [:th "Lng"]
          [:th "Éligible"]
          [:th "Réseau"]
          [:th "Email notification"]]]
        [:tbody
         (for [c checks]
           ^{:key (:eligibility-check/id c)}
           [:tr
            [:td (format-checked-at (:eligibility-check/checked-at c))]
            [:td (:eligibility-check/address c)]
            [:td (.toFixed (:eligibility-check/lat c) 4)]
            [:td (.toFixed (:eligibility-check/lng c) 4)]
            [:td {:class (if (:eligibility-check/eligible? c)
                           "admin-table__eligible--yes"
                           "admin-table__eligible--no")}
             (if (:eligibility-check/eligible? c) "Oui" "Non")]
            [:td (or (:eligibility-check/network-name c) "-")]
            [:td (or (:eligibility-check/notification-email c) "-")]])]])]))
