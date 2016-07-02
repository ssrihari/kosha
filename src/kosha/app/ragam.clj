(ns kosha.app.ragam
  (:require [kosha.app.view :as view]
            [kosha.db.search :as db-search]))

(defn show [{:keys [params] :as request}]
  (let [ragam (first (db-search/ragams (:name params)))]
    (view/html-skeleton [])))
