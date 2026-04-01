(ns app.pages.for-individual-consumer
  (:require [reitit.frontend.easy :as rfee]))

(defn for-individual-consumer-page []
  [:<>
   ;; Hero
   [:section {:style {:position "relative" :min-height "420px" :display "flex"
                      :align-items "flex-end"
                      :background "url('/img/consommateur-particulier.png') center 40% / cover no-repeat"
                      :border-radius "0 0 1.5rem 1.5rem"}}
    [:div {:style {:position "absolute" :inset "0"
                   :background "linear-gradient(to top, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.1) 60%, transparent 100%)"
                   :border-radius "0 0 1.5rem 1.5rem"}}]
    [:div.container {:style {:position "relative" :z-index 1 :padding-bottom "2.5rem"
                             :color "#fff" :max-width "600px"}}
     [:h1 {:style {:font-size "2.2rem" :font-weight "800" :line-height "1.2"}}
      "Consommateur particulier"]]]

   ;; Subtitle
   [:div.container {:style {:text-align "center" :margin-top "1.5rem" :margin-bottom "0.5rem"}}
    [:p {:style {:font-size "1.15rem" :line-height "1.7" :color "var(--color-text)"
                 :max-width "700px" :margin "0 auto"}}
     "Réduisez votre facture d'électricité en consommant une énergie verte, "
     "produite localement par vos voisins."]]

   ;; Cards
   [:section.section
    [:div.container
     [:div {:style {:display "grid" :grid-template-columns "1fr 1fr 1fr" :gap "2rem"
                    :margin-top "1rem"}}

      ;; Card 1 — Énergie locale
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
        "Une énergie verte et locale"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Achetez directement l'électricité produite par les installations "
        "solaires de votre commune. Une énergie propre, en circuit court, "
        "consommée à quelques kilomètres de chez vous."]]

      ;; Card 2 — Économies
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
        "Réduisez votre facture"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Bénéficiez d'un tarif plus avantageux que le tarif réglementé. "
        "Pas besoin de changer de fournisseur\u00a0: EDF reste votre "
        "interlocuteur pour le complément."]]

      ;; Card 3 — Simplicité
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:div {:style {:margin-bottom "1rem"}}
        [:svg {:width "36" :height "36" :viewBox "0 0 24 24" :fill "none"
               :stroke "var(--color-green)" :stroke-width "1.5"
               :stroke-linecap "round" :stroke-linejoin "round"}
         [:path {:d "M22 11.08V12a10 10 0 1 1-5.93-9.14"}]
         [:polyline {:points "22 4 12 14.01 9 11.01"}]]]
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Aucune installation requise"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Enedis continue d'assurer la livraison via le réseau existant. "
        "Aucun travaux chez vous, aucun changement de compteur. "
        "Vous consommez simplement plus responsable."]]]

     ;; Banner
     [:div {:style {:background "var(--color-green-pale)" :border-radius "12px"
                    :padding "2rem 2.5rem" :margin-top "2.5rem"
                    :display "flex" :align-items "center" :gap "1.5rem"}}
      [:svg {:width "40" :height "40" :viewBox "0 0 24 24" :fill "none"
             :stroke "var(--color-green)" :stroke-width "1.5"
             :stroke-linecap "round" :stroke-linejoin "round"
             :style {:flex-shrink "0"}}
       [:path {:d "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"}]
       [:circle {:cx "9" :cy "7" :r "4"}]
       [:path {:d "M23 21v-2a4 4 0 0 0-3-3.87"}]
       [:path {:d "M16 3.13a4 4 0 0 1 0 7.75"}]]
      [:p {:style {:font-size "1.05rem" :line-height "1.8" :color "var(--color-text)"}}
       "Adhérez simplement au réseau Elink-co. Nous nous occupons de toutes "
       "les démarches entre vous, Enedis, EDF et les producteurs locaux."]]

     ;; CTA
     [:div {:style {:text-align "center" :margin-top "2.5rem"}}
      [:a.btn.btn--green {:href "/comment-ca-marche#consommateur"} "Comment ça marche"]
      [:a.btn.btn--accent {:href (rfee/href :page/signup)
                           :style {:margin-left "1rem"}} "Adhérer"]]]]])
