(ns app.pages.for-business
  (:require [reitit.frontend.easy :as rfee]))

(defn for-business-page []
  [:<>
   ;; Hero
   [:section {:style {:position "relative" :min-height "420px" :display "flex"
                      :align-items "flex-end"
                      :background "url('/img/entreprise.png') center 30% / cover no-repeat"
                      :border-radius "0 0 1.5rem 1.5rem"}}
    [:div {:style {:position "absolute" :inset "0"
                   :background "linear-gradient(to top, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.1) 60%, transparent 100%)"
                   :border-radius "0 0 1.5rem 1.5rem"}}]
    [:div.container {:style {:position "relative" :z-index 1 :padding-bottom "2.5rem"
                             :color "#fff" :max-width "600px"}}
     [:h1 {:style {:font-size "2.2rem" :font-weight "800" :line-height "1.2"}}
      "Entreprise"]]]

   ;; Subtitle
   [:div.container {:style {:text-align "center" :margin-top "1.5rem" :margin-bottom "0.5rem"}}
    [:p {:style {:font-size "1.15rem" :line-height "1.7" :color "var(--color-text)"
                 :max-width "700px" :margin "0 auto"}}
     "Comme les particuliers, votre entreprise peut consommer, produire, "
     "ou faire les deux. Engagez votre activité dans la transition énergétique locale."]]

   ;; Cards
   [:section.section
    [:div.container
     [:div {:style {:display "grid" :grid-template-columns "1fr 1fr 1fr" :gap "2rem"
                    :margin-top "1rem"}}

      ;; Card 1 — Consommer
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
               :stroke "var(--color-green)" :stroke-width "1.5"
               :stroke-linecap "round" :stroke-linejoin "round"}
         [:path {:d "M13 2L3 14h9l-1 8 10-12h-9l1-8z"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Consommez local"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Achetez une électricité verte produite à proximité de vos locaux. "
        "Réduisez votre facture tout en affichant un engagement "
        "environnemental concret auprès de vos clients et partenaires."]]

      ;; Card 2 — Produire
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-accent)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
               :stroke "var(--color-accent)" :stroke-width "1.5"
               :stroke-linecap "round" :stroke-linejoin "round"}
         [:circle {:cx "12" :cy "12" :r "5"}]
         [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "3"}]
         [:line {:x1 "12" :y1 "21" :x2 "12" :y2 "23"}]
         [:line {:x1 "4.22" :y1 "4.22" :x2 "5.64" :y2 "5.64"}]
         [:line {:x1 "18.36" :y1 "18.36" :x2 "19.78" :y2 "19.78"}]
         [:line {:x1 "1" :y1 "12" :x2 "3" :y2 "12"}]
         [:line {:x1 "21" :y1 "12" :x2 "23" :y2 "12"}]
         [:line {:x1 "4.22" :y1 "19.78" :x2 "5.64" :y2 "18.36"}]
         [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-accent)"}}
        "Produisez et revendez"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Vos toitures, hangars ou parkings peuvent accueillir des panneaux solaires. "
        "Vendez votre surplus d'énergie aux habitants et entreprises voisines "
        "à un tarif plus avantageux que le rachat EDF OA."]]

      ;; Card 3 — Les deux
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
               :stroke "var(--color-green)" :stroke-width "1.5"
               :stroke-linecap "round" :stroke-linejoin "round"}
         [:polyline {:points "23 6 13.5 15.5 8.5 10.5 1 18"}]
         [:polyline {:points "17 6 23 6 23 12"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Combinez les deux"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Produisez votre propre énergie et consommez celle de vos voisins. "
        "Maximisez votre autonomie énergétique tout en contribuant "
        "au réseau local de votre commune."]]]

     ;; Banner
     [:div {:style {:background "var(--color-green-pale)" :border-radius "12px"
                    :padding "2rem 2.5rem" :margin-top "2.5rem"
                    :display "flex" :align-items "center" :gap "1.5rem"}}
      [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
             :stroke "var(--color-green)" :stroke-width "1.5"
             :stroke-linecap "round" :stroke-linejoin "round"
             :style {:flex-shrink "0"}}
       [:path {:d "M22 11.08V12a10 10 0 1 1-5.93-9.14"}]
       [:polyline {:points "22 4 12 14.01 9 11.01"}]]
      [:p {:style {:font-size "1.05rem" :line-height "1.8" :color "var(--color-text)"}}
       "Elink-co gère l'intégralité des démarches\u00a0: convention Enedis, "
       "relation EDF, suivi de production et de consommation, facturation. "
       "Vous vous concentrez sur votre activité."]]

     ;; CTA
     [:div {:style {:text-align "center" :margin-top "2.5rem"}}
      [:a.btn.btn--green {:href "/comment-ca-marche#consommateur"} "Comment ça marche"]
      [:a.btn.btn--accent {:href (rfee/href :page/signup)
                           :style {:margin-left "1rem"}} "Adhérer"]]]]])
