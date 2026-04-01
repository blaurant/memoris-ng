(ns app.pages.admin
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfee]))

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

;; ── User profile modal ────────────────────────────────────────────────────────

(defn- user-profile-modal [user on-close]
  (let [np (:user/natural-person user)]
    [:div.modal-overlay {:on-click (fn [e]
                                      (when (= (.-target e) (.-currentTarget e))
                                        (on-close)))}
     [:div.modal {:on-click #(.stopPropagation %)}
      [:div.modal__header
       [:span (str "Profil de " (:user/name user))]
       [:button.btn.btn--small
        {:on-click on-close
         :style {:background "transparent" :color "var(--color-muted)"
                 :border "none" :font-size "1.2rem" :padding "0"}}
        "\u00D7"]]
      [:div.modal__body
       (if (and np (seq (:first-name np)))
         [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "0.75rem 1.5rem"}}
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Prénom"]
           [:p {:style {:font-weight "600"}} (:first-name np)]]
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Nom"]
           [:p {:style {:font-weight "600"}} (:last-name np)]]
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Date de naissance"]
           [:p {:style {:font-weight "600"}} (or (:birth-date np) "—")]]
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Adresse"]
           [:p {:style {:font-weight "600"}} (or (:address np) "—")]]
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Code postal"]
           [:p {:style {:font-weight "600"}} (or (:postal-code np) "—")]]
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Ville"]
           [:p {:style {:font-weight "600"}} (or (:city np) "—")]]
          [:div
           [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} "Téléphone"]
           [:p {:style {:font-weight "600"}} (or (:phone np) "—")]]
]
         [:p {:style {:color "var(--color-muted)" :text-align "center" :padding "1rem 0"}}
          "Cet utilisateur n'a pas encore renseigné son identité."])]
      [:div.modal__actions
       [:button.btn.btn--small.btn--green
        {:on-click on-close}
        "Fermer"]]]]))

;; ── Edit user profile modal ───────────────────────────────────────────────────

(def ^:private natural-required-fields
  [:last-name :first-name :birth-date :address :postal-code :city :phone])

(defn- max-birth-date []
  (let [d (js/Date.)
        y (- (.getFullYear d) 17)
        m (+ (.getMonth d) 1)
        day (.getDate d)]
    (str y "-" (when (< m 10) "0") m "-" (when (< day 10) "0") day)))

(defn- birth-date-valid? [v]
  (and (string? v) (seq v)
       (>= (count v) 10)
       (<= (compare "1920-01-01" v) 0)
       (<= (compare v (max-birth-date)) 0)))

