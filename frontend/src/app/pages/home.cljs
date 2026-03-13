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
     [map-section]
     [eligibility-section]]))
