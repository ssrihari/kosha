(ns kosha.scrapers.wikipedia
  (:require [kosha.scrapers.util :as u]
            [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [kosha.scrapers.wikipedia.db :as db])
  (:import [org.jsoup.nodes Document]))


(def output-filename "output/wikipedia.edn")
(defonce current-mela-number (atom 0))

(defn row->raga [row]
  (let [[rname arohanam avarohanam] (.select row "td")
        mela-number (some-> rname (.select "span b") first .ownText Integer.)
        melakartha? (boolean mela-number)]
    (when melakartha?
      (swap! current-mela-number (constantly mela-number)))
    {:raga-link (.attr (.select rname "a") "href")
     :mela-number @current-mela-number
     :melakartha melakartha?
     :raga-name (or (some-> rname (.select "b a") first .text)
                    (.text rname))
     :arohanams (s/split (.html arohanam) #"<br> ")
     :avarohanams (s/split (.html avarohanam) #"<br> ")}))

(defn scrape-ragas []
  (u/init-log!)
  (u/log :INFO :save {:filename output-filename})
  (spit output-filename "[")
  (doall
   (let [^Document doc (u/url->jsoup "https://en.wikipedia.org/wiki/List_of_Janya_ragas")
         raga-rows (rest (.select doc ".wikitable tr"))]
     (->> raga-rows
          (map row->raga)
          (map #(-> % pp/pprint with-out-str))
          (map #(spit output-filename % :append true)))))
  (spit output-filename "]" :append true)
  (u/log :INFO :done {}))

(defn read-ragas []
  (->> output-filename
       slurp
       edn/read-string))

(defn save-to-db []
  (doall
   (pmap db/insert-raga (read-ragas))))

(comment
  (scrape-ragas)
  (save-to-db))
