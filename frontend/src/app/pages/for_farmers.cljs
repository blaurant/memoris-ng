(ns app.pages.for-farmers
  (:require [reitit.frontend.easy :as rfee]))

(defn for-farmers-page []
  [:<>
   ;; Hero with background image
   [:section {:style {:position "relative" :min-height "420px" :display "flex"
                      :align-items "flex-end"
                      :background "url('/img/agriculteur-producteur.png') center 20% / cover no-repeat"
                      :border-radius "0 0 1.5rem 1.5rem"}}
    [:div {:style {:position "absolute" :inset "0"
                   :background "linear-gradient(to top, rgba(0,0,0,0.65) 0%, rgba(0,0,0,0.1) 60%, transparent 100%)"
                   :border-radius "0 0 1.5rem 1.5rem"}}]
    [:div.container {:style {:position "relative" :z-index 1 :padding-bottom "2.5rem"
                             :color "#fff" :max-width "380px" :margin-left "2rem"}}
     [:h1 {:style {:font-size "2.4rem" :font-weight "800" :margin-bottom "0.5rem"}}
      "Agriculteurs"]
     [:p {:style {:font-size "1.1rem" :line-height "1.6" :opacity "0.95"}}
      "Diversifiez vos revenus en valorisant vos installations "
      "photovoltaïques auprès de votre communauté locale."]]]

   ;; Content
   [:section.section
    [:div.container
     [:div {:style {:display "grid" :grid-template-columns "1fr 1fr 1fr" :gap "2rem"
                    :margin-top "2rem"}}

      ;; Card 1
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Vos toitures, votre énergie"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Hangars, granges, étables\u2026 vos surfaces de toiture sont idéales pour "
        "accueillir des panneaux solaires. Produisez de l'électricité verte sans "
        "impacter votre activité agricole."]]

      ;; Card 2
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-accent)"}}
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-accent)"}}
        "Un revenu complémentaire"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "Vendez votre surplus d'énergie directement aux habitants et entreprises de "
        "votre commune, à un tarif plus avantageux que le rachat EDF OA. "
        "Un complément de revenu stable et prévisible."]]

      ;; Card 3
      [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                     :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                     :border-top "3px solid var(--color-green)"}}
       [:h3 {:style {:font-size "1.15rem" :margin-bottom "0.75rem" :color "var(--color-green)"}}
        "Ancrage territorial"]
       [:p {:style {:font-size "1rem" :line-height "1.7" :color "var(--color-text)"}}
        "En partageant votre énergie localement, vous renforcez le lien avec "
        "votre communauté. Un circuit court de l'énergie, du producteur au consommateur, "
        "au sein de votre commune."]]]

     ;; Elink-co handles everything
     [:div {:style {:background "var(--color-green-light)" :border-radius "12px"
                    :padding "2rem 2.5rem" :margin-top "2.5rem"
                    :text-align "center"}}
      [:p {:style {:font-size "1.1rem" :line-height "1.8" :color "var(--color-text)"}}
       "Elink-co s'occupe de toutes les démarches administratives\u00a0: convention avec Enedis, "
       "relation avec EDF, suivi de production et versement de vos revenus. "
       "Vous vous concentrez sur votre exploitation."]]

     ;; CTA
     [:div {:style {:text-align "center" :margin-top "2.5rem"}}
      [:a.btn.btn--green {:href "/comment-ca-marche#producteur"} "Comment ça marche"]
      [:a.btn.btn--accent {:href (rfee/href :page/signup)
                           :style {:margin-left "1rem"}} "Adhérer"]]]]])
