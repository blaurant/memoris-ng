(ns app.pages.home
  (:require [app.components.eligibility-form :as eligibility-form]
            [app.components.google-map :as google-map]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- hero-section []
  [:section.hero
   [:div.container
    [:h1.hero__title "L'énergie locale, partagée entre voisins"]
    [:p.hero__subtitle
     "Rejoignez un réseau d'énergie renouvelable de proximité et consommez
     une électricité produite à quelques kilomètres de chez vous."]
    [:div.hero__values
     [:span.hero__value "Énergie verte"]
     [:span.hero__value "Circuit court"]
     [:span.hero__value "Tarifs transparents"]
     [:span.hero__value "Communauté locale"]]
    [:a.btn.btn--primary {:href "#eligibility"} "Vérifier mon éligibilité"]]])

(defn- map-section []
  [:section.section
   [:div.container
    [:h2.section__title "Les réseaux Wattprox"]
    [:p.section__subtitle
     "Découvrez les zones couvertes par nos réseaux d'énergie partagée en France."]
    [google-map/network-map]]])

(defn- eligibility-section []
  [:section#eligibility.section.section--alt
   [:div.container
    [:h2.section__title "Vérifiez votre éligibilité"]
    [:p.section__subtitle
     "Entrez votre adresse pour savoir si vous êtes dans la zone d'un réseau Wattprox."]
    [eligibility-form/eligibility-form]]])

(defn home-page []
  (r/with-let [_ (rf/dispatch [:networks/fetch])]
    [:<>
     [hero-section]
     [map-section]
     [eligibility-section]]))
