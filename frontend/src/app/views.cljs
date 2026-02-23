(ns app.views
  (:require [re-frame.core :as rf]))

(defn hello-button []
  (let [message  @(rf/subscribe [:hello/message])
        loading? @(rf/subscribe [:hello/loading?])]
    [:div
     [:button {:on-click #(rf/dispatch [:hello/fetch])
               :disabled loading?}
      "Hello World"]
     (when message
       [:p message])]))

(defn main-panel []
  [:div
   [hello-button]])
