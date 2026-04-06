(ns app.pages.profile
  (:require [app.components.legal-person-modal :as lpm]
            [app.components.password-input :refer [password-input]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

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

(defn- phone-valid? [v]
  (and (string? v)
       (re-matches #"[0-9+\-.()\s]+" v)
       (>= (count (re-seq #"\d" v)) 10)))

(defn- filter-phone [v]
  (apply str (re-seq #"[0-9+\-.()\s]" v)))

(defn- phone-field-row [label value on-change & [{:keys [required?]}]]
  (let [touched? (r/atom false)]
    (fn [label value on-change & [{:keys [required?]}]]
      (let [v         (or value "")
            valid?    (or (empty? v) (phone-valid? v))
            show-err? (and @touched? (seq v) (not valid?))]
        [:div {:style {:display "flex" :flex-direction "column" :gap "0.25rem"}}
         [:label {:style {:font-size "0.85rem" :font-weight "600" :color "var(--color-text)"}}
          label (when required? [:span {:style {:color "#d32f2f" :margin-left "2px"}} "*"])]
         [:input {:type        "tel"
                  :inputMode   "tel"
                  :value       v
                  :placeholder "Ex: +33 6 12 34 56 78"
                  :on-blur     #(reset! touched? true)
                  :on-change   #(on-change (filter-phone (-> % .-target .-value)))
                  :style       {:padding "0.5rem 0.75rem"
                                :border (str "1px solid " (if show-err? "#d32f2f" "var(--color-border)"))
                                :border-radius "var(--radius)" :font-size "0.95rem"}}]
         (when show-err?
           [:span {:style {:font-size "0.8rem" :color "#d32f2f"}}
            "Minimum 10 chiffres, caractères autorisés : chiffres, +, -, ., (, ), espaces"])]))))

;; ── Natural person ──────────────────────────────────────────────────────────

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

(defn- natural-person-section [form saved?]
  (fn [form saved?]
    (let [data   @form
          valid? (natural-person-valid? data)]
      [:<>
       [:h3 {:style {:margin-bottom "1rem" :font-size "1.1rem"}} "Identité"]
       [:div {:style {:display "grid" :grid-template-columns "1fr 1fr" :gap "1rem"}}
        [field-row "Prénom" (:first-name data)
         #(do (swap! form assoc :first-name %) (reset! saved? false)) {:required? true}]
        [field-row "Nom" (:last-name data)
         #(do (swap! form assoc :last-name %) (reset! saved? false)) {:required? true}]
        [field-row "Date de naissance" (:birth-date data)
         #(do (swap! form assoc :birth-date %) (reset! saved? false))
         {:type "date" :required? true
          :min "1920-01-01" :max (max-birth-date)}]
        [field-row "Adresse" (:address data)
         #(do (swap! form assoc :address %) (reset! saved? false)) {:required? true}]
        [field-row "Code postal" (:postal-code data)
         #(do (swap! form assoc :postal-code %) (reset! saved? false)) {:required? true}]
        [field-row "Ville" (:city data)
         #(do (swap! form assoc :city %) (reset! saved? false)) {:required? true}]
        [phone-field-row "Numéro de téléphone" (:phone data)
         #(do (swap! form assoc :phone %) (reset! saved? false))
         {:required? true}]]
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

(defn- legal-person-valid? [data]
  (and (every? #(seq (str (get data %))) legal-required-fields)
       (siren-valid? (:siren data))))

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
          [phone-field-row "Numéro de téléphone" (:phone data)
           #(do (swap! form assoc :phone %) (reset! saved? false))
           {:required? true}]]
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

(defn- add-legal-person-button []
  (let [show? (r/atom false)]
    (fn []
      [:<>
       [:button.btn.btn--small.btn--outline
        {:on-click #(reset! show? true)
         :style {:margin-top "0.5rem"}}
        "+ Ajouter une personne morale"]
       (when @show?
         [lpm/new-legal-person-modal #(reset! show? false)])])))

;; ── Change password ─────────────────────────────────────────────────────────

(defn- password-valid? [pw]
  (and (>= (count pw) 8)
       (re-find #"[^a-zA-Z0-9]" pw)))

(defn- change-password-section []
  (let [current  (r/atom "")
        new-pw   (r/atom "")
        confirm  (r/atom "")
        status   (r/atom nil)
        error    (r/atom nil)]
    (fn []
      (let [pw       @new-pw
            pw-ok?   (password-valid? pw)
            match?   (= pw @confirm)
            valid?   (and (seq @current) pw-ok? (seq @confirm) match?)]
        [:div {:style {:margin-top "2.5rem"}}
         [:h3 {:style {:margin-bottom "1rem" :font-size "1.1rem"}} "Changer le mot de passe"]
         [:div {:style {:display "flex" :flex-direction "column" :gap "0.75rem" :max-width "350px"}}
          [password-input
           {:placeholder "Mot de passe actuel"
            :value       @current
            :on-change   #(do (reset! current %) (reset! status nil) (reset! error nil))}]
          [password-input
           {:placeholder "Nouveau mot de passe"
            :value       pw
            :on-change   #(do (reset! new-pw %) (reset! status nil) (reset! error nil))}]
          (when (and (seq pw) (not pw-ok?))
            [:div {:style {:font-size "0.8rem" :color "#d32f2f" :margin-top "-0.25rem"}}
             "8 caract\u00e8res minimum dont 1 caract\u00e8re sp\u00e9cial"])
          [password-input
           {:placeholder "Confirmer le nouveau mot de passe"
            :value       @confirm
            :on-change   #(do (reset! confirm %) (reset! status nil) (reset! error nil))}]
          (when (and (seq pw) (seq @confirm) (not match?))
            [:div {:style {:font-size "0.8rem" :color "#d32f2f" :margin-top "-0.25rem"}}
             "Les mots de passe ne correspondent pas"])
          [:div {:style {:display "flex" :align-items "center" :gap "1rem" :margin-top "0.25rem"}}
           [:button.btn.btn--green.btn--small
            {:disabled (not valid?)
             :on-click (fn []
                         (reset! status :saving)
                         (reset! error nil)
                         (rf/dispatch
                           [:auth/change-password @current @new-pw
                            (fn []
                              (reset! status :saved)
                              (reset! current "")
                              (reset! new-pw "")
                              (reset! confirm ""))
                            (fn [err-msg]
                              (reset! status nil)
                              (reset! error err-msg))]))}
            (if (= @status :saving) "Enregistrement..." "Modifier")]
           (when (= @status :saved)
             [:span {:style {:color "var(--color-green)" :font-size "0.9rem"}}
              "Mot de passe modifi\u00e9 \u2713"])]
          (when @error
            [:div {:style {:font-size "0.85rem" :color "#d32f2f"}}
             @error])]]))))

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

          ;; Change password (email users only)
          (when (= "email" (:provider user))
            [change-password-section])

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
           [add-legal-person-button]]]]))))
