(ns app.pages.profile
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- field-row [label value on-change & [{:keys [placeholder type required?]}]]
  [:div {:style {:display "flex" :flex-direction "column" :gap "0.25rem"}}
   [:label {:style {:font-size "0.85rem" :font-weight "600" :color "var(--color-text)"}}
    label (when required? [:span {:style {:color "#d32f2f" :margin-left "2px"}} "*"])]
   [:input {:type        (or type "text")
            :value       (or value "")
            :placeholder (or placeholder "")
            :on-change   #(on-change (-> % .-target .-value))
            :style       {:padding "0.5rem 0.75rem" :border "1px solid var(--color-border)"
                          :border-radius "var(--radius)" :font-size "0.95rem"}}]])

;; ── Natural person ──────────────────────────────────────────────────────────

(def ^:private natural-required-fields
  [:last-name :first-name :birth-date :birth-place :postal-address :phone])

(defn- natural-person-valid? [data]
  (every? #(seq (str (get data %))) natural-required-fields))

(defn- natural-person-section [form saved?]
  (fn [form saved?]
    (let [data   @form
          valid? (natural-person-valid? data)]
      [:<>
       [:h3 {:style {:margin-bottom "1rem" :font-size "1.1rem"}} "Identité"]
       [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
        [field-row "Nom" (:last-name data)
         #(do (swap! form assoc :last-name %) (reset! saved? false)) {:required? true}]
        [field-row "Prénom" (:first-name data)
         #(do (swap! form assoc :first-name %) (reset! saved? false)) {:required? true}]
        [field-row "Date de naissance" (:birth-date data)
         #(do (swap! form assoc :birth-date %) (reset! saved? false))
         {:type "date" :required? true}]
        [field-row "Lieu de naissance" (:birth-place data)
         #(do (swap! form assoc :birth-place %) (reset! saved? false)) {:required? true}]
        [field-row "Profession" (:profession data)
         #(do (swap! form assoc :profession %) (reset! saved? false))]
        [field-row "Adresse postale" (:postal-address data)
         #(do (swap! form assoc :postal-address %) (reset! saved? false)) {:required? true}]
        [field-row "Numéro de téléphone" (:phone data)
         #(do (swap! form assoc :phone %) (reset! saved? false))
         {:type "tel" :required? true}]]
       [:div {:style {:margin-top "1rem" :display "flex" :align-items "center" :gap "1rem"}}
        [:button {:class (str "btn btn--green" (when-not valid? " btn--disabled"))
                  :style (when-not valid? {:opacity "0.5" :cursor "not-allowed"})
                  :on-click (fn []
                              (when valid?
                                (rf/dispatch [:auth/update-natural-person data])
                                (reset! saved? true)))}
         "Enregistrer"]
        (when-not valid?
          [:span {:style {:color "#d32f2f" :font-size "0.85rem"}}
           "Veuillez remplir les champs obligatoires"])
        (when (and valid? @saved?)
          [:span {:style {:color "var(--color-green)" :font-size "0.9rem"}}
           "Profil enregistré \u2713"])]])))

;; ── Legal person ────────────────────────────────────────────────────────────

(def ^:private legal-required-fields
  [:company-name :siren :headquarters :representative-last-name
   :representative-first-name :representative-role :phone])

(defn- legal-person-valid? [data]
  (every? #(seq (str (get data %))) legal-required-fields))

(defn- legal-person-card [index initial-data]
  (let [form   (r/atom (or initial-data {}))
        saved? (r/atom false)]
    (fn [index _initial-data]
      (let [data   @form
            valid? (legal-person-valid? data)]
        [:div {:style {:background "#fff" :border "1px solid var(--color-border)"
                       :border-radius "var(--radius)" :padding "1.5rem"
                       :margin-bottom "1rem"}}
         [:div {:style {:display "flex" :justify-content "space-between" :align-items "center"
                        :margin-bottom "1rem"}}
          [:h4 {:style {:margin 0 :font-size "1rem"}}
           (if (seq (:company-name data))
             (:company-name data)
             (str "Personne morale " (inc index)))]
          [:button {:on-click #(when (js/confirm "Supprimer cette personne morale ?")
                                 (rf/dispatch [:auth/remove-legal-person index]))
                    :style {:background "none" :border "none" :cursor "pointer"
                            :color "#d32f2f" :font-size "1.2rem"}}
           "\u00D7"]]
         [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
          [field-row "Raison sociale" (:company-name data)
           #(do (swap! form assoc :company-name %) (reset! saved? false)) {:required? true}]
          [field-row "N° SIREN" (:siren data)
           #(do (swap! form assoc :siren %) (reset! saved? false)) {:required? true}]
          [field-row "Siège social" (:headquarters data)
           #(do (swap! form assoc :headquarters %) (reset! saved? false)) {:required? true}]
          [field-row "Numéro de téléphone" (:phone data)
           #(do (swap! form assoc :phone %) (reset! saved? false))
           {:type "tel" :required? true}]]
         [:h4 {:style {:margin-top "1rem" :margin-bottom "0.5rem" :font-size "0.95rem"}}
          "Représentant légal"]
         [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
          [field-row "Nom" (:representative-last-name data)
           #(do (swap! form assoc :representative-last-name %) (reset! saved? false)) {:required? true}]
          [field-row "Prénom" (:representative-first-name data)
           #(do (swap! form assoc :representative-first-name %) (reset! saved? false)) {:required? true}]
          [field-row "Fonction" (:representative-role data)
           #(do (swap! form assoc :representative-role %) (reset! saved? false))
           {:placeholder "Président, gérant, directeur général..." :required? true}]]
         [:div {:style {:margin-top "1rem" :display "flex" :align-items "center" :gap "1rem"}}
          [:button {:class (str "btn btn--small btn--green" (when-not valid? " btn--disabled"))
                    :style (when-not valid? {:opacity "0.5" :cursor "not-allowed"})
                    :on-click (fn []
                                (when valid?
                                  (rf/dispatch [:auth/update-legal-person index data])
                                  (reset! saved? true)))}
           "Enregistrer"]
          (when (and valid? @saved?)
            [:span {:style {:color "var(--color-green)" :font-size "0.85rem"}}
             "Enregistré \u2713"])]]))))

(defn- new-legal-person-form []
  (let [form    (r/atom {})
        adding? (r/atom false)]
    (fn []
      (if-not @adding?
        [:button.btn.btn--small.btn--outline
         {:on-click #(reset! adding? true)
          :style {:margin-top "0.5rem"}}
         "+ Ajouter une personne morale"]
        (let [data   @form
              valid? (legal-person-valid? data)]
          [:div {:style {:background "var(--color-green-pale)" :border-radius "var(--radius)"
                         :padding "1.5rem" :margin-top "0.5rem"}}
           [:h4 {:style {:margin-bottom "1rem"}} "Nouvelle personne morale"]
           [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
            [field-row "Raison sociale" (:company-name data)
             #(swap! form assoc :company-name %) {:required? true}]
            [field-row "N° SIREN" (:siren data)
             #(swap! form assoc :siren %) {:required? true}]
            [field-row "Siège social" (:headquarters data)
             #(swap! form assoc :headquarters %) {:required? true}]
            [field-row "Numéro de téléphone" (:phone data)
             #(swap! form assoc :phone %)
             {:type "tel" :required? true}]]
           [:h4 {:style {:margin-top "1rem" :margin-bottom "0.5rem" :font-size "0.95rem"}}
            "Représentant légal"]
           [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
            [field-row "Nom" (:representative-last-name data)
             #(swap! form assoc :representative-last-name %) {:required? true}]
            [field-row "Prénom" (:representative-first-name data)
             #(swap! form assoc :representative-first-name %) {:required? true}]
            [field-row "Fonction" (:representative-role data)
             #(swap! form assoc :representative-role %)
             {:placeholder "Président, gérant, directeur général..." :required? true}]]
           [:div {:style {:margin-top "1rem" :display "flex" :gap "0.75rem"}}
            [:button {:class (str "btn btn--small btn--green" (when-not valid? " btn--disabled"))
                      :style (when-not valid? {:opacity "0.5" :cursor "not-allowed"})
                      :on-click (fn []
                                  (when valid?
                                    (rf/dispatch [:auth/add-legal-person data])
                                    (reset! form {})
                                    (reset! adding? false)))}
             "Ajouter"]
            [:button.btn.btn--small.btn--outline
             {:on-click (fn [] (reset! form {}) (reset! adding? false))}
             "Annuler"]]])))))

;; ── Main page ───────────────────────────────────────────────────────────────

(defn profile-page []
  (let [user          @(rf/subscribe [:auth/user])
        natural-form  (r/atom (or (:natural-person user) {}))
        natural-saved (r/atom false)]
    (fn []
      (let [user          @(rf/subscribe [:auth/user])
            legal-persons (or (:legal-persons user) [])]
        [:div.contracts
         [:div.consumptions__header
          [:h2 "Mon profil"]]
         [:div {:style {:max-width "700px" :margin-top "1.5rem"}}
          ;; Natural person
          [natural-person-section natural-form natural-saved]

          ;; Legal persons
          [:div {:style {:margin-top "2.5rem"}}
           [:h3 {:style {:margin-bottom "1rem" :font-size "1.1rem"}} "Personnes morales"]
           (if (seq legal-persons)
             (doall
               (for [[idx lp] (map-indexed vector legal-persons)]
                 ^{:key (str "legal-" idx)}
                 [legal-person-card idx lp]))
             [:p {:style {:color "var(--color-muted)" :font-size "0.9rem" :margin-bottom "0.5rem"}}
              "Aucune personne morale enregistrée."])
           [new-legal-person-form]]]]))))
