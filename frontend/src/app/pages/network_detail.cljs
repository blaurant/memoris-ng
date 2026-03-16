(ns app.pages.network-detail
  (:require [re-frame.core :as rf]))

(defn network-detail-page []
  (let [loading? @(rf/subscribe [:network-detail/loading?])
        data     @(rf/subscribe [:network-detail/data])
        error    @(rf/subscribe [:network-detail/error])]
    (cond
      loading?
      [:div.network-detail [:p "Chargement..."]]

      error
      [:div.network-detail [:p "Erreur de chargement du reseau."]]

      data
      [:div.network-detail
       [:h1 (get-in data [:network :network/name])]
       [:p (str "Productions: " (count (:productions data)))]
       [:p (str "Capacite installee: " (:total-capacity-kwc data) " kWc")]
       [:p (str "Consommateurs: " (:consumer-count data))]]

      :else
      [:div.network-detail [:p "Aucune donnee."]])))
