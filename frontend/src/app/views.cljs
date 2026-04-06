(ns app.views
  (:require [app.config :as config]
            [app.pages.about :as about]
            [app.pages.check-email :as check-email]
            [app.pages.contact :as contact]
            [app.pages.faq :as faq]
            [app.pages.for-business :as for-business]
            [app.pages.for-farmers :as for-farmers]
            [app.pages.for-individual-consumer :as for-individual-consumer]
            [app.pages.for-individual-producer :as for-individual-producer]
            [app.pages.forgot-password :as forgot-password]
            [app.pages.home :as home]
            [app.pages.how-it-works :as how-it-works]
            [app.pages.legal :as legal]
            [app.pages.network-detail :as network-detail]
            [app.pages.news :as news]
            [app.pages.login :as login]
            [app.pages.portal :as portal]
            [app.pages.reset-password :as reset-password]
            [app.pages.testimonials :as testimonials]
            [app.pages.verify-email :as verify-email]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as rfee]))

(defn- env-banner []
  (when (not= config/APP_ENV "prod")
    [:div {:style {:background "#ffffff" :color "#000" :text-align "center"
                   :padding "4px 0" :font-size "0.75rem" :font-weight "bold"
                   :letter-spacing "0.05em" :text-transform "uppercase"
                   :position "sticky" :top 0 :z-index 9999}}
     config/APP_ENV]))

(defn- alert-banner []
  (let [active? @(rf/subscribe [:alert/active?])
        message @(rf/subscribe [:alert/message])]
    (when (and active? (seq message))
      [:div {:style {:background "#d32f2f" :color "#fff" :text-align "center"
                     :padding "10px 16px" :font-size "0.95rem" :font-weight "600"
                     :display "flex" :align-items "center" :justify-content "center"
                     :gap "8px"
                     :position "sticky" :top 0 :z-index 9998}}
       [:svg {:width "20" :height "20" :viewBox "0 0 24 24"
              :fill "none" :stroke "currentColor" :stroke-width "2"
              :stroke-linecap "round" :stroke-linejoin "round"
              :style {:flex-shrink "0"}}
        [:path {:d "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"}]
        [:line {:x1 "12" :y1 "9" :x2 "12" :y2 "13"}]
        [:line {:x1 "12" :y1 "17" :x2 "12.01" :y2 "17"}]]
       [:span message]])))

