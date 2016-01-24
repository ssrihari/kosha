(ns kosha.scrapers.sangeethapriya
  (:require [kosha.scrapers.sangeethapriya.raga-names :as raga-names]
            [kosha.scrapers.sangeethapriya.kriti :as kriti]
            [kosha.scrapers.sangeethapriya.db :as db]
            [kosha.scrapers.util :as u]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]))

(def output-filename "output/sangeethapriya.edn")

(defn scrape-renditions []
  (u/init-log!)
  (u/log :INFO :save {:filename output-filename})
  (spit output-filename "[")
  (doall
   (map (fn [ragam]
          (u/log :INFO :fetch {:ragam ragam})
          (let [[exec-time renditions] (u/with-time #(kriti/renditions-in-ragam ragam))]
            (u/log :INFO :ssave {:ragam ragam :total-exec-time exec-time})
            (doall
             (for [rendition-detail renditions
                   :let [pretty-rendition-details (-> rendition-detail pp/pprint with-out-str)]]
               (spit output-filename pretty-rendition-details :append true)))))
        (raga-names/get-all-raga-names)))
  (spit output-filename "]" :append true)
  (u/log :INFO :done {}))

(defn read-renditions []
  (->> output-filename
       slurp
       edn/read-string))

(def tracks-output-filename "output/sangeethapriya-tracks.edn")

(defn concerts []
  (->> (read-renditions)
       (group-by #(select-keys % [:concert-url :concert-id]))
       keys))

(defn scrape-rendition-urls []
  (u/init-log!)
  (u/log :INFO :save {:filename tracks-output-filename})
  (spit tracks-output-filename "[")
  (doall
   (pmap (fn [concert]
           (u/log :INFO :fetch {:concert-id (:concert-id concert)
                                :concert-url (:concert-url concert)})
           (let [[exec-time renditions] (u/with-time #(kriti/fetch-rendition-urls concert))]
             (u/log :INFO :ssave {:exec-time exec-time :concert-id (:concert-id concert)})
             (doall
              (for [rendition-detail renditions
                    :let [pretty-rendition-details (-> rendition-detail pp/pprint with-out-str)]]
                (spit tracks-output-filename pretty-rendition-details :append true)))))
         (concerts)))
  (spit tracks-output-filename "]" :append true)
  (u/log :INFO :done {}))

(defn read-tracks []
  (->> tracks-output-filename
       slurp
       edn/read-string))

(comment
  (pmap db/insert-track (read-tracks))
  (pmap db/insert-rendition (read-renditions)))
