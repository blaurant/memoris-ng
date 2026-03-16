(ns app.pages.home
  (:require [app.components.eligibility-form :as eligibility-form]
            [app.components.google-map :as google-map]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- hero-section []
  [:section.hero
   [:div.hero__overlay]
   [:div.hero__content
    [:h1.hero__title "L'énergie locale," [:br] "partagée entre voisins"]
    [:p.hero__subtitle
     "Rejoignez un réseau d'énergie renouvelable de proximité et consommez
     une électricité produite à quelques kilomètres de chez vous."]
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
      "Communauté locale"]]
    [:a.btn.btn--hero {:href     "#eligibility"
                       :on-click (fn [e]
                                   (.preventDefault e)
                                   (when-let [el (.getElementById js/document "eligibility")]
                                     (.scrollIntoView el #js {:behavior "smooth"})))}
     "Vérifier mon éligibilité"]]])

(defn- autoconsommation-section []
  [:section.section.section--alt
   [:div.container
    [:h2.section__title "L'autoconsommation collective, comment ça marche\u00a0?"]
    [:p.section__subtitle
     "Un producteur local installe des panneaux solaires et partage l'énergie avec ses voisins via le réseau existant. "
     "Pas de travaux chez vous : Enedis calcule automatiquement votre part d'énergie verte chaque mois."]

    [:div.acc-grid

     ;; Card 1 — Explication
     [:div.acc-card
      [:div.acc-card__icon
       [:svg {:width "32" :height "32" :viewBox "0 0 24 24" :fill "none"
              :stroke "var(--color-green)" :stroke-width "2" :stroke-linecap "round"
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
      [:h3.acc-card__title "Énergie solaire de proximité"]
      [:p.acc-card__text
       "Des panneaux photovoltaïques installés dans votre quartier produisent de l'électricité verte. "
       "Cette énergie est partagée entre les participants du réseau local, "
       "sans aucune installation chez vous. Le compteur Linky fait le reste."]]

     ;; Card 2 — Avantage consommateur
     [:div.acc-card.acc-card--highlight
      [:div.acc-card__icon
       [:svg {:width "32" :height "32" :viewBox "0 0 24 24" :fill "none"
              :stroke "var(--color-green)" :stroke-width "2" :stroke-linecap "round"
              :stroke-linejoin "round"}
        [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "23"}]
        [:path {:d "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"}]]]
      [:h3.acc-card__title "Consommateur : vous payez moins"]
      [:p.acc-card__text
       "Le kWh en autoconsommation collective vous revient à environ "
       [:strong "0,17\u00a0€ TTC"]
       " contre "
       [:strong "0,20\u00a0€ TTC"]
       " au tarif réglementé Enedis (TRV 2026). "
       "Soit une économie d'environ "
       [:strong "15\u00a0%"]
       " sur chaque kWh consommé localement."]
      [:div.acc-card__example
       [:span.acc-card__label "Exemple"]
       [:p "Pour 2\u00a0500 kWh/an autoconsommés : environ "
        [:strong "75\u00a0€ d'économie"]
        " par an sur votre facture."]]]

     ;; Card 3 — Avantage producteur
     [:div.acc-card.acc-card--highlight
      [:div.acc-card__icon
       [:svg {:width "32" :height "32" :viewBox "0 0 24 24" :fill "none"
              :stroke "var(--color-green)" :stroke-width "2" :stroke-linecap "round"
              :stroke-linejoin "round"}
        [:polyline {:points "23 6 13.5 15.5 8.5 10.5 1 18"}]
        [:polyline {:points "17 6 23 6 23 12"}]]]
      [:h3.acc-card__title "Producteur : vous vendez mieux"]
      [:p.acc-card__text
       "En vendant à vos voisins, vous obtenez environ "
       [:strong "0,13\u00a0€/kWh"]
       ", soit "
       [:strong "plus du double"]
       " du tarif de rachat EDF OA (0,04 à 0,05\u00a0€/kWh pour le surplus). "
       "Votre installation est rentabilisée plus vite."]
      [:div.acc-card__example
       [:span.acc-card__label "Exemple"]
       [:p "Pour 10\u00a0000 kWh/an produits et vendus localement : "
        [:strong "1\u00a0300\u00a0€ de revenus"]
        " au lieu de ~500\u00a0€ via EDF OA."]]]]

    [:p.acc-source
     "Tarifs indicatifs basés sur le TRV août 2025, tarifs EDF OA T1 2026, "
     "et accise à 0\u00a0€/MWh pour l'autoconsommation collective (loi de finances 2025)."]]])

(defn- map-section []
  [:section.section
   [:div.container
    [:h2.section__title "Les réseaux Elinkco"]
    [:p.section__subtitle
     "Découvrez les zones couvertes par nos réseaux d'énergie partagée en France."]
    [google-map/network-map]]])

(defn- eligibility-section []
  [:section#eligibility.section.section--alt
   [:div.container
    [:h2.section__title "Vérifiez votre éligibilité"]
    [:p.section__subtitle
     "Entrez votre adresse pour savoir si vous êtes dans la zone d'un réseau Elinkco."]
    [eligibility-form/eligibility-form]]])

(defn home-page []
  (r/with-let [_ (rf/dispatch [:networks/fetch])]
    [:<>
     [hero-section]
     [autoconsommation-section]
     [map-section]
     [eligibility-section]]))
