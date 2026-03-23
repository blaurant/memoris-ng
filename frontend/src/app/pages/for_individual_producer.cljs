(ns app.pages.for-individual-producer
  (:require [reitit.frontend.easy :as rfee]))

(defn for-individual-producer-page []
  [:section.section
   [:div.container
    [:h1.section__title "Producteur particulier"]
    [:p.section__subtitle
     "Valorisez votre production solaire en la vendant directement "
     "à vos voisins, à un tarif plus avantageux que le rachat EDF OA."]
    [:div {:style {:max-width "700px" :margin "2rem auto" :text-align "center"}}
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"}}
      "Vous disposez de panneaux photovoltaïques et souhaitez vendre votre surplus "
      "d'électricité ? Avec Elink-co, partagez votre production avec les habitants "
      "de votre commune en autoconsommation collective."]
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"
                  :margin-top "1.5rem"}}
      "Nous gérons la relation avec Enedis, le suivi de production, "
      "et le versement de vos revenus."]]
    [:div {:style {:text-align "center" :margin-top "2rem"}}
     [:a.btn.btn--green {:href (rfee/href :page/how-it-works)} "Comment ça marche"]
     [:a.btn.btn--accent {:href (rfee/href :page/signup)
                          :style {:margin-left "1rem"}} "Adhérer"]]]])
