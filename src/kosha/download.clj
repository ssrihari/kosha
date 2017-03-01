(ns kosha.download
  (:require [kosha.db.search :as db-search]
            [kosha.app.util :as util]
            [clojure.string :as s]
            [clj-http.client :as http]))

(defn download-track [url filename]
  (clojure.java.io/copy
   (:body (http/get url {:as :stream}))
   (java.io.File. filename)))

(defn file-name [rendition]
  (let [id (as-> [:ragam :main-artist :kriti :composer] x
                (map rendition x)
                (s/join "-" x)
                (s/replace x #"\s" "-")
                (str x (rand-int 100)))]
    (str "downloads/" id ".mpeg")))

(defn download-renditions [renditions]
  (let [tasks (for [{:keys [track-url] :as rendition} renditions]
                #(download-track track-url (file-name rendition)))]
    (util/run-in-parallel tasks 50 50)))

(defn filter-tracks [kriti-name ragam-id]
  (filter #(= ragam-id (:ragam-id %))
          (db-search/renditions kriti-name)))

(comment
  (->> "bhairavi"
       (db-search/renditions-in-ragam)
       (take 50)
       (download-renditions)))
