(ns app.pages.portal
  (:require [app.consumptions.utils :as conso-utils]
            [app.pages.admin :as admin]
            [app.pages.consumptions :as consumptions]
            [app.pages.contracts :as contracts]
            [app.pages.productions :as productions]
            [app.pages.profile :as profile]
            [re-frame.core :as rf]))

(defn- sidebar []
  (let [active @(rf/subscribe [:portal/active-section])
        admin? @(rf/subscribe [:auth/admin?])]
    [:nav.sidebar
     [:ul.sidebar__list
      [:li.sidebar__item
       {:class    (when (= :dashboard active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :dashboard])}
       "Tableau de bord"]
      [:li.sidebar__item
       {:class    (when (= :consumptions active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :consumptions])}
       "Mes consommations"]
      [:li.sidebar__item
       {:class    (when (= :productions active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :productions])}
       "Mes productions"]
      [:li.sidebar__item
       {:class    (when (= :contracts active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :contracts])}
       "Mes contrats"]
      [:li.sidebar__item
       {:class    (when (= :profile active) "sidebar__item--active")
        :on-click #(rf/dispatch [:portal/set-section :profile])}
       "Mon profil"]
      (when admin?
        [:<>
         [:li.sidebar__item.sidebar__item--separator]
         [:li.sidebar__item
          {:class    (when (= :admin-users active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-users])
                          (rf/dispatch [:admin/fetch-users]))}
          "Utilisateurs"]
         [:li.sidebar__item
          {:class    (when (= :admin-networks active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-networks])
                          (rf/dispatch [:admin/fetch-networks]))}
          "Réseaux"]
         [:li.sidebar__item
          {:class    (when (= :admin-eligibility active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-eligibility])
                          (rf/dispatch [:admin/fetch-eligibility-checks]))}
          "Éligibilités"]
         [:li.sidebar__item
          {:class    (when (= :admin-consumptions active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-consumptions])
                          (rf/dispatch [:admin/fetch-consumptions]))}
          "Consommations"]
         [:li.sidebar__item
          {:class    (when (= :admin-productions active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-productions])
                          (rf/dispatch [:admin/fetch-productions]))}
          "Productions"]
         [:li.sidebar__item
          {:class    (when (= :admin-contracts active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-contracts])
                          (rf/dispatch [:admin/fetch-users])
                          (rf/dispatch [:admin/fetch-consumptions])
                          (rf/dispatch [:admin/fetch-productions]))}
          "Contrats"]
         [:li.sidebar__item
          {:class    (when (= :admin-alert active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-alert])
                          (rf/dispatch [:admin/fetch-alert]))}
          "Alerte"]
         [:li.sidebar__item
          {:class    (when (= :admin-news active) "sidebar__item--active")
           :on-click #(do (rf/dispatch [:portal/set-section :admin-news])
                          (rf/dispatch [:admin/fetch-news]))}
          "Actualit\u00e9s"]])]]))

(def ^:private lifecycle-labels
  {"active"               "Actif"
   "pending"              "En attente d'activation"
   "consumer-information" "En cours de cr\u00e9ation"
   "linky-reference"      "En cours de cr\u00e9ation"
   "billing-address"      "En cours de cr\u00e9ation"
   "contract-signature"   "Signature en cours"
   "producer-information" "En cours de cr\u00e9ation"
   "installation-info"    "En cours de cr\u00e9ation"
   "payment-info"         "En cours de cr\u00e9ation"})

(defn- status-color [lifecycle]
  (case lifecycle
    "active"  "#2e7d32"
    "pending" "#e65100"
    "#757575"))

