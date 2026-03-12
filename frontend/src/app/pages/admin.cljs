(ns app.pages.admin
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]))

;; ── Users table ───────────────────────────────────────────────────────────────

(defn users-tab []
  (let [users    @(rf/subscribe [:admin/users])
        loading? @(rf/subscribe [:admin/users-loading?])]
    [:div
     [:h2.admin__tab-title "Utilisateurs"]
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
  (let [form (r/atom {:name "" :center-lat "" :center-lng "" :radius-km "10"})]
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
  (let [header "Nom;Latitude;Longitude;Rayon (km);Statut"
        rows   (map (fn [n]
                      (str/join ";" [(:network/name n)
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

(defn networks-tab []
  (let [show-modal? (r/atom false)]
    (fn []
      (let [networks @(rf/subscribe [:admin/networks])
            loading? @(rf/subscribe [:admin/networks-loading?])]
        [:div
         [:div.consumptions__header
          [:h2.admin__tab-title "Réseaux"]
          [:div {:style {:display "flex" :gap "0.5rem"}}
           [:button.btn.btn--small
            {:on-click #(export-networks-csv networks)
             :disabled (empty? networks)
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

           (empty? networks)
           [:p.admin__empty "Aucun réseau."]

           :else
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
                [:td {:class (if (= "public" (:network/lifecycle n))
                               "admin-table__status--public"
                               "admin-table__status--private")}
                 (if (= "public" (:network/lifecycle n)) "Public" "Privé")]
                [:td
                 [:button.btn.btn--small
                  {:on-click #(rf/dispatch [:admin/toggle-network-visibility (:network/id n)])}
                  (if (= "public" (:network/lifecycle n))
                    "Rendre privé"
                    "Rendre public")]]])]])
         (when @show-modal?
           [create-network-modal #(reset! show-modal? false)])]))))

;; ── Eligibility checks table ────────────────────────────────────────────────

(defn- format-checked-at [s]
  (when s
    (let [parts (str/split s #"T")]
      (if (>= (count parts) 2)
        (str (first parts) " " (subs (second parts) 0 (min 8 (count (second parts)))))
        s))))

(defn eligibility-checks-tab []
  (let [checks   @(rf/subscribe [:admin/eligibility-checks])
        loading? @(rf/subscribe [:admin/eligibility-checks-loading?])]
    [:div
     [:h2.admin__tab-title "Vérifications d'éligibilité"]
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
          [:th "Réseau"]]]
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
            [:td (or (:eligibility-check/network-name c) "-")]])]])]))
