(ns app.pages.testimonials)

(def ^:private testimonials-data
  [{:name "Marie L."
    :role "Consommatrice — Réseau de Villefranche"
    :quote "Depuis que j'ai rejoint le réseau Elink-co, je consomme une électricité locale et ma facture a baissé. L'inscription a été très simple, l'équipe s'est occupée de tout."}
   {:name "Pierre D."
    :role "Producteur — Réseau de Saint-Étienne"
    :quote "Je produis plus que ce que je consomme avec mes panneaux. Grâce à Elink-co, je vends mon surplus à mes voisins au lieu de le brader à EDF OA. C'est plus rentable et plus humain."}
   {:name "Sophie M."
    :role "Consommatrice — Réseau de Beaujeu"
    :quote "Ce qui m'a convaincue, c'est la simplicité. Je n'ai pas changé de fournisseur, je n'ai rien eu à installer, et je sais exactement d'où vient mon électricité."}
   {:name "Jean-Marc R."
    :role "Agriculteur — Réseau de Belleville"
    :quote "Mes hangars agricoles produisent de l'électricité pour tout le village. C'est un vrai complément de revenu et ça donne du sens à mon exploitation."}
   {:name "Nathalie et François B."
    :role "Consommateurs — Réseau de Villefranche"
    :quote "On voulait agir pour l'environnement sans se compliquer la vie. Elink-co nous a permis de passer à l'énergie verte locale en quelques clics."}])

(defn- testimonial-card [{:keys [name role quote]}]
  [:div {:style {:background "#fff" :border-radius "12px" :padding "2rem"
                 :box-shadow "0 2px 12px rgba(0,0,0,0.07)"
                 :max-width "600px" :margin "0 auto"}}
   [:p {:style {:font-size "1.1rem" :line-height "1.7" :color "var(--color-text)"
                :font-style "italic" :margin-bottom "1.5rem"}}
    "\u00ab\u00a0" quote "\u00a0\u00bb"]
   [:p {:style {:font-weight "700" :color "var(--color-green)" :margin-bottom "0.2rem"}}
    name]
   [:p {:style {:font-size "0.9rem" :color "#888"}} role]])

(defn testimonials-page []
  [:section.section
   [:div.container
    [:h1.section__title "Témoignages"]
    [:p.section__subtitle
     "Ils ont rejoint un réseau Elink-co. Découvrez leurs retours d'expérience."]
    [:div {:style {:display "flex" :flex-direction "column" :gap "2rem"
                   :margin-top "2rem"}}
     (for [t testimonials-data]
       ^{:key (:name t)}
       [testimonial-card t])]]])
