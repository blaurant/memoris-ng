(ns app.pages.faq
  (:require [reagent.core :as r]))

(def ^:private faqs
  [{:q "Qu'est-ce que l'autoconsommation collective ?"
    :a "C'est le circuit court de l'électricité : un producteur d'énergie renouvelable local injecte sa production sur le réseau de distribution (géré par Enedis), et celle-ci est répartie virtuellement entre les consommateurs proches. Plus de 400 opérations sont actives en France avec plus de 5 000 participants."}
   {:q "Comment fonctionne concrètement le partage d'énergie ?"
    :a "La répartition de l'électricité est effectuée toutes les 30 minutes selon des clés de répartition définies par la communauté. Enedis calcule la part d'énergie locale consommée et en informe votre fournisseur habituel et le producteur."}
   {:q "Est-ce que je garde mon fournisseur d'électricité actuel ?"
    :a "Oui. Vous conservez votre contrat avec votre fournisseur habituel, qui continue à vous alimenter lorsque la production locale ne couvre pas vos besoins. Vous aurez donc deux factures : celle de votre fournisseur et celle du producteur local."}
   {:q "Y a-t-il un risque de coupure d'électricité ?"
    :a "Non. En cas d'arrêt de la production locale (manque de soleil, maintenance, panne), votre fournisseur habituel prend automatiquement le relais. Vous ne subissez aucune interruption de service."}
   {:q "Est-ce que l'autoconsommation collective réduit ma facture ?"
    :a "Oui. L'électricité locale est vendue à des tarifs avantageux, généralement inférieurs aux prix du marché. Votre facture fournisseur diminue puisque vous achetez moins d'électricité conventionnelle. Seul le prix de l'énergie change ; les frais de réseau et taxes restent identiques."}
   {:q "Qu'est-ce que la Personne Morale Organisatrice (PMO) ?"
    :a "C'est l'entité juridique (souvent une association loi 1901) qui organise l'opération d'autoconsommation collective. Elle représente les membres auprès du gestionnaire de réseau, définit les règles de répartition et gère les entrées et sorties de participants."}
   {:q "Quelles sont les étapes pour rejoindre un projet ?"
    :a "Il y a quatre étapes : autoriser la collecte de vos données de consommation, signer le contrat de vente d'électricité avec le producteur, adhérer à l'association (PMO), et signer la convention de participation qui autorise la transmission de vos données à Enedis."}
   {:q "Dois-je habiter à proximité du producteur ?"
    :a "Oui. La réglementation impose une distance maximale entre participants : 2 km en zone urbaine standard, 10 km en zone périurbaine et jusqu'à 20 km en zone rurale (sur dérogation)."}
   {:q "Quelles sont mes obligations en tant que consommateur ?"
    :a "Adhérer à l'association, signer le contrat de vente et la convention de participation, et payer régulièrement vos factures d'électricité locale. Vous devez également maintenir votre contrat avec votre fournisseur habituel."}
   {:q "Puis-je quitter le projet à tout moment ?"
    :a "Pour les particuliers, oui : vous pouvez quitter une opération d'autoconsommation collective à tout moment, sans frais. Pour les professionnels, des engagements de durée et des pénalités de sortie anticipée peuvent s'appliquer."}
   {:q "Que se passe-t-il si le producteur arrête de produire ?"
    :a "Votre fournisseur habituel continue de vous alimenter normalement. Vous pouvez également rejoindre un autre projet local si un réseau est disponible dans votre zone."}
   {:q "Quels types d'énergie renouvelable sont utilisés ?"
    :a "Environ 90 % des opérations utilisent l'énergie solaire (panneaux photovoltaïques). D'autres projets fonctionnent avec la petite hydraulique, l'éolien, la biomasse ou la cogénération."}
   {:q "Ma facture va-t-elle changer ?"
    :a "Votre facture fournisseur habituel diminue car la partie « consommation » (prix du kWh) est réduite proportionnellement à l'énergie locale consommée. En parallèle, vous recevez une facture du producteur local pour l'énergie verte consommée, à un tarif négocié."}
   {:q "Ai-je besoin d'un compteur spécial ?"
    :a "Vous devez disposer d'un compteur communicant (Linky) qui permet à Enedis de mesurer et répartir votre consommation d'énergie locale toutes les 30 minutes."}
   {:q "Comment sont fixés les prix de l'électricité locale ?"
    :a "Les prix sont négociés directement entre le producteur et les consommateurs, sans intermédiaire. Ils sont généralement garantis pour plusieurs années, découplés des fluctuations du marché, ce qui offre une visibilité et une stabilité tarifaire."}])

(defn- faq-item [{:keys [q a]} idx]
  (let [open? (r/atom false)]
    (fn [{:keys [q a]} _idx]
      [:div.faq-item {:class (when @open? "faq-item--open")}
       [:button.faq-item__question
        {:on-click #(swap! open? not)}
        [:span q]
        [:svg.faq-item__chevron
         {:width "20" :height "20" :viewBox "0 0 24 24"
          :fill "none" :stroke "currentColor" :stroke-width "2"
          :stroke-linecap "round" :stroke-linejoin "round"}
         [:polyline {:points "6 9 12 15 18 9"}]]]
       (when @open?
         [:div.faq-item__answer
          [:p a]])])))

(defn faq-page []
  [:div.landing
   [:section.section
    [:div.container
     [:h1.section__title {:style {:margin-bottom "0.5rem"}} "Foire aux Questions"]
     [:p.section__subtitle "Tout ce que vous devez savoir sur l'autoconsommation collective et Elink-co."]
     [:div.faq-list
      (doall
        (for [[idx item] (map-indexed vector faqs)]
          ^{:key idx}
          [faq-item item idx]))]]]])
