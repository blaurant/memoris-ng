(ns app.pages.how-it-works
  (:require [re-frame.core :as rf]
            [reitit.frontend.easy :as rfee]))

;; ── Icons ───────────────────────────────────────────────────────────────────

(defn- icon-network []
  [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:circle {:cx "12" :cy "5" :r "3"}]
   [:circle {:cx "5" :cy "19" :r "3"}]
   [:circle {:cx "19" :cy "19" :r "3"}]
   [:line {:x1 "12" :y1 "8" :x2 "5" :y2 "16"}]
   [:line {:x1 "12" :y1 "8" :x2 "19" :y2 "16"}]])

(defn- icon-consumer []
  [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"}]
   [:polyline {:points "9 22 9 12 15 12 15 22"}]])

(defn- icon-producer []
  [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:circle {:cx "12" :cy "12" :r "5"}]
   [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "3"}]
   [:line {:x1 "12" :y1 "21" :x2 "12" :y2 "23"}]
   [:line {:x1 "4.22" :y1 "4.22" :x2 "5.64" :y2 "5.64"}]
   [:line {:x1 "18.36" :y1 "18.36" :x2 "19.78" :y2 "19.78"}]
   [:line {:x1 "1" :y1 "12" :x2 "3" :y2 "12"}]
   [:line {:x1 "21" :y1 "12" :x2 "23" :y2 "12"}]
   [:line {:x1 "4.22" :y1 "19.78" :x2 "5.64" :y2 "18.36"}]
   [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]])

(defn- icon-steps []
  [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:line {:x1 "8" :y1 "6" :x2 "21" :y2 "6"}]
   [:line {:x1 "8" :y1 "12" :x2 "21" :y2 "12"}]
   [:line {:x1 "8" :y1 "18" :x2 "21" :y2 "18"}]
   [:line {:x1 "3" :y1 "6" :x2 "3.01" :y2 "6"}]
   [:line {:x1 "3" :y1 "12" :x2 "3.01" :y2 "12"}]
   [:line {:x1 "3" :y1 "18" :x2 "3.01" :y2 "18"}]])

