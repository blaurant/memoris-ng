(ns app.productions.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :productions/list
  (fn [db _] (:productions/list db)))

(rf/reg-sub :productions/loading?
  (fn [db _] (:productions/loading? db)))