(defn- navbar []
  (r/with-let [dropdown-open? (r/atom false)
               about-open?    (r/atom false)
               mobile-open?   (r/atom false)]
    (let [logged-in? @(rf/subscribe [:auth/logged-in?])
          user-name  @(rf/subscribe [:auth/user-name])]
      [:nav.navbar
       [:button.navbar__burger
        {:on-click #(swap! mobile-open? not)
         :aria-label "Menu"}
        [:svg {:width "24" :height "24" :viewBox "0 0 24 24" :fill "none"
               :stroke "currentColor" :stroke-width "2"
               :stroke-linecap "round" :stroke-linejoin "round"}
         (if @mobile-open?
           [:<>
            [:line {:x1 "18" :y1 "6" :x2 "6" :y2 "18"}]
            [:line {:x1 "6" :y1 "6" :x2 "18" :y2 "18"}]]
           [:<>
            [:line {:x1 "3" :y1 "6" :x2 "21" :y2 "6"}]
            [:line {:x1 "3" :y1 "12" :x2 "21" :y2 "12"}]
            [:line {:x1 "3" :y1 "18" :x2 "21" :y2 "18"}]])]]
       [:a.navbar__logo {:href "/"}
        [:img.navbar__logo-img {:src "/img/logo-elinkco.jpg" :alt "elink-co"}]]
       [:div.navbar__menu
        {:class (when @mobile-open? "navbar__menu--open")}
        [:div.navbar__dropdown
         {:on-mouse-enter #(reset! dropdown-open? true)
          :on-mouse-leave #(reset! dropdown-open? false)}
         [:span.navbar__menu-item.navbar__menu-item--dropdown
          "Pour qui\u00a0?"
          [:svg {:width "12" :height "12" :viewBox "0 0 24 24" :fill "none"
                 :stroke "currentColor" :stroke-width "2.5"
                 :style {:margin-left "4px" :vertical-align "middle"}}
           [:polyline {:points "6 9 12 15 18 9"}]]]
         (when @dropdown-open?
           [:div.navbar__dropdown-menu
            [:a.navbar__dropdown-item {:href (rfee/href :page/for-individual-producer)}
             "Producteur particulier"]
            [:a.navbar__dropdown-item {:href (rfee/href :page/for-individual-consumer)}
             "Consommateur particulier"]
            [:a.navbar__dropdown-item {:href (rfee/href :page/for-business)}
             "Entreprise"]
            [:a.navbar__dropdown-item {:href (rfee/href :page/for-farmers)}
             "Agriculteurs"]])]
        [:a.navbar__menu-item {:href (rfee/href :page/news)} "Actualit\u00e9s"]
        [:div.navbar__dropdown
         {:on-mouse-enter #(reset! about-open? true)
          :on-mouse-leave #(reset! about-open? false)}
         [:span.navbar__menu-item.navbar__menu-item--dropdown
          "\u00c0 propos"
          [:svg {:width "12" :height "12" :viewBox "0 0 24 24" :fill "none"
                 :stroke "currentColor" :stroke-width "2.5"
                 :style {:margin-left "4px" :vertical-align "middle"}}
           [:polyline {:points "6 9 12 15 18 9"}]]]
         (when @about-open?
           [:div.navbar__dropdown-menu
            [:a.navbar__dropdown-item {:href (rfee/href :page/about)}
             "Qui sommes-nous\u00a0?"]
            [:a.navbar__dropdown-item {:href (rfee/href :page/testimonials)}
             "T\u00e9moignages"]
            [:a.navbar__dropdown-item {:href (rfee/href :page/contact)}
             "Contact"]])]
        (when-not logged-in?
          [:a.btn.btn--accent.btn--small
           {:href     (rfee/href :page/signup)
            :on-click #(rf/dispatch [:eligibility/set-join-network nil])}
           "Adh\u00e9rer"])]
       [:div.navbar__auth
        (if logged-in?
          [:div.navbar__user
           [:span user-name]
           [:button.btn.btn--small
            {:on-click #(rf/dispatch [:auth/logout])}
            "D\u00e9connexion"]]
          [:a.btn.btn--small.navbar__account-btn
           {:href (rfee/href :page/login)}
           [:span.navbar__account-label "Mon compte"]
           [:svg.navbar__account-icon {:width "22" :height "22" :viewBox "0 0 24 24" :fill "none"
                                       :stroke "currentColor" :stroke-width "2"
                                       :stroke-linecap "round" :stroke-linejoin "round"}
            [:path {:d "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"}]
            [:circle {:cx "12" :cy "7" :r "4"}]]])]])))


(defn- current-page []
  (let [page       @(rf/subscribe [:router/current-page])
        logged-in? @(rf/subscribe [:auth/logged-in?])]
    (case page
      :page/login        (if logged-in?
                           (do (rf/dispatch [:router/navigate :page/portal]) nil)
                           [login/login-page {:signup? false}])
      :page/signup       (if logged-in?
                           (do (rf/dispatch [:router/navigate :page/portal]) nil)
                           [login/login-page {:signup? true}])
      :page/portal       (if logged-in?
                           [portal/portal-page]
                           (do (rf/dispatch [:router/navigate :page/home]) nil))
      :page/verify-email    [verify-email/verify-email-page]
      :page/check-email     [check-email/check-email-page]
      :page/forgot-password [forgot-password/forgot-password-page]
      :page/reset-password  [reset-password/reset-password-page]
      :page/network-detail  [network-detail/network-detail-page]
      :page/faq             [faq/faq-page]
      :page/about           [about/about-page]
      :page/how-it-works    [how-it-works/how-it-works-page]
      :page/for-individual-consumer [for-individual-consumer/for-individual-consumer-page]
      :page/for-individual-producer [for-individual-producer/for-individual-producer-page]
      :page/for-business    [for-business/for-business-page]
      :page/for-farmers     [for-farmers/for-farmers-page]
      :page/testimonials    [testimonials/testimonials-page]
      :page/contact         [contact/contact-page]
      :page/news            [news/news-page]
      :page/legal           [legal/legal-page]
      [home/home-page])))

(defn main-panel []
  [:<>
   [env-banner]
   [alert-banner]
   [navbar]
   [current-page]
   [:footer.footer
    [:span "\u00a9 2026 elink-co \u2014 \u00c9nergie locale partag\u00e9e"]
    [:a.footer__link {:href (rfee/href :page/about)} "Qui sommes-nous"]
    [:a.footer__link {:href (rfee/href :page/faq)} "FAQ"]
    [:a.footer__link {:href (rfee/href :page/legal)} "Mentions l\u00e9gales"]]])
