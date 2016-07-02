(ns kosha.app.search
  (:require [kosha.app.view :as view]
            [kosha.db.search :as db-search]))

(defn ragam [{:keys [params] :as request}]
  (def *r request)
  (let [ragams (db-search/ragams (:q params)
                                 (or (and (:n params) (Integer/parseInt (:n params))) 6))]
    {:status 200
     :body (view/html-skeleton
            [:table
             [:thead [:th "Ragam"] [:th "Arohanam"] [:th "Avarohanam"]]
             [:tbody
              (for [{:keys [ragam-name arohanam avarohanam]} ragams]
                [:tr
                 [:td ragam-name]
                 [:td arohanam]
                 [:td avarohanam]])]])}))