(defn- dashboard-section []
  ;; Fetch on mount
  (rf/dispatch [:auth/refresh-user])
  (rf/dispatch [:consumptions/fetch])
  (rf/dispatch [:productions/fetch])
  (fn []
    (let [user          @(rf/subscribe [:auth/user])
          user-name     (or (:user/name user)
                            (some-> (:natural-person user)
                                    ((fn [np] (str (:first-name np) " " (:last-name np))))))
          adhesion?     (some? (:adhesion-signed-at user))
          consumptions  @(rf/subscribe [:consumptions/list])
          productions   @(rf/subscribe [:productions/list])
          conso-loading? @(rf/subscribe [:consumptions/loading?])
          prod-loading?  @(rf/subscribe [:productions/loading?])
          conso-loaded? (some? consumptions)
          prod-loaded?  (some? productions)
          has-conso?    (seq consumptions)
          has-prod?     (seq productions)
          active-consos (filterv #(= "active" (:consumption/lifecycle %)) consumptions)
          active-prods  (filterv #(= "active" (:production/lifecycle %)) productions)
          pending-consos (filterv #(= "pending" (:consumption/lifecycle %)) consumptions)
          pending-prods  (filterv #(= "pending" (:production/lifecycle %)) productions)
          onboarding-consos (filterv #(contains? #{"consumer-information" "linky-reference" "billing-address" "contract-signature"}
                                                  (:consumption/lifecycle %)) consumptions)
          onboarding-prods  (filterv #(contains? #{"producer-information" "installation-info" "payment-info" "contract-signature"}
                                                  (:production/lifecycle %)) productions)
          total-conso-monthly (reduce + 0 (keep conso-utils/latest-monthly-kwh active-consos))
          total-prod-monthly  (reduce + 0
                                       (keep (fn [p]
                                               (when-let [h (seq (:production/monthly-history p))]
                                                 (:kwh (first (sort-by (juxt :year :month) #(compare %2 %1) h)))))
                                             active-prods))]
      [:div.dashboard
       ;; Welcome
       [:h1 {:style {:margin-bottom "0.25rem"}}
        (str "Bienvenue" (when (seq user-name) (str ", " user-name)) "\u00a0!")]
       [:p {:style {:color "var(--color-muted)" :font-size "0.95rem" :margin-bottom "1.5rem"}}
        "Voici un r\u00e9sum\u00e9 de votre activit\u00e9 \u00e9nerg\u00e9tique sur la plateforme Elink-co."]

       (cond
         (or conso-loading? prod-loading? (not conso-loaded?) (not prod-loaded?))
         [:p.loading "Chargement..."]

         ;; No consumption and no production → onboarding
         (and (not has-conso?) (not has-prod?))
         [:div
          [:p {:style {:font-size "1.05rem" :margin-bottom "1rem" :line-height "1.6"}}
           "Vous n\u2019avez pas encore de point de consommation ni de site de production. "
           "L\u2019autoconsommation collective vous permet de consommer ou de produire de l\u2019\u00e9lectricit\u00e9 "
           "verte partag\u00e9e entre voisins. Choisissez comment vous souhaitez participer\u00a0:"]
          [:div {:style {:display "flex" :gap "1rem" :flex-wrap "wrap" :margin-top "1rem"}}
           [:button.btn.btn--green
            {:on-click (fn []
                         (rf/dispatch [:consumptions/create])
                         (rf/dispatch [:portal/set-section :consumptions]))
             :style {:flex "1" :min-width "220px" :padding "1rem"}}
            [:div {:style {:display "flex" :flex-direction "column" :align-items "center" :gap "0.3rem"}}
             [:span {:style {:font-size "1.1rem"}} "\u26a1 Je veux consommer"]
             [:span {:style {:font-size "0.8rem" :opacity "0.85"}} "de l\u2019\u00e9lectricit\u00e9 locale"]]]
           [:button.btn.btn--green
            {:on-click (fn []
                         (rf/dispatch [:productions/create])
                         (rf/dispatch [:portal/set-section :productions]))
             :style {:flex "1" :min-width "220px" :padding "1rem"}}
            [:div {:style {:display "flex" :flex-direction "column" :align-items "center" :gap "0.3rem"}}
             [:span {:style {:font-size "1.1rem"}} "\u2600\ufe0f Je veux produire"]
             [:span {:style {:font-size "0.8rem" :opacity "0.85"}} "de l\u2019\u00e9lectricit\u00e9 renouvelable"]]]]]

         ;; Has activity
         :else
         [:div {:style {:display "flex" :flex-direction "column" :gap "1.5rem"}}

          ;; ── Adhesion status ──
          (when-not adhesion?
            [:div {:style {:background "#fff3e0" :border "1px solid #ffe082"
                           :border-radius "var(--radius)" :padding "1rem"}}
             [:p {:style {:color "#e65100" :font-weight "600" :margin-bottom "0.5rem"}}
              "Adh\u00e9sion Elink-co non sign\u00e9e"]
             [:p {:style {:font-size "0.9rem" :color "var(--color-muted)"}}
              "Pour finaliser vos contrats, vous devez signer l\u2019adh\u00e9sion \u00e0 l\u2019association Elink-co. "
              "Rendez-vous dans la section "
              [:a {:href "#" :style {:color "var(--color-green)" :font-weight "600"}
                   :on-click (fn [e] (.preventDefault e)
                               (rf/dispatch [:portal/set-section :contracts]))}
               "Mes contrats"]
              " pour la signer."]])

          ;; ── Actions en cours ──
          (when (or (seq onboarding-consos) (seq onboarding-prods)
                    (seq pending-consos) (seq pending-prods))
            [:div {:style {:background "#fff8e1" :border "1px solid #ffe082"
                           :border-radius "var(--radius)" :padding "1rem"}}
             [:h3 {:style {:margin "0 0 0.75rem" :font-size "1rem" :color "#e65100"}}
              "Actions en cours"]
             (when (seq onboarding-consos)
               [:div {:style {:margin-bottom "0.5rem"}}
                [:p {:style {:font-size "0.9rem"}}
                 (str (count onboarding-consos)
                      " consommation" (when (> (count onboarding-consos) 1) "s")
                      " en cours de cr\u00e9ation \u2014 ")
                 [:a {:href "#" :style {:color "var(--color-green)" :font-weight "600"}
                      :on-click (fn [e] (.preventDefault e)
                                  (rf/dispatch [:portal/set-section :consumptions]))}
                  "Reprendre"]]])
             (when (seq onboarding-prods)
               [:div {:style {:margin-bottom "0.5rem"}}
                [:p {:style {:font-size "0.9rem"}}
                 (str (count onboarding-prods)
                      " production" (when (> (count onboarding-prods) 1) "s")
                      " en cours de cr\u00e9ation \u2014 ")
                 [:a {:href "#" :style {:color "var(--color-green)" :font-weight "600"}
                      :on-click (fn [e] (.preventDefault e)
                                  (rf/dispatch [:portal/set-section :productions]))}
                  "Reprendre"]]])
             (when (seq pending-consos)
               [:div {:style {:margin-bottom "0.5rem"}}
                [:p {:style {:font-size "0.9rem"}}
                 (str (count pending-consos)
                      " consommation" (when (> (count pending-consos) 1) "s")
                      " en attente d\u2019activation par l\u2019administrateur.")]])
             (when (seq pending-prods)
               [:div
                [:p {:style {:font-size "0.9rem"}}
                 (str (count pending-prods)
                      " production" (when (> (count pending-prods) 1) "s")
                      " en attente d\u2019activation par l\u2019administrateur.")]])])

          ;; ── Consommations actives ──
          (when has-conso?
            [:div
             [:h2 {:style {:font-size "1.1rem" :color "var(--color-green)" :margin-bottom "0.75rem"}}
              (str "\u26a1 Mes consommations (" (count active-consos) " active"
                   (when (> (count active-consos) 1) "s") ")")]
             [:div {:style {:display "flex" :gap "1rem" :flex-wrap "wrap" :margin-bottom "0.75rem"}}
              [:div.nd-stat-card
               [:span.nd-stat-value (count active-consos)]
               [:span.nd-stat-label "Point(s) de consommation actif(s)"]]
              [:div.nd-stat-card
               [:span.nd-stat-value (if (pos? total-conso-monthly)
                                      (str (.toFixed total-conso-monthly 1) " kWh")
                                      "\u2014")]
               [:span.nd-stat-label "Consommation totale du mois pr\u00e9c\u00e9dent"]]]
             ;; List each active consumption
             (doall
               (for [c active-consos]
                 ^{:key (:consumption/id c)}
                 [:div {:style {:padding "0.5rem 0.75rem" :margin-bottom "0.5rem"
                                :background "var(--color-green-pale)" :border-radius "var(--radius)"
                                :display "flex" :justify-content "space-between" :align-items "center"}}
                  [:div
                   [:span {:style {:font-weight "600"}} (or (:consumption/consumer-address c) "Consommation")]
                   (when-let [kwh (conso-utils/latest-monthly-kwh c)]
                     [:span {:style {:color "var(--color-muted)" :margin-left "0.75rem" :font-size "0.85rem"}}
                      (str (.toFixed kwh 1) " kWh ce mois")])]
                  [:button.btn.btn--small.btn--outline
                   {:on-click #(rf/dispatch [:portal/set-section :consumptions])}
                   "Voir"]]))
             [:p {:style {:font-size "0.85rem" :color "var(--color-muted)" :margin-top "0.5rem"}}
              "Retrouvez le d\u00e9tail de chaque consommation (historique, contrats, informations bancaires) dans "
              [:a {:href "#" :style {:color "var(--color-green)"}
                   :on-click (fn [e] (.preventDefault e)
                               (rf/dispatch [:portal/set-section :consumptions]))}
               "Mes consommations"] "."]])

          ;; ── Productions actives ──
          (when has-prod?
            [:div
             [:h2 {:style {:font-size "1.1rem" :color "var(--color-green)" :margin-bottom "0.75rem"}}
              (str "\u2600\ufe0f Mes productions (" (count active-prods) " active"
                   (when (> (count active-prods) 1) "s") ")")]
             [:div {:style {:display "flex" :gap "1rem" :flex-wrap "wrap" :margin-bottom "0.75rem"}}
              [:div.nd-stat-card
               [:span.nd-stat-value (count active-prods)]
               [:span.nd-stat-label "Site(s) de production actif(s)"]]
              [:div.nd-stat-card
               [:span.nd-stat-value (if (pos? total-prod-monthly)
                                      (str (.toFixed total-prod-monthly 1) " kWh")
                                      "\u2014")]
               [:span.nd-stat-label "Production totale du mois pr\u00e9c\u00e9dent"]]]
             ;; List each active production
             (doall
               (for [p active-prods]
                 ^{:key (:production/id p)}
                 [:div {:style {:padding "0.5rem 0.75rem" :margin-bottom "0.5rem"
                                :background "var(--color-green-pale)" :border-radius "var(--radius)"
                                :display "flex" :justify-content "space-between" :align-items "center"}}
                  [:div
                   [:span {:style {:font-weight "600"}}
                    (str (or (:production/producer-address p) "Production")
                         " \u2014 " (:production/energy-type p)
                         " " (:production/installed-power p) " kWh")]
                   (when-let [h (seq (:production/monthly-history p))]
                     (let [latest (:kwh (first (sort-by (juxt :year :month) #(compare %2 %1) h)))]
                       [:span {:style {:color "var(--color-muted)" :margin-left "0.75rem" :font-size "0.85rem"}}
                        (str (.toFixed latest 1) " kWh ce mois")]))]
                  [:button.btn.btn--small.btn--outline
                   {:on-click #(rf/dispatch [:portal/set-section :productions])}
                   "Voir"]]))
             [:p {:style {:font-size "0.85rem" :color "var(--color-muted)" :margin-top "0.5rem"}}
              "Retrouvez le d\u00e9tail de chaque production (historique, consommateurs, facturation) dans "
              [:a {:href "#" :style {:color "var(--color-green)"}
                   :on-click (fn [e] (.preventDefault e)
                               (rf/dispatch [:portal/set-section :productions]))}
               "Mes productions"] "."]])

          ;; ── Quick actions ──
          [:div {:style {:margin-top "0.5rem" :padding-top "1rem" :border-top "1px solid var(--color-border)"}}
           [:h3 {:style {:font-size "0.95rem" :color "var(--color-muted)" :margin-bottom "0.75rem"}}
            "Actions rapides"]
           [:div {:style {:display "flex" :gap "0.75rem" :flex-wrap "wrap"}}
            [:button.btn.btn--small.btn--outline
             {:on-click (fn []
                          (rf/dispatch [:consumptions/create])
                          (rf/dispatch [:portal/set-section :consumptions]))}
             "+ Ajouter une consommation"]
            [:button.btn.btn--small.btn--outline
             {:on-click (fn []
                          (rf/dispatch [:productions/create])
                          (rf/dispatch [:portal/set-section :productions]))}
             "+ Ajouter une production"]
            [:button.btn.btn--small.btn--outline
             {:on-click #(rf/dispatch [:portal/set-section :contracts])}
             "Voir mes contrats"]
            [:button.btn.btn--small.btn--outline
             {:on-click #(rf/dispatch [:portal/set-section :profile])}
             "Mon profil"]]]])])))



(defn portal-page []
  (let [active @(rf/subscribe [:portal/active-section])]
    [:div.portal
     [:div.portal__sidebar
      [sidebar]]
     [:div.portal__content
      (case active
        :consumptions      [consumptions/consumptions-page]
        :productions       [productions/productions-page]
        :contracts         [contracts/contracts-page]
        :profile           [profile/profile-page]
        :admin-users       [admin/users-tab]
        :admin-networks    [admin/networks-tab]
        :admin-consumptions [admin/consumptions-tab]
        :admin-eligibility [admin/eligibility-checks-tab]
        :admin-productions [admin/productions-tab]
        :admin-contracts   [admin/contracts-tab]
        :admin-alert       [admin/alert-tab]
        :admin-news        [admin/news-tab]
        [dashboard-section])]]))
