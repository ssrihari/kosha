(ns kosha.scrapers.karnatik
  (:require [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [kosha.scrapers.karnatik.kriti-list :as kl]
            [kosha.scrapers.karnatik.kriti :as kk]
            [kosha.scrapers.util :as u]))

(def output-filename "output/karnatik.edn")

(defn scrape-and-save-all-kritis []
  (u/init-log!)
  (u/log :INFO :save {:filename output-filename})
  (spit output-filename "[")
  (doall
   (pmap (fn [kriti-url]
           (let [[exec-time kriti-details] (u/with-time #(kk/get-kriti-details kriti-url))
                 pretty-kriti-details (-> kriti-details pp/pprint with-out-str)]
             (do
               (spit output-filename pretty-kriti-details :append true)
               (u/log :INFO :ssave {:kriti-url kriti-url :total-exec-time exec-time}))))
         (kl/all-kriti-urls)))
  (spit output-filename "]" :append true)
  (u/log :INFO :done {}))

(defn read-output []
  (->> output-filename
       slurp
       edn/read-string))

(comment

  ;; To run this:
  (time (scrape-and-save-all-kritis))

  ;; TODO: convert these to tests
  ;; cases covered in lyrics and meaning
  (get-kriti-details "http://www.karnatik.com/c1373.shtml") ;; p+a+c with meanings
  (get-kriti-details "http://www.karnatik.com/c2254.shtml") ;; multiple charanams
  (get-kriti-details "http://www.karnatik.com/c1726.shtml") ;; p+c multiple meaning paras
  (get-kriti-details "http://www.karnatik.com/c1811.shtml") ;; no meaning
  )
