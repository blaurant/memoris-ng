(ns app.components.password-input
  (:require [reagent.core :as r]))

(defn password-input
  "Password input with show/hide toggle eye icon.
   Props: same as a regular input (:placeholder, :value, :on-change, etc.)"
  [props]
  (let [visible? (r/atom false)]
    (fn [props]
      [:div {:style {:position "relative" :display "inline-block" :width "100%"}}
       [:input.onboarding__input
        (merge (dissoc props :style)
               {:type  (if @visible? "text" "password")
                :style (merge {:padding-right "2.5rem" :width "100%" :box-sizing "border-box"}
                              (:style props))})]
       [:button {:on-click  #(swap! visible? not)
                 :type      "button"
                 :tab-index -1
                 :style     {:position "absolute" :right "10px" :top "0" :bottom "0"
                            :background "transparent" :border "none" :cursor "pointer"
                            :padding "0" :display "flex" :align-items "center"
                            :color "var(--color-muted)"}}
        (if @visible?
          ;; Eye-off icon
          [:svg {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
                 :stroke "currentColor" :stroke-width "2"
                 :stroke-linecap "round" :stroke-linejoin "round"}
           [:path {:d "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"}]
           [:path {:d "M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"}]
           [:path {:d "M14.12 14.12a3 3 0 1 1-4.24-4.24"}]
           [:line {:x1 "1" :y1 "1" :x2 "23" :y2 "23"}]]
          ;; Eye icon
          [:svg {:width "18" :height "18" :viewBox "0 0 24 24" :fill "none"
                 :stroke "currentColor" :stroke-width "2"
                 :stroke-linecap "round" :stroke-linejoin "round"}
           [:path {:d "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"}]
           [:circle {:cx "12" :cy "12" :r "3"}]])]])))
