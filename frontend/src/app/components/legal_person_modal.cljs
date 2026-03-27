(ns app.components.legal-person-modal
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private required-fields
  [:company-name :siren :headquarters :representative-last-name
   :representative-first-name :representative-role :phone])

(defn- luhn-valid? [s]
  (let [digits (map #(- (.charCodeAt % 0) 48) s)]
    (zero? (rem (reduce + (map-indexed
                            (fn [i d]
                              (let [v (if (even? i) d (* 2 d))]
                                (if (> v 9) (- v 9) v)))
                            digits))
                10))))

(defn- siren-valid? [v]
  (and (string? v)
       (some? (re-matches #"^\d{9}$" v))
       (luhn-valid? v)))

(defn- valid? [data]
  (and (every? #(seq (str (get data %))) required-fields)
       (siren-valid? (:siren data))))

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

(defn new-legal-person-modal
  "Modal for creating a new legal person. Calls on-close when done or cancelled."
  [on-close]
  (let [form (r/atom {})]
    (fn [on-close]
      (let [data   @form
            valid? (valid? data)]
        [:div.modal-overlay {:on-click (fn [e]
                                          (when (= (.-target e) (.-currentTarget e))
                                            (on-close)))}
         [:div.modal {:on-click #(.stopPropagation %)}
          [:div.modal__header
           [:span "Nouvelle personne morale"]
           [:button.btn.btn--small
            {:on-click on-close
             :style {:background "transparent" :color "var(--color-muted)"
                     :border "none" :font-size "1.2rem" :padding "0"}}
            "\u00D7"]]
          [:div.modal__body
           [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
            [field-row "Raison sociale" (:company-name data)
             #(swap! form assoc :company-name %) {:required? true}]
            (let [s (or (:siren data) "")]
              [:div {:style {:display "flex" :flex-direction "column" :gap "0.25rem"}}
               [:label {:style {:font-size "0.85rem" :font-weight "600" :color "var(--color-text)"}}
                "N° SIREN" [:span {:style {:color "#d32f2f" :margin-left "2px"}} "*"]]
               [:input {:type "text" :inputMode "numeric" :maxLength 9
                        :value s :placeholder "9 chiffres"
                        :on-change #(swap! form assoc :siren (-> % .-target .-value))
                        :style {:padding "0.5rem 0.75rem"
                                :border (str "1px solid "
                                             (if (and (seq s) (not (siren-valid? s)))
                                               "#d32f2f" "var(--color-border)"))
                                :border-radius "var(--radius)" :font-size "0.95rem"}}]
               (when (and (seq s) (not (siren-valid? s)))
                 [:span {:style {:font-size "0.8rem" :color "#d32f2f"}}
                  (if (not (re-matches #"^\d{9}$" s))
                    "Le SIREN doit contenir exactement 9 chiffres"
                    "Numéro SIREN invalide (clé de contrôle incorrecte)")])])
            [field-row "Siège social" (:headquarters data)
             #(swap! form assoc :headquarters %) {:required? true}]
            [phone-field-row "Numéro de téléphone" (:phone data)
             #(swap! form assoc :phone %)
             {:required? true}]]
           [:h4 {:style {:margin-top "1rem" :margin-bottom "0.5rem" :font-size "0.95rem"}}
            "Représentant légal"]
           [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
            [field-row "Prénom" (:representative-first-name data)
             #(swap! form assoc :representative-first-name %) {:required? true}]
            [field-row "Nom" (:representative-last-name data)
             #(swap! form assoc :representative-last-name %) {:required? true}]
            [field-row "Fonction" (:representative-role data)
             #(swap! form assoc :representative-role %)
             {:placeholder "Président, gérant, directeur général..." :required? true}]]]
          [:div.modal__actions
           [:button.btn.btn--small.btn--outline
            {:on-click on-close}
            "Annuler"]
           [:button {:class (str "btn btn--small btn--green" (when-not valid? " btn--disabled"))
                     :style (when-not valid? {:opacity "0.5" :cursor "not-allowed"})
                     :on-click (fn []
                                 (when valid?
                                   (rf/dispatch [:auth/add-legal-person data])
                                   (on-close)))}
            "Ajouter"]]]]))))