(defn- postal-code-valid? [v]
  (and (string? v)
       (some? (re-matches #"^(0[1-9]|[1-8]\d|9[0-5]|97[1-6]|98[0-8])\d{3}$" v))))

(defn- natural-person-valid? [data]
  (and (every? #(seq (str (get data %))) natural-required-fields)
       (postal-code-valid? (:postal-code data))
       (birth-date-valid? (:birth-date data))))

(defn- field-row [label value on-change & [{:keys [placeholder type required? min max]}]]
  [:div {:style {:display "flex" :flex-direction "column" :gap "0.25rem"}}
   [:label {:style {:font-size "0.85rem" :font-weight "600" :color "var(--color-text)"}}
    label (when required? [:span {:style {:color "#d32f2f" :margin-left "2px"}} "*"])]
   [:input (cond-> {:type        (or type "text")
                    :value       (or value "")
                    :placeholder (or placeholder "")
                    :on-change   #(on-change (-> % .-target .-value))
                    :style       {:padding "0.5rem 0.75rem" :border "1px solid var(--color-border)"
                                  :border-radius "var(--radius)" :font-size "0.95rem"}}
             min (assoc :min min)
             max (assoc :max max))]])

(defn- filter-phone [v]
  (apply str (re-seq #"[0-9+\-.()\s]" v)))

(defn- phone-field-row [label value on-change & [{:keys [required?]}]]
  [:div {:style {:display "flex" :flex-direction "column" :gap "0.25rem"}}
   [:label {:style {:font-size "0.85rem" :font-weight "600" :color "var(--color-text)"}}
    label (when required? [:span {:style {:color "#d32f2f" :margin-left "2px"}} "*"])]
   [:input {:type        "tel"
            :inputMode   "tel"
            :value       (or value "")
            :placeholder "Ex: +33 6 12 34 56 78"
            :on-change   #(on-change (filter-phone (-> % .-target .-value)))
            :style       {:padding "0.5rem 0.75rem" :border "1px solid var(--color-border)"
                          :border-radius "var(--radius)" :font-size "0.95rem"}}]])

(defn- edit-user-profile-modal [user on-close]
  (let [np   (or (:user/natural-person user) {})
        form (r/atom np)]
    (fn [user on-close]
      (let [data   @form
            valid? (natural-person-valid? data)]
        [:div.modal-overlay {:on-click (fn [e]
                                          (when (= (.-target e) (.-currentTarget e))
                                            (on-close)))}
         [:div.modal {:on-click #(.stopPropagation %)}
          [:div.modal__header
           [:span (str "Modifier l'identité de " (:user/name user))]
           [:button.btn.btn--small
            {:on-click on-close
             :style {:background "transparent" :color "var(--color-muted)"
                     :border "none" :font-size "1.2rem" :padding "0"}}
            "\u00D7"]]
          [:div.modal__body
           [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
            [field-row "Prénom" (:first-name data)
             #(swap! form assoc :first-name %) {:required? true}]
            [field-row "Nom" (:last-name data)
             #(swap! form assoc :last-name %) {:required? true}]
            [field-row "Date de naissance" (:birth-date data)
             #(swap! form assoc :birth-date %)
             {:type "date" :required? true
              :min "1920-01-01" :max (max-birth-date)}]
            [field-row "Adresse" (:address data)
             #(swap! form assoc :address %) {:required? true}]
            [field-row "Code postal" (:postal-code data)
             #(swap! form assoc :postal-code %) {:required? true}]
            [field-row "Ville" (:city data)
             #(swap! form assoc :city %) {:required? true}]
            [phone-field-row "Téléphone" (:phone data)
             #(swap! form assoc :phone %)
             {:required? true}]]]
          [:div.modal__actions
           [:button.btn.btn--small.btn--outline {:on-click on-close} "Annuler"]
           [:button {:class (str "btn btn--small btn--green" (when-not valid? " btn--disabled"))
                     :style (when-not valid? {:opacity "0.5" :cursor "not-allowed"})
                     :on-click (fn []
                                 (when valid?
                                   (rf/dispatch [:admin/update-user-profile
                                                 (:user/id user) data on-close])))}
            "Enregistrer"]]]]))))

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
  (let [selected-user (r/atom nil)
        editing-user  (r/atom nil)]
    (fn []
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
              [:th "User"]
              [:th "Email"]
              [:th "Fournisseur"]
              [:th "Role"]
              [:th "Statut"]
              [:th ""]]]
            [:tbody
             (for [u users]
               ^{:key (:user/id u)}
               [:tr
                [:td [:a {:href "#" :style {:color "var(--color-primary)" :text-decoration "underline"
                                            :cursor "pointer"}
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (reset! selected-user u))}
                      (:user/name u)]]
                [:td (:user/email u)]
                [:td (:user/provider u)]
                [:td {:class (when (= "admin" (:user/role u)) "admin-table__role--admin")}
                 (:user/role u)]
                [:td (:user/lifecycle u)]
                [:td [:button.btn.btn--small
                      {:on-click #(reset! editing-user u)}
                      "Identité"]]])]])
         (when-let [u @selected-user]
           [user-profile-modal u #(reset! selected-user nil)])
         (when-let [u @editing-user]
           [edit-user-profile-modal u #(reset! editing-user nil)])]))))

;; ── Create network modal ──────────────────────────────────────────────────────

(defn- create-network-modal [on-close]
  (let [form (r/atom {:name "" :center-lat "" :center-lng "" :diameter-km "2" :description "" :price-per-kwh ""})]
    (fn [on-close]
      (let [{:keys [name center-lat center-lng diameter-km description price-per-kwh]} @form
            valid? (and (seq name)
                        (seq center-lat)
                        (seq center-lng)
                        (seq diameter-km))]
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
            [:label "Description"]
            [:textarea.onboarding__input
             {:value       description
              :placeholder "Description du réseau (optionnel)"
              :rows        4
              :style       {:resize "vertical"}
              :on-change   #(swap! form assoc :description (.-value (.-target %)))}]
            [:label "Prix de l'électricité (€HT/kWh)"]
            [:input.onboarding__input
             {:type        "number"
              :step        "0.01"
              :value       price-per-kwh
              :placeholder "ex: 0.17"
              :on-change   #(swap! form assoc :price-per-kwh (.-value (.-target %)))}]
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
            [:label "Diam\u00e8tre (km)"]
            [:input.onboarding__input
             {:type        "number"
              :step        "any"
              :value       diameter-km
              :placeholder "10"
              :on-change   #(swap! form assoc :diameter-km (.-value (.-target %)))}]]]
          [:div.modal__actions
           [:button.btn.btn--small {:on-click on-close} "Annuler"]
           [:button.btn.btn--green.btn--small
            {:disabled (not valid?)
             :on-click (fn []
                         (rf/dispatch [:admin/create-network
                                       (cond-> {:name       name
                                                :center-lat (js/parseFloat center-lat)
                                                :center-lng (js/parseFloat center-lng)
                                                :radius-km  (/ (js/parseFloat diameter-km) 2)}
                                         (seq description)   (assoc :description description)
                                         (seq price-per-kwh) (assoc :price-per-kwh (js/parseFloat price-per-kwh)))])
                         (on-close))}
            "Créer"]]]]))))

;; ── Edit network modal ────────────────────────────────────────────────────────

(defn- edit-network-modal [network on-close]
  (let [form (r/atom {:name          (:network/name network)
                      :center-lat    (str (:network/center-lat network))
                      :center-lng    (str (:network/center-lng network))
                      :diameter-km   (str (* 2 (:network/radius-km network)))
                      :description   (or (:network/description network) "")
                      :price-per-kwh (if (:network/price-per-kwh network)
                                       (str (:network/price-per-kwh network))
                                       "")})
        mousedown-on-overlay? (atom false)]
    (fn [_network on-close]
      (let [{:keys [name center-lat center-lng diameter-km description price-per-kwh]} @form
            valid? (and (seq name) (seq center-lat) (seq center-lng) (seq diameter-km))]
        [:div.modal-overlay {:on-mouse-down (fn [e]
                                              (reset! mousedown-on-overlay?
                                                      (= (.-target e) (.-currentTarget e))))
                             :on-click (fn [e]
                                         (when (and (= (.-target e) (.-currentTarget e))
                                                    @mousedown-on-overlay?)
                                           (on-close)))}
         [:div.modal {:on-click #(.stopPropagation %)}
          [:div.modal__header
           [:span "Modifier le réseau"]
           [:button.btn.btn--small
            {:on-click on-close
             :style {:background "transparent" :color "var(--color-muted)"
                     :border "none" :font-size "1.2rem" :padding "0"}}
            "\u00D7"]]
          [:div.modal__body
           [:div.onboarding__form
            [:label "Nom"]
            [:input.onboarding__input
             {:value     name
              :on-change #(swap! form assoc :name (.-value (.-target %)))}]
            [:label "Description"]
            [:textarea.onboarding__input
             {:value     description
              :rows      4
              :style     {:resize "vertical"}
              :on-change #(swap! form assoc :description (.-value (.-target %)))}]
            [:label "Prix de l'électricité (€HT/kWh)"]
            [:input.onboarding__input
             {:type "number" :step "0.01" :value price-per-kwh
              :placeholder "ex: 0.17"
              :on-change #(swap! form assoc :price-per-kwh (.-value (.-target %)))}]
            [:label "Latitude du centre"]
            [:input.onboarding__input
             {:type "number" :step "any" :value center-lat
              :on-change #(swap! form assoc :center-lat (.-value (.-target %)))}]
            [:label "Longitude du centre"]
            [:input.onboarding__input
             {:type "number" :step "any" :value center-lng
              :on-change #(swap! form assoc :center-lng (.-value (.-target %)))}]
            [:label "Diam\u00e8tre (km)"]
            [:input.onboarding__input
             {:type "number" :step "any" :value diameter-km
              :on-change #(swap! form assoc :diameter-km (.-value (.-target %)))}]]]
          [:div.modal__actions
           [:button.btn.btn--small {:on-click on-close} "Annuler"]
           [:button.btn.btn--green.btn--small
            {:disabled (not valid?)
             :on-click (fn []
                         (rf/dispatch [:admin/update-network
                                       (:network/id network)
                                       (cond-> {:name        name
                                                :center-lat  (js/parseFloat center-lat)
                                                :center-lng  (js/parseFloat center-lng)
                                                :radius-km   (/ (js/parseFloat diameter-km) 2)
                                                :description description}
                                         (seq price-per-kwh) (assoc :price-per-kwh (js/parseFloat price-per-kwh)))])
                         (on-close))}
            "Enregistrer"]]]]))))

;; ── Networks export ──────────────────────────────────────────────────────────

(defn- export-networks-csv [networks]
  (let [header "ID;Nom;Latitude;Longitude;Diam\u00e8tre (km);Statut"
        rows   (map (fn [n]
                      (str/join ";" [(:network/id n)
                                     (:network/name n)
                                     (:network/center-lat n)
                                     (:network/center-lng n)
                                     (* 2 (:network/radius-km n))
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

(defn- pending-networks-table [pending-networks on-edit on-delete]
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
        [:th "Diam\u00e8tre (km)"]
        [:th ""]]]
      [:tbody
       (for [n pending-networks]
         ^{:key (:network/id n)}
         [:tr.admin-table__row--pending
          [:td [:a {:href  (rfee/href :page/network-detail {:id (:network/id n)})
                    :style {:color "var(--color-primary)" :text-decoration "underline" :cursor "pointer"}}
                (:network/name n)]]
          [:td (:network/center-lat n)]
          [:td (:network/center-lng n)]
          [:td (* 2 (:network/radius-km n))]
          [:td
           [:div {:style {:display "flex" :gap "0.25rem"}}
            [:button.btn.btn--small
             {:on-click #(on-edit n)}
             "Éditer"]
            [:button.btn.btn--small.btn--green
             {:on-click #(rf/dispatch [:admin/validate-network (:network/id n)])}
             "Valider"]
            [:button.btn.btn--small
             {:on-click #(on-delete n)
              :style {:background "#d32f2f" :color "#fff" :border "none"}}
             "Supprimer"]]]])]]]))

(defn- confirm-delete-modal [network on-confirm on-cancel]
  [:div.modal-overlay {:on-click on-cancel}
   [:div.modal {:on-click #(.stopPropagation %)}
    [:div.modal__header
     [:span "Supprimer le réseau"]
     [:button.btn.btn--small {:on-click on-cancel
                              :style {:background "transparent" :color "var(--color-muted)"
                                      :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:p {:style {:margin-bottom "0.5rem" :font-weight "600" :color "#d32f2f"}}
      "Cette action est irréversible."]
     [:p {:style {:margin-bottom "0.5rem"}}
      (str "Voulez-vous vraiment supprimer le réseau \"" (:network/name network) "\" ?")]]
    [:div.modal__actions
     [:button.btn.btn--small {:on-click on-cancel} "Annuler"]
     [:button.btn.btn--small {:on-click on-confirm
                              :style {:background "#d32f2f" :color "#fff"}}
      "Supprimer"]]]])

(defn- delete-blocked-modal []
  (let [blocked @(rf/subscribe [:admin/network-delete-blocked])]
    (when blocked
      (let [{:keys [consumptions productions]} blocked
            on-close #(rf/dispatch [:admin/dismiss-network-delete-blocked])]
        [:div.modal-overlay {:on-click on-close}
         [:div.modal {:on-click #(.stopPropagation %)}
          [:div.modal__header
           [:span "Suppression impossible"]
           [:button.btn.btn--small {:on-click on-close
                                    :style {:background "transparent" :color "var(--color-muted)"
                                            :border "none" :font-size "1.2rem" :padding "0"}}
            "\u00D7"]]
          [:div.modal__body
           [:p {:style {:margin-bottom "1rem"}}
            "Ce réseau ne peut pas être supprimé car il contient des consommations et/ou des productions. "
            "Veuillez les supprimer ou les réaffecter avant de supprimer le réseau."]
           (when (seq consumptions)
             [:div {:style {:margin-bottom "1rem"}}
              [:h4 {:style {:font-size "0.95rem" :margin-bottom "0.5rem"}}
               (str (count consumptions) " consommation(s)")]
              [:ul {:style {:font-size "0.85rem" :color "var(--color-muted)" :padding-left "1.25rem"}}
               (doall
                 (for [c consumptions]
                   ^{:key (str (:consumption/id c))}
                   [:li
                    [:div {:style {:margin-bottom "0.25rem"}}
                     [:span {:style {:font-family "monospace" :font-size "0.8rem"}}
                      (str (:consumption/id c))]
                     [:span " — " (consumption-lifecycle-label (name (:consumption/lifecycle c)))]]
                    (when (or (:user/name c) (:user/email c))
                      [:div {:style {:font-size "0.8rem" :color "var(--color-muted)" :margin-left "0.5rem"}}
                       (when (:user/name c) [:span (:user/name c)])
                       (when (:user/email c) [:span " (" (:user/email c) ")"])])]))]])
           (when (seq productions)
             [:div
              [:h4 {:style {:font-size "0.95rem" :margin-bottom "0.5rem"}}
               (str (count productions) " production(s)")]
              [:ul {:style {:font-size "0.85rem" :color "var(--color-muted)" :padding-left "1.25rem"}}
               (doall
                 (for [p productions]
                   ^{:key (str (:production/id p))}
                   [:li
                    [:div {:style {:margin-bottom "0.25rem"}}
                     [:span {:style {:font-family "monospace" :font-size "0.8rem"}}
                      (str (:production/id p))]
                     [:span " — " (:production/lifecycle p)]]
                    (when (or (:user/name p) (:user/email p))
                      [:div {:style {:font-size "0.8rem" :color "var(--color-muted)" :margin-left "0.5rem"}}
                       (when (:user/name p) [:span (:user/name p)])
                       (when (:user/email p) [:span " (" (:user/email p) ")"])])]))]])]
          [:div.modal__actions
           [:button.btn.btn--small.btn--green {:on-click on-close} "Compris"]]]]))))

(defn networks-tab []
  (let [show-modal?       (r/atom false)
        confirm-network   (r/atom nil)
        delete-network    (r/atom nil)
        edit-network      (r/atom nil)]
    (fn []
      (let [all-networks @(rf/subscribe [:admin/networks])
            loading?     @(rf/subscribe [:admin/networks-loading?])
            pending      (filterv #(= "pending-validation" (:network/lifecycle %)) all-networks)
            networks     (filterv #(not= "pending-validation" (:network/lifecycle %)) all-networks)
            public-count (count (filterv #(= "public" (:network/lifecycle %)) all-networks))]
        [:div
         [:div {:style {:display "flex" :align-items "center" :gap "0.75rem" :margin-bottom "0.75rem"}}
          [:span {:style {:background "var(--color-green)" :color "#fff" :padding "0.25rem 0.75rem"
                          :border-radius "var(--radius)" :font-weight "600" :font-size "0.9rem"}}
           (str public-count " réseau" (when (> public-count 1) "x") " public" (when (> public-count 1) "s"))]]
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
            [pending-networks-table pending
             #(reset! edit-network %)
             #(reset! delete-network %)]
            (when (seq networks)
              [:table.admin-table
               [:thead
                [:tr
                 [:th "Nom"]
                 [:th "Latitude"]
                 [:th "Longitude"]
                 [:th "Diam\u00e8tre (km)"]
                 [:th "Statut"]
                 [:th ""]]]
               [:tbody
                (for [n networks]
                  ^{:key (:network/id n)}
                  [:tr {:class (when (not= "public" (:network/lifecycle n)) "admin-table__row--disabled")}
                   [:td [:a {:href  (rfee/href :page/network-detail {:id (:network/id n)})
                             :style {:color "var(--color-primary)" :text-decoration "underline" :cursor "pointer"}}
                         (:network/name n)]]
                   [:td (:network/center-lat n)]
                   [:td (:network/center-lng n)]
                   [:td (* 2 (:network/radius-km n))]
                   [:td {:class (network-status-class (:network/lifecycle n))}
                    (network-status-label (:network/lifecycle n))]
                   [:td
                    [:div {:style {:display "flex" :gap "0.25rem" :flex-wrap "nowrap"}}
                     [:button.btn.btn--small
                      {:on-click #(reset! edit-network n)}
                      "Éditer"]
                     [:button.btn.btn--small
                      {:on-click (fn []
                                   (if (and (= "public" (:network/lifecycle n))
                                            (pos? (or (:network/consumption-count n) 0)))
                                     (reset! confirm-network n)
                                     (rf/dispatch [:admin/toggle-network-visibility (:network/id n)])))}
                      (if (= "public" (:network/lifecycle n))
                        "Rendre privé"
                        "Rendre public")]
                     [:button.btn.btn--small
                      {:on-click #(reset! delete-network n)}
                      "Supprimer"]]]])]])])
         (when @show-modal?
           [create-network-modal #(reset! show-modal? false)])
         (when-let [net @confirm-network]
           [confirm-unpublish-modal net
            (fn []
              (rf/dispatch [:admin/toggle-network-visibility (:network/id net)])
              (reset! confirm-network nil))
            #(reset! confirm-network nil)])
         (when-let [net @edit-network]
           [edit-network-modal net #(reset! edit-network nil)])
         (when-let [net @delete-network]
           [confirm-delete-modal net
            (fn []
              (rf/dispatch [:admin/delete-network (:network/id net)])
              (reset! delete-network nil))
            #(reset! delete-network nil)])
         [delete-blocked-modal]]))))

;; ── Productions table ────────────────────────────────────────────────────────

(def ^:private energy-type-labels
  {"solar"        "Solaire"
   "wind"         "Eolien"
   "hydro"        "Hydraulique"
   "biomass"      "Biomasse"
   "cogeneration" "Cogeneration"})

(defn- export-productions-csv [productions]
  (let [header "ID;Utilisateur;Email;Adresse;Réseau;PDL/PRM;Puissance (kWh);Type;Compteur Linky;IBAN;Statut"
        rows   (map (fn [p]
                      (str/join ";" [(:production/id p)
                                     (or (:production/user-name p) (:production/user-id p))
                                     (or (:production/user-email p) "")
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

(defn- activation-error-modal [error-msg]
  [:div.modal-overlay
   {:on-click #(rf/dispatch [:admin/dismiss-production-error])}
   [:div.modal
    {:on-click #(.stopPropagation %)}
    [:div.modal__header
     [:span "Activation impossible"]
     [:button.btn.btn--small
      {:on-click #(rf/dispatch [:admin/dismiss-production-error])
       :style {:background "transparent" :color "var(--color-muted)"
               :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:p {:style {:color "#d32f2f" :font-weight "600" :margin-bottom "0.5rem"}}
      error-msg]
     [:p "Le réseau associé à cette production doit d'abord être validé avant de pouvoir activer la production."]]
    [:div.modal__actions
     [:button.btn.btn--small.btn--green
      {:on-click #(rf/dispatch [:admin/dismiss-production-error])}
      "Compris"]]]])

;; ── Consumptions tab ──────────────────────────────────────────────────────────

(defn- consumption-lifecycle-label [lifecycle]
  (case lifecycle
    "consumer-information" "Étape 1 — Adresse"
    "linky-reference"      "Étape 2 — Linky"
    "billing-address"      "Étape 3 — Facturation"
    "contract-signature"   "Étape 4 — Contrats"
    "pending"              "En attente"
    "active"               "Active"
    "terminated"           "Résiliée"
    "abandoned"            "Abandonnée"
    lifecycle))

(defn- consumption-status-class [lifecycle]
  (case lifecycle
    "active"    "admin-table__status--public"
    "pending"   "admin-table__status--pending"
    "abandoned" "admin-table__status--private"
    "terminated" "admin-table__status--private"
    nil))

(defn- export-consumptions-csv [consumptions]
  (let [header "ID;Utilisateur;Email;Réseau;Statut;Réf. Linky;Créé le"
        rows   (map (fn [c]
                      (str/join ";" [(subs (str (:consumption/id c)) 0 8)
                                     (or (:consumption/user-name c) "")
                                     (or (:consumption/user-email c) "")
                                     (or (:consumption/network-name c) "")
                                     (:consumption/lifecycle c)
                                     (or (:consumption/linky-reference c) "")
                                     (subs (str (:consumption/id c)) 0 8)]))
                    consumptions)
        csv    (str/join "\n" (cons header rows))
        blob   (js/Blob. #js [csv] #js {:type "text/csv;charset=utf-8;"})
        url    (.createObjectURL js/URL blob)
        a      (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "consommations.csv")
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn- format-datetime-fr
  "Formats an ISO datetime string to French format: dd/MM/yyyy HH:mm:ss"
  [s]
  (when (and (string? s) (seq s))
    (try
      (let [d (js/Date. s)
            pad #(if (< % 10) (str "0" %) (str %))]
        (str (pad (.getDate d)) "/" (pad (inc (.getMonth d))) "/" (.getFullYear d)
             " " (pad (.getHours d)) ":" (pad (.getMinutes d)) ":" (pad (.getSeconds d))))
      (catch :default _ s))))

(defn- detail-field [label value]
  (when value
    [:div
     [:span {:style {:font-size "0.8rem" :color "var(--color-muted)"}} label]
     [:p {:style {:font-weight "600" :margin "0 0 0.5rem 0"}} value]]))

(defn- consumption-detail-modal [c on-close]
  (let [confirm-delete? (r/atom false)]
    (fn [c on-close]
      [:div.modal-overlay {:on-click (fn [e]
                                        (when (= (.-target e) (.-currentTarget e))
                                          (on-close)))}
       [:div.modal {:on-click #(.stopPropagation %)
                    :style {:max-width "600px"}}
        [:div.modal__header
         [:span (str "Consommation — "
                     (or (:consumption/user-name c)
                         (subs (str (:consumption/user-id c)) 0 8)))]
         [:button.btn.btn--small
          {:on-click on-close
           :style {:background "transparent" :color "var(--color-muted)"
                   :border "none" :font-size "1.2rem" :padding "0"}}
          "\u00D7"]]
        [:div.modal__body
         [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "0.5rem 1.5rem"}}
          [detail-field "Statut"
           (consumption-lifecycle-label (:consumption/lifecycle c))]
          [detail-field "Utilisateur"
           (or (:consumption/user-name c) (subs (str (:consumption/user-id c)) 0 8))]
          [detail-field "Email" (:consumption/user-email c)]
          [detail-field "Réseau"
           (or (:consumption/network-name c) (:consumption/network-id c))]
          [detail-field "Adresse de consommation" (:consumption/consumer-address c)]
          [detail-field "Référence Linky" (:consumption/linky-reference c)]
          [detail-field "Adresse de facturation" (:consumption/billing-address c)]
          [detail-field "IBAN"
           (when-let [iban (:consumption/iban c)]
             (if (> (count iban) 8)
               (str (subs iban 0 4) " •••• •••• " (subs iban (- (count iban) 4)))
               iban))]
          [detail-field "BIC" (:consumption/bic c)]
          [detail-field "Contrat producteur signé le"
           (format-datetime-fr (:consumption/producer-contract-signed-at c))]
          [detail-field "Mandat SEPA signé le"
           (format-datetime-fr (:consumption/sepa-mandate-signed-at c))]]
         (when @confirm-delete?
           [:div {:style {:margin-top "1rem" :padding "1rem"
                          :background "#fdecea" :border "1px solid #f5c6cb"
                          :border-radius "var(--radius)"}}
            (when (= "active" (:consumption/lifecycle c))
              [:p {:style {:color "#d32f2f" :font-weight "700" :margin-bottom "0.5rem"
                           :animation "blink 1s step-end infinite"}}
               "Attention, consommation active !"])
            [:p {:style {:font-weight "600" :margin-bottom "0.75rem"}}
             "Cette action est irréversible. Confirmer la suppression ?"]
            [:div {:style {:display "flex" :gap "0.5rem" :justify-content "flex-end"}}
             [:button.btn.btn--small.btn--outline
              {:on-click #(reset! confirm-delete? false)}
              "Annuler"]
             [:button.btn.btn--small
              {:style {:background "#d32f2f" :color "#fff" :border "none"}
               :on-click (fn []
                           (rf/dispatch [:admin/delete-consumption (:consumption/id c)])
                           (on-close))}
              "Confirmer la suppression"]]])]
        [:div.modal__actions
         (when (= "pending" (:consumption/lifecycle c))
           [:button.btn.btn--green.btn--small
            {:on-click (fn []
                         (rf/dispatch [:admin/activate-consumption (:consumption/id c)])
                         (on-close))}
            "Activer"])
         [:button.btn.btn--small
          {:style {:background "#d32f2f" :color "#fff" :border "none"}
           :on-click #(reset! confirm-delete? true)}
          "Supprimer"]
         [:button.btn.btn--small.btn--outline
          {:on-click on-close}
          "Fermer"]]]])))

(defn- month-label [m]
  (get {1 "Jan" 2 "Fév" 3 "Mar" 4 "Avr" 5 "Mai" 6 "Juin"
        7 "Juil" 8 "Août" 9 "Sep" 10 "Oct" 11 "Nov" 12 "Déc"} m "?"))

(defn- monthly-history-modal [consumption on-close]
  (let [now    (js/Date.)
        cur-y  (.getFullYear now)
        cur-m  (inc (.getMonth now))
        existing (or (:consumption/monthly-history consumption) [])
        ;; Initialize with existing entries, or prefill 6 months if empty
        init   (if (seq existing)
                 (mapv (fn [e] {:year (int (:year e)) :month (int (:month e)) :kwh (str (:kwh e))})
                       (sort-by (juxt :year :month) #(compare %2 %1) existing))
                 (vec (for [i (range 6)]
                        (let [m (- cur-m i)
                              y (if (<= m 0) (dec cur-y) cur-y)
                              m (if (<= m 0) (+ 12 m) m)]
                          {:year y :month m :kwh ""}))))
        form   (r/atom init)]
    (fn [_consumption on-close]
      (let [entries @form
            can-save? (every? #(and (pos? (:year %))
                                   (<= 1 (:month %) 12)
                                   (seq (str (:kwh %)))
                                   (js/parseFloat (str (:kwh %))))
                              entries)]
        [:div.modal-overlay {:on-click (fn [e]
                                         (when (= (.-target e) (.-currentTarget e))
                                           (on-close)))}
         [:div.modal {:on-click #(.stopPropagation %)
                      :style {:max-width "500px"}}
          [:div.modal__header
           [:span "Historique consommation"]
           [:button.btn.btn--small {:on-click on-close
                                    :style {:background "transparent" :color "var(--color-muted)"
                                            :border "none" :font-size "1.2rem" :padding "0"}}
            "\u00D7"]]
          [:div.modal__body
           [:table {:style {:width "100%" :border-collapse "collapse"}}
            [:thead
             [:tr
              [:th {:style {:text-align "left" :padding "0.3rem" :font-size "0.8rem" :color "var(--color-muted)"}} "Année"]
              [:th {:style {:text-align "left" :padding "0.3rem" :font-size "0.8rem" :color "var(--color-muted)"}} "Mois"]
              [:th {:style {:text-align "left" :padding "0.3rem" :font-size "0.8rem" :color "var(--color-muted)"}} "kWh"]
              [:th {:style {:width "30px"}}]]]
            [:tbody
             (doall
               (for [idx (range (count entries))]
                 ^{:key idx}
                 [:tr
                  [:td {:style {:padding "0.2rem 0.3rem"}}
                   [:input.onboarding__input
                    {:type "number" :value (:year (nth entries idx))
                     :style {:width "80px" :margin "0" :padding "0.3rem"}
                     :on-change #(swap! form assoc-in [idx :year] (js/parseInt (.. % -target -value)))}]]
                  [:td {:style {:padding "0.2rem 0.3rem"}}
                   [:select.onboarding__input
                    {:value (:month (nth entries idx))
                     :style {:margin "0" :padding "0.3rem"}
                     :on-change #(swap! form assoc-in [idx :month] (js/parseInt (.. % -target -value)))}
                    (for [m (range 1 13)]
                      ^{:key m}
                      [:option {:value m} (month-label m)])]]
                  [:td {:style {:padding "0.2rem 0.3rem"}}
                   [:input.onboarding__input
                    {:type "number" :step "0.1" :value (:kwh (nth entries idx))
                     :style {:width "90px" :margin "0" :padding "0.3rem"}
                     :on-change #(swap! form assoc-in [idx :kwh] (.. % -target -value))}]]
                  [:td {:style {:padding "0.2rem 0.3rem"}}
                   [:button {:on-click #(swap! form (fn [v] (vec (concat (subvec v 0 idx) (subvec v (inc idx))))))
                             :style {:background "transparent" :border "none" :cursor "pointer"
                                     :color "#d32f2f" :font-size "1.1rem" :padding "0"}}
                    "\u00D7"]]]))]]
           [:button.btn.btn--small.btn--outline
            {:on-click #(swap! form conj {:year cur-y :month cur-m :kwh ""})
             :style {:margin-top "0.5rem"}}
            "+ Ajouter un mois"]]
          [:div.modal__actions
           [:button.btn.btn--small {:on-click on-close} "Annuler"]
           [:button.btn.btn--small.btn--green
            {:on-click (fn []
                         (let [parsed (mapv (fn [e]
                                              {:year  (int (:year e))
                                               :month (int (:month e))
                                               :kwh   (js/parseFloat (str (:kwh e)))})
                                            entries)]
                           (rf/dispatch [:admin/update-monthly-history
                                         (:consumption/id consumption)
                                         parsed
                                         on-close])))
             :disabled (not can-save?)}
            "Enregistrer"]]]]))))

(defn- consumptions-table [consumptions selected history-modal]
  [:table.admin-table
   [:thead
    [:tr
     [:th "Utilisateur"]
     [:th "Réseau"]
     [:th "Statut"]
     [:th "Réf. Linky"]
     [:th "Actions"]]]
   [:tbody
    (doall
      (for [c consumptions]
        ^{:key (:consumption/id c)}
        [:tr {:style    {:cursor "pointer"}
              :on-click #(reset! selected c)}
         [:td
          [:div (or (:consumption/user-name c)
                    (subs (str (:consumption/user-id c)) 0 8))]
          (when-let [email (:consumption/user-email c)]
            [:div {:style {:font-size "0.8rem" :color "var(--color-muted)"}} email])]
         [:td (if-let [nid (:consumption/network-id c)]
                [:a {:href (rfee/href :page/network-detail {:id nid})
                     :style {:color "var(--color-green)" :text-decoration "underline"}
                     :on-click #(.stopPropagation %)}
                 (or (:consumption/network-name c) (subs nid 0 8))]
                "—")]
         [:td [:span {:class (consumption-status-class (:consumption/lifecycle c))}
               (consumption-lifecycle-label (:consumption/lifecycle c))]]
         [:td (or (:consumption/linky-reference c) "—")]
         [:td {:style {:display "flex" :gap "0.3rem"}}
          (when (= "pending" (:consumption/lifecycle c))
            [:button.btn.btn--green.btn--small
             {:on-click (fn [e]
                          (.stopPropagation e)
                          (rf/dispatch [:admin/activate-consumption (:consumption/id c)]))}
             "Activer"])
          [:button.btn.btn--small.btn--outline
           {:on-click (fn [e]
                        (.stopPropagation e)
                        (reset! history-modal c))}
           "Conso"]]]))]])

(defn consumptions-tab []
  (let [selected      (r/atom nil)
        history-modal (r/atom nil)]
    (fn []
      (let [consumptions @(rf/subscribe [:admin/consumptions])
            loading?     @(rf/subscribe [:admin/consumptions-loading?])]
        [:div
         [:div.consumptions__header
          [:h2.admin__tab-title "Consommations"]
          [:button.btn.btn--small
           {:on-click #(export-consumptions-csv consumptions)
            :disabled (empty? consumptions)
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
           loading?              [:p.loading "Chargement..."]
           (empty? consumptions) [:p.admin__empty "Aucune consommation."]
           :else                 [consumptions-table consumptions selected history-modal])
         (when-let [c @selected]
           [consumption-detail-modal c #(reset! selected nil)])
         (when-let [c @history-modal]
           [monthly-history-modal c #(reset! history-modal nil)])]))))

;; ── Productions tab ──────────────────────────────────────────────────────────

(defn- production-detail-modal [p on-close]
  [:div.modal-overlay {:on-click (fn [e]
                                    (when (= (.-target e) (.-currentTarget e))
                                      (on-close)))}
   [:div.modal {:on-click #(.stopPropagation %)
                :style {:max-width "600px"}}
    [:div.modal__header
     [:span (str "Production — "
                 (or (:production/user-name p)
                     (subs (str (:production/user-id p)) 0 8)))]
     [:button.btn.btn--small
      {:on-click on-close
       :style {:background "transparent" :color "var(--color-muted)"
               :border "none" :font-size "1.2rem" :padding "0"}}
      "\u00D7"]]
    [:div.modal__body
     [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "0.5rem 1.5rem"}}
      [detail-field "Statut" (:production/lifecycle p)]
      [detail-field "Utilisateur"
       (or (:production/user-name p) (subs (str (:production/user-id p)) 0 8))]
      [detail-field "Email" (:production/user-email p)]
      [detail-field "Réseau"
       (or (:production/network-name p) (:production/network-id p))]
      [detail-field "Adresse de production" (:production/producer-address p)]
      [detail-field "PDL / PRM" (:production/pdl-prm p)]
      [detail-field "Puissance installée"
       (when-let [pw (:production/installed-power p)] (str pw " kWh"))]
      [detail-field "Type d'énergie"
       (get energy-type-labels (:production/energy-type p))]
      [detail-field "Compteur Linky" (:production/linky-meter p)]
      [detail-field "IBAN"
       (when-let [iban (:production/iban p)]
         (if (> (count iban) 8)
           (str (subs iban 0 4) " •••• •••• " (subs iban (- (count iban) 4)))
           iban))]
      [detail-field "Contrat signé le" (format-datetime-fr (:production/adhesion-signed-at p))]]]
    [:div.modal__actions
     (when (= "pending" (:production/lifecycle p))
       [:button.btn.btn--green.btn--small
        {:on-click (fn []
                     (rf/dispatch [:admin/activate-production (:production/id p)])
                     (on-close))}
        "Activer"])
     [:button.btn.btn--small.btn--outline
      {:on-click on-close}
      "Fermer"]]]])

(defn- productions-table [productions selected]
  [:table.admin-table
   [:thead
    [:tr
     [:th "Utilisateur"]
     [:th "Puissance"]
     [:th "Type"]
     [:th "Compteur"]
     [:th "Statut"]
     [:th "Actions"]]]
   [:tbody
    (doall
      (for [p productions]
        ^{:key (:production/id p)}
        [:tr {:style    {:cursor "pointer"}
              :on-click #(reset! selected p)}
         [:td
          [:div (or (:production/user-name p) (subs (str (:production/user-id p)) 0 8))]
          (when-let [email (:production/user-email p)]
            [:div {:style {:font-size "0.8rem" :color "var(--color-muted)"}} email])]
         [:td (when-let [pw (:production/installed-power p)] (str pw " kWh"))]
         [:td (get energy-type-labels (:production/energy-type p) "-")]
         [:td (or (:production/linky-meter p) "-")]
         [:td (:production/lifecycle p)]
         [:td (when (= "pending" (:production/lifecycle p))
                [:button.btn.btn--green.btn--small
                 {:on-click (fn [e]
                              (.stopPropagation e)
                              (rf/dispatch [:admin/activate-production (:production/id p)]))}
                 "Activer"])]]))]])

(defn productions-tab []
  (let [selected (r/atom nil)]
    (fn []
      (let [productions @(rf/subscribe [:admin/productions])
            loading?    @(rf/subscribe [:admin/productions-loading?])
            error-msg   @(rf/subscribe [:admin/production-error])]
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
           loading?         [:p.loading "Chargement..."]
           (empty? productions) [:p.admin__empty "Aucune production."]
           :else            [productions-table productions selected])
         (when-let [p @selected]
           [production-detail-modal p #(reset! selected nil)])
         (when error-msg
           [activation-error-modal error-msg])]))))

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

;; ── News tab ────────────────────────────────────────────────────────────────

(defn- news-format-date [iso-str]
  (when iso-str
    (try
      (let [d (js/Date. iso-str)]
        (str (.getDate d) "/" (inc (.getMonth d)) "/" (.getFullYear d)))
      (catch :default _ iso-str))))

(defn- news-edit-modal [news-item on-close]
  (let [creating? (nil? news-item)
        draft     (r/atom (if creating?
                            {:title "" :content "" :image-url "" :published true}
                            {:title        (or (:news/title news-item) "")
                             :content      (or (:news/content news-item) "")
                             :image-url    (or (:news/image-url news-item) "")
                             :published    (some? (:news/published-at news-item))}))]
    (fn [_news-item on-close]
      (let [{:keys [title content image-url published]} @draft
            valid? (and (seq (str/trim title)) (seq content))]
        [:div.modal-overlay {:on-click (fn [e]
                                         (when (= (.-target e) (.-currentTarget e))
                                           (on-close)))}
         [:div.modal {:on-click #(.stopPropagation %)
                      :style {:max-width "600px"}}
          [:div.modal__header
           [:span (if creating? "Nouvelle actualit\u00e9" "Modifier l'actualit\u00e9")]
           [:button.btn.btn--small {:on-click on-close
                                    :style {:background "transparent" :color "var(--color-muted)"
                                            :border "none" :font-size "1.2rem" :padding "0"}}
            "\u00D7"]]
          [:div.modal__body
           [:div {:style {:display "flex" :flex-direction "column" :gap "0.75rem"}}
            [:div
             [:label {:style {:font-weight "600" :font-size "0.85rem" :display "block" :margin-bottom "4px"}} "Titre"]
             [:input.onboarding__input
              {:type "text" :value title
               :style {:margin "0"}
               :on-change #(swap! draft assoc :title (.. % -target -value))}]]
            [:div
             [:label {:style {:font-weight "600" :font-size "0.85rem" :display "block" :margin-bottom "4px"}} "Contenu"]
             [:textarea.onboarding__input
              {:rows 8 :value content
               :style {:margin "0"}
               :on-change #(swap! draft assoc :content (.. % -target -value))}]]
            [:div
             [:label {:style {:font-weight "600" :font-size "0.85rem" :display "block" :margin-bottom "4px"}} "URL de l'image"]
             [:input.onboarding__input
              {:type "text" :value image-url :placeholder "https://..."
               :style {:margin "0"}
               :on-change #(swap! draft assoc :image-url (.. % -target -value))}]
             (when (seq image-url)
               [:img {:src image-url :alt "Preview"
                      :style {:max-width "100%" :max-height "150px" :margin-top "0.5rem"
                              :border-radius "var(--radius)" :object-fit "cover"}}])]
            [:div {:style {:display "flex" :align-items "center" :gap "0.5rem"}}
             [:input {:type "checkbox" :checked published
                      :on-change #(swap! draft assoc :published (.. % -target -checked))}]
             [:label {:style {:font-size "0.85rem"}} "Publi\u00e9"]]]]
          [:div.modal__actions
           [:button.btn.btn--small {:on-click on-close} "Annuler"]
           [:button.btn.btn--small.btn--green
            {:on-click (fn []
                         (let [params {:title     (str/trim title)
                                       :content   content
                                       :image-url (when (seq image-url) image-url)
                                       :published-at (when published
                                                       (or (:news/published-at news-item)
                                                           (.toISOString (js/Date.))))}]
                           (if creating?
                             (rf/dispatch [:admin/create-news params on-close])
                             (rf/dispatch [:admin/update-news (:news/id news-item) params on-close]))))
             :disabled (not valid?)}
            "Enregistrer"]]]]))))

(defn- news-table [news-list edit-modal]
  [:table.admin-table
   [:thead
    [:tr
     [:th "Titre"]
     [:th "Date"]
     [:th "Statut"]
     [:th "Actions"]]]
   [:tbody
    (doall
      (for [n news-list]
        ^{:key (:news/id n)}
        [:tr
         [:td {:style {:max-width "300px" :overflow "hidden" :text-overflow "ellipsis" :white-space "nowrap"}}
          (:news/title n)]
         [:td (news-format-date (or (:news/published-at n) (:news/created-at n)))]
         [:td (if (:news/published-at n)
                [:span {:style {:color "#2e7d32"}} "Publi\u00e9"]
                [:span {:style {:color "#e65100"}} "Brouillon"])]
         [:td {:style {:display "flex" :gap "0.3rem"}}
          [:button.btn.btn--small.btn--outline
           {:on-click #(reset! edit-modal n)}
           "\u00c9diter"]
          [:button.btn.btn--small
           {:on-click #(when (js/confirm "Supprimer cette actualit\u00e9 ?")
                         (rf/dispatch [:admin/delete-news (:news/id n)]))
            :style {:background "#d32f2f" :color "#fff"}}
           "Supprimer"]]]))]])

(defn news-tab []
  (let [edit-modal (r/atom nil)]
    (fn []
      (let [news-list @(rf/subscribe [:admin/news-list])
            loading?  @(rf/subscribe [:admin/news-loading?])]
        [:div
         [:div {:style {:display "flex" :justify-content "space-between" :align-items "center"
                        :margin-bottom "1rem"}}
          [:h2.admin__tab-title "Actualit\u00e9s"]
          [:button.btn.btn--green.btn--small
           {:on-click #(reset! edit-modal :new)}
           "+ Nouvelle"]]
         (cond
           loading?           [:p.loading "Chargement..."]
           (empty? news-list) [:p.admin__empty "Aucune actualit\u00e9."]
           :else              [news-table news-list edit-modal])
         (when-let [item @edit-modal]
           [news-edit-modal
            (when (not= item :new) item)
            #(reset! edit-modal nil)])]))))

;; ── Contracts tab ─────────────────────────────────────────────────────────────

(def ^:private energy-type-labels
  {"solar"        "Solaire"
   "wind"         "\u00c9olien"
   "hydro"        "Hydraulique"
   "biomass"      "Biomasse"
   "cogeneration" "Cog\u00e9n\u00e9ration"})

(defn- contract-status-badge [signed-at]
  (if signed-at
    [:span {:style {:background "#e8f5e9" :color "#2a4137" :padding "2px 10px"
                    :border-radius "12px" :font-size "0.8rem" :font-weight "600"}}
     (str "Sign\u00e9 le " (format-datetime-fr signed-at))]
    [:span {:style {:background "#fff3e0" :color "#e65100" :padding "2px 10px"
                    :border-radius "12px" :font-size "0.8rem" :font-weight "600"}}
     "Non sign\u00e9"]))

(defn- contracts-table-section [title rows]
  (when (seq rows)
    [:<>
     [:h3 {:style {:margin "1.5rem 0 0.75rem" :color "var(--color-green)" :font-size "1rem"}} title]
     [:table.admin-table
      [:thead
       [:tr
        [:th "Utilisateur"]
        [:th "Type"]
        [:th "D\u00e9tail"]
        [:th "Statut"]]]
      [:tbody
       (doall
         (for [{:keys [id user-name user-email contract-type detail signed-at]} rows]
           ^{:key id}
           [:tr
            [:td
             [:div {:style {:font-weight "600"}} (or user-name "-")]
             [:div {:style {:font-size "0.8rem" :color "var(--color-muted)"}} (or user-email "")]]
            [:td contract-type]
            [:td (or detail "")]
            [:td [contract-status-badge signed-at]]]))]]]))

(defn- export-contracts-csv [adhesions productions consumptions]
  (let [all-rows (concat
                   (map (fn [r] [(:user-name r) (:user-email r) (:contract-type r) (:detail r) (or (:signed-at r) "")]) adhesions)
                   (map (fn [r] [(:user-name r) (:user-email r) (:contract-type r) (:detail r) (or (:signed-at r) "")]) productions)
                   (map (fn [r] [(:user-name r) (:user-email r) (:contract-type r) (:detail r) (or (:signed-at r) "")]) consumptions))
        header "Utilisateur,Email,Type,D\u00e9tail,Sign\u00e9 le"
        rows   (map (fn [[n e t d s]] (str/join "," [n e t d s])) all-rows)
        csv    (str/join "\n" (cons header rows))
        blob   (js/Blob. #js [csv] #js {:type "text/csv;charset=utf-8;"})
        url    (.createObjectURL js/URL blob)
        a      (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) "contrats.csv")
    (.click a)
    (.revokeObjectURL js/URL url)))

(defn contracts-tab []
  (fn []
    (let [users         @(rf/subscribe [:admin/users])
          consumptions  @(rf/subscribe [:admin/consumptions])
          productions   @(rf/subscribe [:admin/productions])
          users-load?   @(rf/subscribe [:admin/users-loading?])
          conso-load?   @(rf/subscribe [:admin/consumptions-loading?])
          prod-load?    @(rf/subscribe [:admin/productions-loading?])
          loading?      (or users-load? conso-load? prod-load?)

          ;; Adhesions from users
          adhesion-rows (keep (fn [u]
                                (when (:adhesion-signed-at u)
                                  {:id            (str "adh-" (:user/id u))
                                   :user-name     (or (:user/name u) (:user/email u))
                                   :user-email    (:user/email u)
                                   :contract-type "Adh\u00e9sion"
                                   :detail        "Association Elink-co"
                                   :signed-at     (:adhesion-signed-at u)}))
                              users)

          ;; Production contracts
          prod-rows     (keep (fn [p]
                                (when (:production/adhesion-signed-at p)
                                  {:id            (str "prod-" (:production/id p))
                                   :user-name     (:production/user-name p)
                                   :user-email    (:production/user-email p)
                                   :contract-type "Producteur"
                                   :detail        (str (get energy-type-labels (:production/energy-type p) (:production/energy-type p))
                                                       " \u2014 " (:production/installed-power p) " kWh")
                                   :signed-at     (:production/adhesion-signed-at p)}))
                              productions)

          ;; Consumption contracts (producer + SEPA)
          conso-rows    (mapcat (fn [c]
                                  (let [cid  (:consumption/id c)
                                        name (:consumption/user-name c)
                                        email (:consumption/user-email c)
                                        net  (or (:consumption/network-name c) "")]
                                    (cond-> []
                                      (:consumption/producer-contract-signed-at c)
                                      (conj {:id            (str "conso-prod-" cid)
                                             :user-name     name
                                             :user-email    email
                                             :contract-type "Contrat Producteur"
                                             :detail        net
                                             :signed-at     (:consumption/producer-contract-signed-at c)})

                                      (:consumption/sepa-mandate-signed-at c)
                                      (conj {:id            (str "conso-sepa-" cid)
                                             :user-name     name
                                             :user-email    email
                                             :contract-type "Mandat SEPA"
                                             :detail        net
                                             :signed-at     (:consumption/sepa-mandate-signed-at c)}))))
                                consumptions)]
      [:div
       [:div {:style {:display "flex" :justify-content "space-between" :align-items "center"
                      :margin-bottom "1rem"}}
        [:h2.admin__tab-title "Contrats"]
        [:button.btn.btn--green.btn--small
         {:on-click #(export-contracts-csv adhesion-rows prod-rows conso-rows)}
         "Exporter CSV"]]
       (cond
         loading?
         [:p.loading "Chargement..."]

         (and (empty? adhesion-rows) (empty? prod-rows) (empty? conso-rows))
         [:p.admin__empty "Aucun contrat sign\u00e9."]

         :else
         [:div
          [contracts-table-section "Adh\u00e9sions" adhesion-rows]
          [contracts-table-section "Productions" prod-rows]
          [contracts-table-section "Consommations" conso-rows]])])))
