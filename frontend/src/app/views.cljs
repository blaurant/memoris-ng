(ns app.views
  (:require [app.config :as config]
            [app.pages.check-email :as check-email]
            [app.pages.forgot-password :as forgot-password]
            [app.pages.home :as home]
            [app.pages.network-detail :as network-detail]
            [app.pages.login :as login]
            [app.pages.portal :as portal]
            [app.pages.reset-password :as reset-password]
            [app.pages.verify-email :as verify-email]
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
  (let [logged-in? @(rf/subscribe [:auth/logged-in?])
        user-name  @(rf/subscribe [:auth/user-name])]
    [:nav.navbar
     [:a.navbar__logo {:href "/"}
      [:img.navbar__logo-img {:src "/img/logo-elinkco.jpg" :alt "elink-co"}]]
     [:div.navbar__auth
      (if logged-in?
        [:div.navbar__user
         [:span user-name]
         [:button.btn.btn--small
          {:on-click #(rf/dispatch [:auth/logout])}
          "Déconnexion"]]
        [:div.navbar__buttons
         [:a.btn.btn--green.btn--small
          {:href     (rfee/href :page/signup)
           :on-click #(rf/dispatch [:eligibility/set-join-network nil])}
          "Nouvel utilisateur"]
         [:a.btn.btn--small
          {:href (rfee/href :page/login)}
          "Se connecter"]])]]))

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
      [home/home-page])))

(defn main-panel []
  [:<>
   [env-banner]
   [alert-banner]
   [navbar]
   [current-page]
   [:footer.footer "© 2026 elink-co — Énergie locale partagée"]])
