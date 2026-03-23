(ns app.pages.home
  (:require [app.components.eligibility-form :as eligibility-form]
            [app.components.google-map :as google-map]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfee]))

(defn- hero-section []
  [:section.hero
   [:div.hero__overlay]
   [:div.hero__content
    [:h1.hero__title "L'énergie locale," [:br] "partagée entre voisins"]
    [:p.hero__subtitle
     "Rejoignez un réseau d'énergie renouvelable de proximité et consommez
     une électricité produite à quelques kilomètres de chez vous."]
    [:a.btn.btn--hero {:href     "#eligibility"
                       :on-click (fn [e]
                                   (.preventDefault e)
                                   (when-let [el (.getElementById js/document "eligibility")]
                                     (.scrollIntoView el #js {:behavior "smooth"})))}
     "Vérifier mon éligibilité"]
    [:div.hero__values
     [:span.hero__value
      [:svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none"
             :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
             :stroke-linejoin "round" :style {:vertical-align "middle" :margin-right "6px"}}
       [:path {:d "M13 2L3 14h9l-1 8 10-12h-9l1-8z"}]]
      "Énergie verte"]
     [:span.hero__value
      [:svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none"
             :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
             :stroke-linejoin "round" :style {:vertical-align "middle" :margin-right "6px"}}
       [:circle {:cx "12" :cy "12" :r "10"}]
       [:polyline {:points "16 12 12 8 8 12"}]
       [:line {:x1 "12" :y1 "16" :x2 "12" :y2 "8"}]]
      "Circuit court"]
     [:span.hero__value
      [:svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none"
             :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
             :stroke-linejoin "round" :style {:vertical-align "middle" :margin-right "6px"}}
       [:path {:d "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"}]]
      "Tarifs transparents"]
     [:span.hero__value
      [:svg {:width "16" :height "16" :viewBox "0 0 24 24" :fill "none"
             :stroke "currentColor" :stroke-width "2" :stroke-linecap "round"
             :stroke-linejoin "round" :style {:vertical-align "middle" :margin-right "6px"}}
       [:path {:d "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
       [:circle {:cx "9" :cy "7" :r "4"}]
       [:path {:d "M23 21v-2a4 4 0 0 0-3-3.87"}]
       [:path {:d "M16 3.13a4 4 0 0 1 0 7.75"}]]
      "Communauté locale"]]]])

(defn- autoconsommation-section []
  [:section.section.section--alt
   [:div.container
    [:h2.section__title "L'autoconsommation collective, comment ça marche\u00a0?"]
    [:p.section__subtitle
     "Un producteur local génère de l’énergie électrique grâce à ses panneaux solaires. "
     "Il peut choisir de la partager avec ses voisins, en utilisant le réseau électrique existant."]

    [:div.acc-grid

     ;; Card 1 — Énergie verte en circuit court
     [:div.acc-card
      [:div.acc-card__icon
       [:svg {:width "32" :height "32" :viewBox "0 0 24 24" :fill "none"
              :stroke "var(--color-accent)" :stroke-width "2" :stroke-linecap "round"
              :stroke-linejoin "round"}
        [:circle {:cx "12" :cy "12" :r "5"}]
        [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "3"}]
        [:line {:x1 "12" :y1 "21" :x2 "12" :y2 "23"}]
        [:line {:x1 "4.22" :y1 "4.22" :x2 "5.64" :y2 "5.64"}]
        [:line {:x1 "18.36" :y1 "18.36" :x2 "19.78" :y2 "19.78"}]
        [:line {:x1 "1" :y1 "12" :x2 "3" :y2 "12"}]
        [:line {:x1 "21" :y1 "12" :x2 "23" :y2 "12"}]
        [:line {:x1 "4.22" :y1 "19.78" :x2 "5.64" :y2 "18.36"}]
        [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]]]
      [:h3.acc-card__title "Une énergie verte en circuit court"]
      [:p.acc-card__text
       "Des panneaux photovoltaïques installés dans votre quartier produisent de l'électricité "
       "verte. Cette énergie est partagée entre les participants du réseau local, sans aucune "
       "installation chez vous, sans changement de fournisseur d'énergie."]
      [:p.acc-card__text {:style {:margin-top "0.75rem"}}
       "Elink-co fait le lien entre Enedis, EDF, le producteur et le consommateur\u00a0: nous nous chargeons "
       "de la partie administrative, et technique. Vous consommez à présent une part d'énergie verte "
       "produite localement."]
      [:div {:style {:text-align "center"}}
       [:a.btn.btn--green.btn--small {:href (rfee/href :page/how-it-works)}
        "En savoir plus"]]]

     ;; Card 2 — Avantage producteur
     [:div.acc-card.acc-card--highlight
      [:div.acc-card__icon
       [:svg {:width "32" :height "32" :viewBox "0 0 24 24" :fill "none"
              :stroke "var(--color-green)" :stroke-width "2" :stroke-linecap "round"
              :stroke-linejoin "round"}
        [:polyline {:points "23 6 13.5 15.5 8.5 10.5 1 18"}]
        [:polyline {:points "17 6 23 6 23 12"}]]]
      [:h3.acc-card__title "Producteur\u00a0: gagner en rentabilité"]
      [:p.acc-card__text
       "Vous avez des panneaux solaires sur votre toit, une éolienne dans votre jardin, \u2026 "
       "Vous pouvez vendre directement aux habitants de votre commune, votre surplus d'énergie à "
       "un tarif plus avantageux que le tarif réglementé (EDF OA)."]
      [:p.acc-card__text {:style {:margin-top "0.75rem"}}
       "Vous conservez votre contrat de revente d'énergie avec EDF OA. Elink-co se charge des "
       "démarches entre vous, Enedis, EDF et les consommateurs finaux."]
      [:div {:style {:text-align "center"}}
       [:a.btn.btn--green.btn--small {:href (rfee/href :page/how-it-works)}
        "En savoir plus"]]]

     ;; Card 3 — Avantage consommateur
     [:div.acc-card.acc-card--highlight
      [:div.acc-card__icon
       [:svg {:width "32" :height "32" :viewBox "0 0 24 24" :fill "none"
              :stroke "var(--color-green)" :stroke-width "2" :stroke-linecap "round"
              :stroke-linejoin "round"}
        [:path {:d "M17.2 7C16 5.2 14.1 4 12 4c-3.3 0-6 2.7-6 6s2.7 6 6 6c2.1 0 4-1.2 5.2-3"}]
        [:line {:x1 "4" :y1 "9" :x2 "15" :y2 "9"}]
        [:line {:x1 "4" :y1 "11.5" :x2 "15" :y2 "11.5"}]]]
      [:h3.acc-card__title "Consommateur : une énergie locale, responsable et plus abordable"]
      [:p.acc-card__text
       "Vous souhaitez consommer de l'électricité plus responsable tout en réduisant "
       "votre facture : achetez votre énergie verte directement auprès des producteurs "
       "de votre commune sans changer d'opérateur."]
      [:p.acc-card__text
       "Adhérez simplement au réseau Elink-co, nous nous occupons des démarches entre "
       "vous, Enedis, EDF et les producteurs finaux."]
      [:div {:style {:text-align "center"}}
       [:a.btn.btn--green.btn--small {:href (rfee/href :page/how-it-works)}
        "En savoir plus"]]]

     ]

    [:p.acc-source
     "Tarifs indicatifs basés sur le TRV août 2025, tarifs EDF OA T1 2026, "
     "et accise à 0\u00a0€/MWh pour l'autoconsommation collective (loi de finances 2025)."]
    [:div {:style {:text-align "center" :margin-top "2rem"}}
     [:a.btn.btn--accent {:href (rfee/href :page/signup)
                          :style {:padding "1rem 2.5rem" :font-size "1.1rem"
                                  :border-radius "2rem" :font-weight "700"}}
      "Adhérer"]]]])

(defn- map-section []
  [:section.section
   [:div.container
    [:h2.section__title "Les réseaux Elink-co"]
    [:p.section__subtitle
     "Découvrez les zones couvertes par nos réseaux d'énergie partagée en France."]
    [google-map/network-map]]])

(defn- eligibility-section []
  [:section#eligibility.section.section--alt
   [:div.container
    [:h2.section__title "Vérifiez votre éligibilité"]
    [:p.section__subtitle
     "Entrez votre adresse pour savoir si vous êtes dans la zone d'un réseau Elink-co."]
    [eligibility-form/eligibility-form]]])

(defn home-page []
  (r/with-let [_ (rf/dispatch [:networks/fetch])]
    [:div.landing
     [hero-section]
     [autoconsommation-section]
     [map-section]
     [eligibility-section]]))
