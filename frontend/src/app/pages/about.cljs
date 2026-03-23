(ns app.pages.about
  (:require [reitit.frontend.easy :as rfee]))

(defn- icon-leaf []
  [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M17 8C8 10 5.9 16.17 3.82 21.34l1.89.66"}]
   [:path {:d "M20.59 3.41A21 21 0 0 0 3 21c5-4 8-6 11-7s6-2 8-4a3 3 0 0 0-1.41-5.59z"}]])

(defn- icon-people []
  [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
   [:circle {:cx "9" :cy "7" :r "4"}]
   [:path {:d "M23 21v-2a4 4 0 0 0-3-3.87"}]
   [:path {:d "M16 3.13a4 4 0 0 1 0 7.75"}]])

(defn- icon-scale []
  [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:line {:x1 "12" :y1 "3" :x2 "12" :y2 "21"}]
   [:polyline {:points "1 12 5 8 9 12"}]
   [:polyline {:points "15 12 19 8 23 12"}]
   [:line {:x1 "5" :y1 "8" :x2 "19" :y2 "8"}]])

(defn- icon-shield []
  [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"}]])

(defn about-page []
  [:div.landing
   ;; Hero with image
   [:section.section.section--alt
    [:div.container {:style {:text-align "center"}}
     [:h1.section__title "Qui sommes-nous ?"]
     [:p.section__subtitle {:style {:max-width "700px" :margin "0 auto"}}
      "Elink-co est une association loi 1901 qui organise des opérations d'autoconsommation collective "
      "pour permettre à chacun de consommer une énergie renouvelable, locale et à prix juste."]
     [:img {:src "/img/about-community.jpg"
            :alt "Voisins partageant un moment devant des panneaux solaires"
            :style {:width "100%" :max-width "700px" :margin "2rem auto 0"
                    :border-radius "var(--radius)" :display "block"
                    :box-shadow "var(--shadow)"}}]]]

   ;; Mission
   [:section.section
    [:div.container
     [:h2.section__title "Notre mission"]
     [:div.about-text
      [:p
       "Elink-co est née d'une conviction simple : l'énergie produite localement doit bénéficier "
       "en priorité aux habitants du territoire. En tant que "
       [:strong "Personne Morale Organisatrice (PMO)"]
       ", nous sommes le lien entre les producteurs d'énergie renouvelable et les consommateurs "
       "de proximité, conformément aux articles L315-2 et suivants du Code de l'énergie."]
      [:p
       "Notre rôle est d'organiser le partage d'énergie verte au sein de réseaux locaux, "
       "en garantissant une répartition équitable, des tarifs transparents et un suivi rigoureux "
       "de chaque kilowattheure échangé."]]]]

   ;; Valeurs
   [:section.section.section--alt
    [:div.container
     [:h2.section__title "Nos valeurs"]
     [:div.about-values
      [:div.about-value-card
       [icon-leaf]
       [:h3 "Transition énergétique"]
       [:p "Nous accélérons le développement des énergies renouvelables en offrant aux producteurs "
        "locaux un débouché direct et rémunérateur pour leur production."]]
      [:div.about-value-card
       [icon-people]
       [:h3 "Solidarité locale"]
       [:p "L'énergie partagée crée du lien entre voisins. Nous construisons des communautés "
        "énergétiques où chacun, producteur comme consommateur, trouve sa place."]]
      [:div.about-value-card
       [icon-scale]
       [:h3 "Transparence"]
       [:p "Pas d'intermédiaire opaque : les prix sont négociés directement, les clés de répartition "
        "sont connues de tous, et chaque membre a accès au suivi de sa consommation."]]
      [:div.about-value-card
       [icon-shield]
       [:h3 "Protection"]
       [:p "Vos données personnelles sont traitées conformément au RGPD. "
        "Vous gardez votre fournisseur habituel et pouvez quitter l'association à tout moment."]]]]]

   ;; Ce que nous faisons
   [:section.section
    [:div.container
     [:h2.section__title "Ce que nous faisons concrètement"]
     [:div.about-text
      [:ul.about-list
       [:li "Nous identifions et structurons des " [:strong "réseaux d'énergie locale"] " sur tout le territoire"]
       [:li "Nous représentons les membres auprès du " [:strong "gestionnaire de réseau (Enedis)"] " et assurons toutes les démarches administratives"]
       [:li "Nous définissons les " [:strong "clés de répartition"] " de l'énergie entre les participants de chaque réseau"]
       [:li "Nous assurons le " [:strong "suivi mensuel"] " de la production et de la consommation de chaque membre"]
       [:li "Nous garantissons des " [:strong "tarifs stables et transparents"] ", découplés des fluctuations du marché de l'énergie"]
       [:li "Nous veillons à une " [:strong "répartition équitable"] " et versons les compensations financières aux producteurs"]]
      [:img {:src "/img/about-project.jpg"
             :alt "Habitants planifiant un projet solaire dans leur village"
             :style {:width "100%" :margin-top "1.5rem"
                     :border-radius "var(--radius)"
                     :box-shadow "var(--shadow)"}}]]]]

   ;; Cadre légal
   [:section.section.section--alt
    [:div.container
     [:h2.section__title "Un cadre légal solide"]
     [:div.about-text
      [:p
       "L'autoconsommation collective est encadrée par les "
       [:strong "articles L315-2 et suivants du Code de l'énergie"]
       ". Ce dispositif, introduit en 2017 et renforcé depuis, permet à des producteurs et des "
       "consommateurs situés dans un périmètre géographique restreint (2 à 20 km selon les "
       "zones) de partager de l'énergie renouvelable via le réseau public de distribution."]
      [:p
       "En tant qu'association loi 1901, Elink-co agit au service de ses membres et de la transition "
       "énergétique locale. Les frais de gestion facturés aux producteurs et consommateurs ainsi que "
       "les frais de cotisation annuelle participent au bon fonctionnement de l'association."]]]]

   ;; CTA
   [:section.section
    [:div.container {:style {:text-align "center"}}
     [:h2.section__title "Rejoignez le mouvement"]
     [:p.section__subtitle {:style {:max-width "600px" :margin "0 auto 1.5rem"}}
      "Que vous soyez consommateur ou producteur, rejoindre Elink-co c'est faire le choix "
      "d'une énergie locale, verte et solidaire."]
     [:a.btn.btn--green {:href (rfee/href :page/signup)} "Créer mon compte"]]]])
