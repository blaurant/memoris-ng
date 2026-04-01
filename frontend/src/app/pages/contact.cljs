(ns app.pages.contact
  (:require [ajax.core :as ajax]
            [app.config :as config]
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
      [:section.section
       [:div.container {:style {:max-width "600px"}}
        [:h1.section__title "Contact"]
        [:p.section__subtitle "Envoyez un message à l'équipe Elink-co."]

        (case @status
          :sent
          [:div {:style {:text-align "center" :margin-top "2rem"}}
           [:div {:style {:background "#e8f5e9" :border-radius "var(--radius)"
                          :padding "2rem" :color "var(--color-green)"}}
            [:p {:style {:font-size "1.1rem" :font-weight "600" :margin-bottom "0.5rem"}}
             "Message envoyé !"]
            [:p "Nous vous répondrons dans les meilleurs délais."]
            [:button.btn.btn--green.btn--small
             {:style {:margin-top "1rem"}
              :on-click #(do (reset! form {:name "" :email "" :subject "" :message ""})
                             (reset! status nil))}
             "Envoyer un autre message"]]]

          ;; Default: show form
          [:div {:style {:margin-top "2rem"}}
           (when (= @status :error)
             [:div {:style {:background "#ffebee" :border-radius "var(--radius)"
                            :padding "0.75rem 1rem" :margin-bottom "1rem"
                            :color "var(--color-error)" :font-size "0.9rem"}}
              "Une erreur est survenue. Veuillez réessayer."])

           [:div.form-group
            [:label.form-label "Votre nom"]
            [:input.form-input {:type "text" :value (:name @form)
                                :on-change #(swap! form assoc :name (.. % -target -value))}]]

           [:div.form-group
            [:label.form-label "Votre email"]
            [:input.form-input {:type "email" :value (:email @form)
                                :on-change #(swap! form assoc :email (.. % -target -value))}]]

           [:div.form-group
            [:label.form-label "Sujet"]
            [:input.form-input {:type "text" :value (:subject @form)
                                :on-change #(swap! form assoc :subject (.. % -target -value))}]]

           [:div.form-group
            [:label.form-label "Message"]
            [:textarea.form-input {:rows 6 :value (:message @form)
                                   :style {:resize "vertical"}
                                   :on-change #(swap! form assoc :message (.. % -target -value))}]]

           [:div {:style {:text-align "center" :margin-top "1.5rem"}}
            [:button.btn.btn--green
             {:disabled (or @sending
                           (some clojure.string/blank? (vals @form)))
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
             (if @sending "Envoi en cours..." "Envoyer")]]])]])))
