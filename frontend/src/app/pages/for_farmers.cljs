(ns app.pages.for-farmers
  (:require [reitit.frontend.easy :as rfee]))

(defn for-farmers-page []
  [:section.section
   [:div.container
    [:h1.section__title "Agriculteurs"]
    [:p.section__subtitle
     "Diversifiez vos revenus en valorisant vos installations "
     "photovoltaïques auprès de votre communauté locale."]
    [:div {:style {:max-width "700px" :margin "2rem auto" :text-align "center"}}
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"}}
      "Vos toitures de hangars, de granges ou d'étables sont idéales pour "
      "accueillir des panneaux solaires. Avec Elink-co, vendez votre électricité "
      "directement aux habitants et entreprises de votre commune."]
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"
                  :margin-top "1.5rem"}}
      "Un complément de revenu stable, un circuit court de l'énergie, "
      "et un ancrage territorial renforcé."]]
    [:div {:style {:text-align "center" :margin-top "2rem"}}
     [:a.btn.btn--green {:href (rfee/href :page/how-it-works)} "Comment ça marche"]
     [:a.btn.btn--accent {:href (rfee/href :page/signup)
                          :style {:margin-left "1rem"}} "Adhérer"]]]])
