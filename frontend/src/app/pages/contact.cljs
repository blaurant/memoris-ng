(ns app.pages.contact
  (:require [ajax.core :as ajax]
            [app.config :as config]
            [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(rf/reg-event-fx :contact/send
  (fn [_ [_ form-data on-success on-failure]]
    {:http-xhrio {:method          :post
                  :uri             (str config/API_BASE "/api/v1/contact")
                  :params          form-data
                  :format          (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:contact/send-ok on-success]
                  :on-failure      [:contact/send-err on-failure]}}))

(rf/reg-event-fx :contact/send-ok
  (fn [_ [_ on-success _resp]]
    (when on-success (on-success))
    {}))

(rf/reg-event-fx :contact/send-err
  (fn [_ [_ on-failure resp]]
    (when on-failure (on-failure (get-in resp [:response :error] "Erreur lors de l'envoi.")))
    {}))

(defn contact-page []
  (let [form    (r/atom {:name "" :email "" :subject "" :message ""})
        status  (r/atom nil)
        sending (r/atom false)]
    (fn []
      [:div.landing
       ;; Hero
       [:section.section.section--alt
        [:div.container {:style {:text-align "center"}}
         [:h1.section__title "Contactez-nous"]
         [:p.section__subtitle {:style {:max-width "600px" :margin "0 auto"}}
          "Une question, une suggestion ou besoin d'informations\u00a0? "
          "L'\u00e9quipe Elink-co vous r\u00e9pond dans les meilleurs d\u00e9lais."]]]

       [:section.section
        [:div.container {:style {:max-width "580px"}}

         (if (= @status :sent)
           ;; Success state
           [:div {:style {:text-align "center" :padding "3rem 2rem"
                          :background "var(--color-white)" :border-radius "12px"
                          :box-shadow "var(--shadow)"}}
            [:svg {:width "48" :height "48" :viewBox "0 0 24 24" :fill "none"
                   :stroke "var(--color-green)" :stroke-width "1.5"
                   :stroke-linecap "round" :stroke-linejoin "round"
                   :style {:margin-bottom "1rem"}}
             [:path {:d "M22 11.08V12a10 10 0 1 1-5.93-9.14"}]
             [:polyline {:points "22 4 12 14.01 9 11.01"}]]
            [:h2 {:style {:color "var(--color-green)" :margin-bottom "0.5rem"
                          :font-size "1.3rem"}}
             "Message envoy\u00e9\u00a0!"]
            [:p {:style {:color "var(--color-text)" :margin-bottom "1.5rem"
                         :line-height "1.6"}}
             "Merci pour votre message. Nous vous r\u00e9pondrons rapidement."]
            [:button.btn.btn--green
             {:on-click #(do (reset! form {:name "" :email "" :subject "" :message ""})
                             (reset! status nil))}
             "Envoyer un autre message"]]

           ;; Form
           [:div {:style {:background "var(--color-white)" :border-radius "12px"
                          :box-shadow "var(--shadow)" :padding "2.5rem"}}

            (when (= @status :error)
              [:div {:style {:background "#ffebee" :border-radius "var(--radius)"
                             :padding "0.75rem 1rem" :margin-bottom "1.5rem"
                             :color "var(--color-error)" :font-size "0.9rem"}}
               "Une erreur est survenue. Veuillez r\u00e9essayer."])

            [:div.onboarding__form
             [:div
              [:label {:style {:font-weight "600" :margin-bottom "4px" :display "block"
                               :color "var(--color-text)" :font-size "0.9rem"}}
               "Votre nom"]
              [:input.onboarding__input
               {:type "text" :placeholder "Pr\u00e9nom Nom"
                :value (:name @form)
                :style {:width "100%"}
                :on-change #(swap! form assoc :name (.. % -target -value))}]]

             [:div
              [:label {:style {:font-weight "600" :margin-bottom "4px" :display "block"
                               :color "var(--color-text)" :font-size "0.9rem"}}
               "Votre email"]
              [:input.onboarding__input
               {:type "email" :placeholder "vous@exemple.fr"
                :value (:email @form)
                :style {:width "100%"}
                :on-change #(swap! form assoc :email (.. % -target -value))}]]

             [:div
              [:label {:style {:font-weight "600" :margin-bottom "4px" :display "block"
                               :color "var(--color-text)" :font-size "0.9rem"}}
               "Sujet"]
              [:input.onboarding__input
               {:type "text" :placeholder "De quoi souhaitez-vous nous parler\u00a0?"
                :value (:subject @form)
                :style {:width "100%"}
                :on-change #(swap! form assoc :subject (.. % -target -value))}]]

             [:div
              [:label {:style {:font-weight "600" :margin-bottom "4px" :display "block"
                               :color "var(--color-text)" :font-size "0.9rem"}}
               "Message"]
              [:textarea.onboarding__input
               {:rows 6 :placeholder "Votre message..."
                :value (:message @form)
                :style {:width "100%" :resize "vertical" :min-height "140px"
                        :font-family "var(--font-sans)"}
                :on-change #(swap! form assoc :message (.. % -target -value))}]]

             [:div {:style {:text-align "center" :margin-top "0.5rem"}}
              [:button.btn.btn--green
               {:disabled (or @sending
                              (some str/blank? (vals @form)))
                :style {:padding "0.75rem 2.5rem" :font-size "1rem"}
                :on-click (fn []
                            (reset! sending true)
                            (reset! status nil)
                            (rf/dispatch [:contact/send @form
                                          (fn []
                                            (reset! sending false)
                                            (reset! status :sent))
                                          (fn [_err]
                                            (reset! sending false)
                                            (reset! status :error))]))}
               (if @sending "Envoi en cours..." "Envoyer")]]]])]]])))
