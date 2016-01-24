(ns kosha.scrapers.sangeethapriya.raga-names
  (:require [clojure.string :as s]
            [kosha.scrapers.util :as u])
  (:import [org.jsoup.nodes Document]))

(defn get-all-raga-names []
  (prn "fetching all raga names")
  (let [^Document doc (u/url->jsoup "http://www.sangeethapriya.org/display_tracks.php")
        options (.select doc "form:nth-child(5) .full option")]
    (->> options
         (mapv #(.attr % "value"))
         (remove s/blank?))))
