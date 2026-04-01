(ns app.pages.for-individual-producer
  (:require [reitit.frontend.easy :as rfee]))

(defn for-individual-producer-page []
  [:<>
   ;; Hero
   [:section {:style {:position "relative" :min-height "420px" :display "flex"
                      :align-items "flex-end"
                      :background "url('/img/producteur-particulier.png') center 30% / cover no-repeat"
                      :border-radius "0 0 1.5rem 1.5rem"}}
    [:div {:style {:position "absolute" :inset "0"
                   :background "linear-gradient(to top, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.1) 60%, transparent 100%)"
                   :border-radius "0 0 1.5rem 1.5rem"}}]
    [:div.container {:style {:position "relative" :z-index 1 :padding-bottom "2.5rem"
                             :color "#fff" :max-width "600px"}}
     [:h1 {:style {:font-size "2.2rem" :font-weight "800" :line-height "1.2"}}
      "Producteur particulier"]]]

   ;; Subtitle
   [:div.container {:style {:text-align "center" :margin-top "1.5rem" :margin-bottom "0.5rem"}}
    [:p {:style {:font-size "1.15rem" :line-height "1.7" :color "var(--color-text)"
                 :max-width "700px" :margin "0 auto"}}
     "Valorisez votre production solaire en la vendant directement "
     "à vos voisins, à un tarif plus avantageux que le rachat EDF OA."]]

   ;; Cards
   [:section.section
    [:div.container
     [:div {:style {:display "grid" :grid-template-columns "1fr 1fr 1fr" :gap "2rem"
                    :margin-top "1rem"}}

      ;; Card 1 — Surplus
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
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
         [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Vendez votre surplus"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Vous disposez de panneaux photovoltaïques sur votre toit\u00a0? "
        "Partagez votre production excédentaire avec les habitants "
        "de votre commune en autoconsommation collective."]]

      ;; Card 2 — Revenus
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-accent)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
               :stroke "var(--color-accent)" :stroke-width "1.5"
               :stroke-linecap "round" :stroke-linejoin "round"}
         [:path {:d "M17.2 7C16 5.2 14.1 4 12 4c-3.3 0-6 2.7-6 6s2.7 6 6 6c2.1 0 4-1.2 5.2-3"}]
         [:line {:x1 "4" :y1 "9" :x2 "15" :y2 "9"}]
         [:line {:x1 "4" :y1 "11.5" :x2 "15" :y2 "11.5"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-accent)"}}
        "Un revenu complémentaire"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Vendez votre énergie à un tarif supérieur au rachat EDF OA. "
        "Un complément de revenu stable, versé chaque mois, "
        "sans effort de votre part."]]

      ;; Card 3 — Local
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
               :stroke "var(--color-green)" :stroke-width "1.5"
               :stroke-linecap "round" :stroke-linejoin "round"}
         [:circle {:cx "12" :cy "5" :r "3"}]
         [:circle {:cx "5" :cy "19" :r "3"}]
         [:circle {:cx "19" :cy "19" :r "3"}]
         [:line {:x1 "12" :y1 "8" :x2 "5" :y2 "16"}]
         [:line {:x1 "12" :y1 "8" :x2 "19" :y2 "16"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Circuit court de l'énergie"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Votre énergie reste locale\u00a0: elle est consommée par vos voisins, "
        "dans votre commune. Un geste concret pour la transition "
        "énergétique de votre territoire."]]]

     ;; Banner — Elink-co handles everything
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
       "Elink-co s'occupe de toutes les démarches administratives\u00a0: "
       "convention avec Enedis, relation avec EDF, suivi de production "
       "et versement de vos revenus. Vous n'avez rien à gérer."]]

     ;; CTA
     [:div {:style {:text-align "center" :margin-top "2.5rem"}}
      [:a.btn.btn--green {:href "/comment-ca-marche#producteur"} "Comment ça marche"]
      [:a.btn.btn--accent {:href (rfee/href :page/signup)
                           :style {:margin-left "1rem"}} "Adhérer"]]]]])
