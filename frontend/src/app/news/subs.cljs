(ns app.news.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub :news/list
  (fn [db _] (:news/list db)))

(rf/reg-sub :news/loading?
  (fn [db _] (:news/loading? db)))
