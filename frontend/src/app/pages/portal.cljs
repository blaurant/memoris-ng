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

(defn- dashboard-section []
  ;; Fetch on mount
  (rf/dispatch [:auth/refresh-user])
  (rf/dispatch [:consumptions/fetch])
  (rf/dispatch [:productions/fetch])
  (fn []
    (let [user-name     @(rf/subscribe [:auth/user-name])
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
          total-conso-monthly (reduce + 0 (keep conso-utils/latest-monthly-kwh active-consos))
          total-prod-monthly  (reduce + 0
                                       (keep (fn [p]
                                               (when-let [h (seq (:production/monthly-history p))]
                                                 (:kwh (first (sort-by (juxt :year :month) #(compare %2 %1) h)))))
                                             active-prods))]
    [:div.dashboard
     [:h1 "Bienvenue\u00a0!"]
     (cond
       (or conso-loading? prod-loading? (not conso-loaded?) (not prod-loaded?))
       [:p "Chargement..."]

       (and (not has-conso?) (not has-prod?))
       [:div {:style {:margin-top "2rem"}}
        [:div {:style {:display "flex" :gap "1rem" :flex-wrap "wrap"}}
         [:button.btn.btn--green
          {:on-click (fn []
                       (rf/dispatch [:consumptions/create])
                       (rf/dispatch [:portal/set-section :consumptions]))
           :style {:flex "1" :min-width "200px"}}
          "Je veux devenir consommateur d'\u00e9lectricit\u00e9 locale"]
         [:button.btn.btn--green
          {:on-click #(rf/dispatch [:portal/set-section :productions])
           :style {:flex "1" :min-width "200px"}}
          "Je veux devenir producteur d'\u00e9lectricit\u00e9 locale"]]]

       :else
       [:div {:style {:margin-top "1.5rem" :display "flex" :flex-direction "column" :gap "1.5rem"}}
        (when has-conso?
          [:div {:style {:display "flex" :gap "1.5rem" :flex-wrap "wrap"}}
           [:div.nd-stat-card {:style {:max-width "250px"}}
            [:span.nd-stat-value (count active-consos)]
            [:span.nd-stat-label (str "Consommation" (when (> (count active-consos) 1) "s") " active" (when (> (count active-consos) 1) "s"))]]
           [:div.nd-stat-card {:style {:max-width "250px"}}
            [:span.nd-stat-value (if (pos? total-conso-monthly)
                                   (str (.toFixed total-conso-monthly 1) " kWh")
                                   "-")]
            [:span.nd-stat-label "Consommation du mois precedent"]]])
        (when has-prod?
          [:div {:style {:display "flex" :gap "1.5rem" :flex-wrap "wrap"}}
           [:div.nd-stat-card {:style {:max-width "250px"}}
            [:span.nd-stat-value (count active-prods)]
            [:span.nd-stat-label (str "Production" (when (> (count active-prods) 1) "s") " active" (when (> (count active-prods) 1) "s"))]]
           [:div.nd-stat-card {:style {:max-width "250px"}}
            [:span.nd-stat-value (if (pos? total-prod-monthly)
                                   (str (.toFixed total-prod-monthly 1) " kWh")
                                   "-")]
            [:span.nd-stat-label "Production du mois precedent"]]])])])))

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
