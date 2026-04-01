(ns app.pages.news
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- format-date [iso-str]
  (when iso-str
    (try
      (let [d (js/Date. iso-str)]
        (str (.getDate d) "/"
             (inc (.getMonth d)) "/"
             (.getFullYear d)))
      (catch :default _ iso-str))))

(defn- news-card [{:keys [news/title news/content news/image-url news/published-at]}]
  [:div.news-card
   (when (seq image-url)
     [:img.news-card__image {:src image-url :alt title}])
   [:div.news-card__body
    [:span.news-card__date (format-date published-at)]
    [:h3.news-card__title title]
    [:p.news-card__excerpt
     (if (> (count content) 200)
       (str (subs content 0 200) "\u2026")
       content)]]])

(defn news-page []
  (r/create-class
    {:component-did-mount
     (fn [_] (rf/dispatch [:news/fetch]))

     :reagent-render
     (fn []
       (let [news     @(rf/subscribe [:news/list])
             loading? @(rf/subscribe [:news/loading?])]
         [:div.landing
          [:section.section
           [:div.container
            [:h1.section__title "Actualit\u00e9s"]
            [:p.section__subtitle "Suivez les derni\u00e8res nouvelles de l'autoconsommation collective et de la production locale."]
            (cond
              loading?
              [:p.loading "Chargement..."]

              (empty? news)
              [:p "Aucune actualit\u00e9 pour le moment."]

              :else
              [:div.news-grid
               (doall
                 (for [n news]
                   ^{:key (:news/id n)}
                   [news-card n]))])]]]))}))
