(ns app.pages.for-individual-consumer
  (:require [reitit.frontend.easy :as rfee]))

(defn for-individual-consumer-page []
  [:section.section
   [:div.container
    [:h1.section__title "Consommateur particulier"]
    [:p.section__subtitle
     "Réduisez votre facture d'électricité en consommant une énergie verte, "
     "produite localement par vos voisins."]
    [:div {:style {:max-width "700px" :margin "2rem auto" :text-align "center"}}
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"}}
      "En rejoignant un réseau Elink-co, vous achetez directement l'électricité "
      "produite par les installations solaires de votre commune. Pas besoin de changer "
      "de fournisseur : Enedis continue d'assurer la livraison, et EDF reste votre "
      "interlocuteur pour le complément."]
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"
                  :margin-top "1.5rem"}}
      "Elink-co s'occupe de toutes les démarches administratives pour vous."]]
    [:div {:style {:text-align "center" :margin-top "2rem"}}
     [:a.btn.btn--green {:href (rfee/href :page/how-it-works)} "Comment ça marche"]
     [:a.btn.btn--accent {:href (rfee/href :page/signup)
                          :style {:margin-left "1rem"}} "Adhérer"]]]])
