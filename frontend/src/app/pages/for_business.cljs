(ns app.pages.for-business
  (:require [reitit.frontend.easy :as rfee]))

(defn for-business-page []
  [:section.section
   [:div.container
    [:h1.section__title "Entreprise"]
    [:p.section__subtitle
     "Engagez votre entreprise dans la transition énergétique locale "
     "et réduisez vos coûts d'électricité."]
    [:div {:style {:max-width "700px" :margin "2rem auto" :text-align "center"}}
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"}}
      "Que vous soyez un commerce, un artisan ou une PME, rejoindre un réseau "
      "Elink-co vous permet de consommer une électricité verte produite à "
      "proximité de vos locaux."]
     [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"
                  :margin-top "1.5rem"}}
      "Affichez votre engagement environnemental tout en maîtrisant "
      "votre budget énergie."]]
    [:div {:style {:text-align "center" :margin-top "2rem"}}
     [:a.btn.btn--green {:href (rfee/href :page/how-it-works)} "Comment ça marche"]
     [:a.btn.btn--accent {:href (rfee/href :page/signup)
                          :style {:margin-left "1rem"}} "Adhérer"]]]])
