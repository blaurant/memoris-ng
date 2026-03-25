(ns app.components.legal-person-modal
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(def ^:private required-fields
  [:company-name :siren :headquarters :representative-last-name
   :representative-first-name :representative-role :phone])

(defn- valid? [data]
  (every? #(seq (str (get data %))) required-fields))

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
