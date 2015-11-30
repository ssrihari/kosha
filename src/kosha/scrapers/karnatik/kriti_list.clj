(ns kosha.scrapers.karnatik.kriti-list
  (:require [clojure.string :as s]
            [kosha.scrapers.util :as u])
  (:import org.jsoup.Jsoup
           [org.jsoup.nodes Document Element]
           org.jsoup.select.Elements))

(defn scrape-kriti-list [^Document doc]
  (->> (.select doc "option + option")
       (map #(.attr % "value"))
       (map #(s/replace % #"\\" ""))
       (map #(s/replace % #"\"" ""))
       (filter #(re-find #"c\d+.*" %))))

(defn build-kriti-url [relative-url]
  (str "http://www.karnatik.com/" relative-url))

(defn all-kriti-urls []
  (u/log :INFO :fetch-all-urls {})
  (let [url "http://www.karnatik.com/lyrics.shtml"
        doc (u/url->jsoup url)]
    (->> doc
         scrape-kriti-list
         (map build-kriti-url))))