(defn- icon-handshake []
  [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"}]])

(defn- icon-contract []
  [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
         :stroke "var(--color-green)" :stroke-width "1.5"
         :stroke-linecap "round" :stroke-linejoin "round"}
   [:path {:d "M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"}]
   [:polyline {:points "14 2 14 8 20 8"}]
   [:line {:x1 "16" :y1 "13" :x2 "8" :y2 "13"}]
   [:line {:x1 "16" :y1 "17" :x2 "8" :y2 "17"}]
   [:polyline {:points "10 9 9 9 8 9"}]])

;; ── Step component ──────────────────────────────────────────────────────────

(defn- step-number [n]
  [:div.hiw-step__number (str n)])

;; ── Page ────────────────────────────────────────────────────────────────────

(defn how-it-works-page []
  [:div.landing

   ;; Hero
   [:section.section.section--alt
    [:div.container {:style {:text-align "center"}}
     [:h1.section__title "Comment ça marche ?"]
     [:p.section__subtitle {:style {:max-width "700px" :margin "0 auto"}}
      "De la découverte d'un réseau à la signature de votre contrat, "
      "voici tout ce qu'il faut savoir pour rejoindre l'autoconsommation collective avec Elink-co."]]]

   ;; 1 — Consommateur
   [:section.section {:id "consommateur"}
    [:div.container
     [:div.hiw-block
      [:div.hiw-block__header
       [icon-consumer]
       [:div
        [:h2.hiw-block__title "Devenir consommateur"]
        [:p.hiw-block__subtitle "Consommer de l'énergie verte produite à côté de chez vous"]]]
      [:div.hiw-block__content
       [:p
        "En tant que consommateur, vous participez à une opération " [:strong "d'autoconsommation "]
        [:strong "collective"] " qui vous permet de " [:strong "consommer une électricité locale"]
        " , produite à partir " [:strong "d'énergies renouvelables"] " par des producteurs situés à proximité."]
       [:p
        "L'électricité produite est répartie entre les consommateurs selon une clé de "
        "répartition qui peut être définie par les membres du collectif. Vous achetez ainsi une "
        "part de votre électricité directement auprès de plusieurs producteurs locaux."]
       [:p
        "Votre prix d'électricité correspond aux prix fixés par les producteurs à laquelle "
        "s'ajoutent des frais de gestion liés à l'organisation du service (frais d'adhésion "
        "30\u00a0€/an, frais de fonctionnement 0.015\u00a0€ HT/kWh acheté)."]
       [:p
        "Au final, votre électricité vous coûtera environ " [:strong "10\u00a0% moins cher"]
        " que le prix réglementé EDF. Le prix de vente est fixé à l'avance et reste stable "
        "dans le temps. Il ne varie pas en fonction du marché de l'électricité ou du contexte "
        "géopolitique."]
       [:p
        [:strong "L'électricité renouvelable"] " étant par nature variable (selon la météo, les saisons, "
        "etc.), elle ne couvre pas en permanence l'ensemble de vos besoins. Vous conservez "
        "le contrat avec votre fournisseur d'électricité classique pour compléter votre "
        "approvisionnement et garantir la continuité de service."]
       [:p
        "En participant à un collectif, vous consommez automatiquement et en priorité "
        "l'énergie verte des producteurs participants."]
       [:p
        "Enfin, vous pouvez quitter le dispositif à tout moment, dans le respect des conditions "
        "de résiliation prévues au contrat (c'est-à-dire 2 mois)."]]]]]

   ;; 2 — Producteur
   [:section.section.section--alt {:id "producteur"}
    [:div.container
     [:div.hiw-block
      [:div.hiw-block__header
       [icon-producer]
       [:div
        [:h2.hiw-block__title "Devenir producteur"]
        [:p.hiw-block__subtitle "Vendre votre énergie à vos voisins au meilleur prix"]]]
      [:div.hiw-block__content
       [:p
        "En tant que " [:strong "producteur"] ", vous participez à une opération " [:strong "d'autoconsommation "]
        [:strong "collective"] " qui vous permet de " [:strong "vendre directement l'électricité produite"] " par votre "
        "installation à des consommateurs situés à proximité."]
       [:p
        "Vous fixez votre propre prix de vente (en accord avec Elink-co) et êtes rémunéré en "
        "fonction des quantités d'électricité réellement consommées. La répartition de "
        "l'électricité entre les consommateurs est définie par une clé de répartition gérée par "
        "Elink-co."]
       [:p
        [:strong "Elink-co"] " prend en charge l'organisation globale\u00a0: coordination des participants, "
        "gestion administrative, collecte des données, facturation à votre nom et répartition "
        "des paiements."]
       [:p
        "Votre engagement repose sur une obligation de moyens\u00a0: vous devez exploiter et "
        "entretenir correctement votre installation, mais vous ne garantissez ni un volume de "
        "production ni une fourniture continue, la production d'énergie renouvelable étant par "
        "nature variable."]
       [:p
        "Si votre surplus n'est pas consommé par les membres du collectif, votre contrat de "
        "rachat EDF prend alors le relais."]
       [:p
        "Des frais de gestion sont appliqués pour couvrir les services de l'association (frais "
        "d'adhésion 30\u00a0€/an, frais de fonctionnement 0.015\u00a0€ HT/kWh vendu)."]
       [:p
        "Enfin, vous pouvez quitter l'opération à tout moment dans le respect des conditions "
        "de résiliation prévues au contrat."]]]]]

   ;; 3 — Les réseaux
   [:section.section {:id "reseaux"}
    [:div.container
     [:div.hiw-block
      [:div.hiw-block__header
       [icon-network]
       [:div
        [:h2.hiw-block__title "Les réseaux d'énergie locale"]
        [:p.hiw-block__subtitle "Le socle de l'autoconsommation collective"]]]
      [:div.hiw-block__content
       [:p
        [:strong "Elink-co"] " est une association et une entité centrale qui organise, structure et pilote "
        [:strong "l'opération d'autoconsommation collective"] " en réunissant des producteurs et des "
        "consommateurs au sein d'un périmètre géographique défini (2km/10km ou 20km)."]
       [:p
        "Elle assure la coordination entre tous les participants et veille au bon déroulement de "
        "l'opération conformément à la réglementation en vigueur. À ce titre, elle définit, met à "
        "jour et transmet la clé de répartition de l'électricité au gestionnaire de réseau, "
        "permettant d'allouer l'énergie produite entre les consommateurs."]
       [:p
        [:strong "Elink-co"] " prend en charge l'ensemble des aspects administratifs et opérationnels\u00a0: "
        "collecte et traitement des données de production et de consommation, établissement "
        "des factures, et répartition des revenus entre les producteurs et les consommateurs, "
        "dans le cadre des mandats qui lui sont confiés."]
       [:p
        "Elle agit également comme interface entre les participants et le gestionnaire de "
        "réseau public, garantissant la bonne intégration de l'opération dans le système "
        "électrique."]
       [:p
        [:strong "Elink-co"] " peut adapter les modalités de fonctionnement, y compris les prix et les "
        "conditions contractuelles, avec un préavis, afin de tenir compte des évolutions "
        "économiques, techniques ou réglementaires."]
       [:p
        "En contrepartie de ses services, elle perçoit des frais de gestion auprès des "
        "producteurs et des consommateurs."]]]]]

   ;; 4 — L'inscription (onboarding)
   [:section.section.section--alt
    [:div.container
     [:div.hiw-block
      [:div.hiw-block__header
       [icon-steps]
       [:div
        [:h2.hiw-block__title "L'inscription en 4 étapes"]
        [:p.hiw-block__subtitle "Un parcours simple et guidé"]]]
      [:div.hiw-block__content
       [:div.hiw-steps
        [:div.hiw-step
         [step-number 1]
         [:div
          [:h3 "Adresse et réseau"]
          [:p "Renseignez votre adresse et sélectionnez le réseau d'énergie locale le plus proche. "
           "Si aucun réseau n'existe dans votre zone, vous pouvez en proposer la création."]]]
        [:div.hiw-step
         [step-number 2]
         [:div
          [:h3 "Informations techniques"]
          [:p "Pour un consommateur : votre référence Linky et votre adresse de facturation. "
           "Pour un producteur : votre numéro PDL/PRM, la puissance installée, le type d'énergie "
           "et votre compteur Linky."]]]
        [:div.hiw-step
         [step-number 3]
         [:div
          [:h3 "Informations bancaires"]
          [:p "Votre IBAN pour le prélèvement (consommateur) ou le versement des compensations (producteur). "
           "Le mandat SEPA est signé électroniquement à l'étape suivante."]]]
        [:div.hiw-step
         [step-number 4]
         [:div
          [:h3 "Contrats et adhésion"]
          [:p "Signature de l'adhésion à l'association Elink-co, du contrat d'énergie et du mandat SEPA. "
           "Tout se fait en ligne, en quelques clics."]]]]]]]]

   ;; 5 — L'adhésion
   [:section.section
    [:div.container
     [:div.hiw-block
      [:div.hiw-block__header
       [icon-handshake]
       [:div
        [:h2.hiw-block__title "L'adhésion à Elink-co"]
        [:p.hiw-block__subtitle "Rejoindre l'association qui organise votre réseau"]]]
      [:div.hiw-block__content
       [:p
        "Elink-co est une " [:strong "association loi 1901"] " qui joue le rôle de Personne Morale "
        "Organisatrice (PMO), conformément aux articles L315-2 du Code de l'énergie. "
        "L'adhésion est " [:strong "obligatoire"] " pour participer à une opération d'autoconsommation "
        "collective : c'est elle qui vous lie juridiquement au réseau."]
       [:p
        "En adhérant, vous autorisez Elink-co à :"]
       [:ul.about-list
        [:li "Vous représenter auprès du gestionnaire de réseau (Enedis)"]
        [:li "Collecter et transmettre vos données de consommation pour la répartition"]
        [:li "Définir et appliquer les clés de répartition de l'énergie"]
        [:li "Assurer le suivi mensuel et la facturation"]]
       [:p
        "L'adhésion est unique : elle vaut pour toutes vos consommations et productions "
        "au sein d'Elink-co. Les particuliers peuvent " [:strong "quitter l'association à tout moment"]
        ", sans frais."]]]]]

   ;; 6 — Les contrats
   [:section.section.section--alt
    [:div.container
     [:div.hiw-block
      [:div.hiw-block__header
       [icon-contract]
       [:div
        [:h2.hiw-block__title "Les contrats"]
        [:p.hiw-block__subtitle "Des engagements clairs et transparents"]]]
      [:div.hiw-block__content
       [:p "En rejoignant un réseau, vous signez électroniquement plusieurs documents :"]
       [:div.hiw-contracts-grid
        [:div.hiw-contract-card
         [:h3 "Adhésion Elink-co"]
         [:p "Contrat d'adhésion à l'association. Signé une seule fois, "
          "il couvre toutes vos participations (consommation et production)."]]
        [:div.hiw-contract-card
         [:h3 "Contrat d'énergie"]
         [:p "Contrat de vente d'électricité entre vous et le producteur local. "
          "Il fixe le prix au kWh, les conditions et la durée."]]
        [:div.hiw-contract-card
         [:h3 "Mandat SEPA"]
         [:p "Autorisation de prélèvement pour le paiement de votre consommation d'énergie locale. "
          "Révocable à tout moment."]]]
       [:p
        "Tous vos contrats signés sont consultables à tout moment depuis votre espace "
        [:strong "\"Mes contrats\""] " dans le portail client, avec leurs dates de signature."]]]]]

   ;; CTA + liens
   [:section.section
    [:div.container {:style {:text-align "center"}}
     [:h2.section__title "Prêt à passer à l'énergie locale ?"]
     [:p.section__subtitle {:style {:max-width "600px" :margin "0 auto 1.5rem"}}
      "Vérifiez si un réseau existe près de chez vous et rejoignez l'autoconsommation collective."]
     [:div {:style {:display "flex" :justify-content "center" :gap "1rem" :flex-wrap "wrap"}}
      [:a.btn.btn--green
       {:href     "#eligibility"
        :on-click (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [:router/navigate :page/home])
                    (js/setTimeout
                      (fn []
                        (when-let [el (.getElementById js/document "eligibility")]
                          (.scrollIntoView el #js {:behavior "smooth"})))
                      200))}
       "Vérifier mon éligibilité"]
      [:a.btn.btn--outline
       {:href (rfee/href :page/faq)}
       "Consulter la FAQ"]]]]])
