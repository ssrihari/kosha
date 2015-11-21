(ns kosha.scrapers.karnatik
  (:require [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as m]
            [clj-http.client :as client]
            [kosha.scrapers.util :as u])
  (:import org.jsoup.Jsoup
           [org.jsoup.nodes Document Element]
           org.jsoup.select.Elements))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; scrape list of kritis

(defn scrape-kriti-list [^Document doc]
  (->> (.select doc "option + option")
       (map #(.attr % "value"))
       (map #(s/replace % #"\\" ""))
       (map #(s/replace % #"\"" ""))
       (filter #(re-find #"c\d+.*" %))))

(defn build-kriti-url [relative-url]
  (str "http://www.karnatik.com/" relative-url))

(defn all-kriti-urls []
  (let [url "http://www.karnatik.com/lyrics.shtml"
        doc (u/url->jsoup url)]
    (->> doc
         scrape-kriti-list
         (map build-kriti-url))))

(comment
  (def kriti-urls (doall (all-kriti-urls))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; scrape single kriti

(defn kriti-name [^Elements elems]
  (->> (.select elems "b")
       (map #(.text %))
       (m/find-first #(re-find #"Song:" %))
       (u/replace-str #"Song:" "")
       u/clean-str
       s/lower-case))

(defn raga-name [^Elements elems]
  (->> (.select elems "a")
       (m/find-first #(re-find #"ragas" (.attr % "href")))
       .text
       u/clean-str
       s/lower-case))

(defn get-prop [^Elements elems prop-name]
  (let [find-pat (re-pattern (str prop-name ": \\w+"))
        remove-pat (re-pattern (str prop-name ": "))]
    (->> (.select elems "p")
         (map #(.text %))
         (m/find-first #(re-find find-pat %))
         (re-seq find-pat)
         first
         (u/replace-str remove-pat "")
         u/clean-str
         s/lower-case)))

(defn language [elems]
  (get-prop elems "Language"))

(defn taalam [elems]
  (get-prop elems "taaLam"))

(defn composer [elems]
  (get-prop elems "Composer"))

(defn lyrics [elems]
  (let [main-text (.select elems "font font > p")
        pallavi-index (u/find-first-match main-text "p" "pallavi")
        meaning-index (u/find-first-match main-text "p" "meaning:")
        until-index (if (= -1 meaning-index)
                      (count main-text)
                      meaning-index)]
    (->> (.subList main-text pallavi-index until-index)
         (map #(.html %))
         (map u/clean-str)
         (remove s/blank?))))

(defn meaning [elems]
  (let [meaning-index (u/find-first-match elems "p" "meaning:")
        other-info-index (u/find-first-match elems "p" "other information:")
        bottom-index (u/find-first-match elems "center" "first|previous|next")
        until-index (if (= -1 other-info-index)
                      bottom-index
                      (dec other-info-index))]
    (when-not (= -1 meaning-index)
      (->> (.subList elems meaning-index until-index)
           (map #(.html %))
           (remove #{"Meaning:"})
           (map u/clean-str)
           (remove s/blank?)))))

(defn scrape-kriti-page [^Document doc url]
  (let [main-content (.select doc "*")]
    {:kriti    (kriti-name main-content)
     :ragam    (raga-name main-content)
     :composer (composer main-content)
     :language (language main-content)
     :taalam   (taalam main-content)
     :lyrics   (lyrics main-content)
     :meaning  (meaning main-content)
     :url      url}))

(defn kriti-details [kriti-url]
  (try
    (prn "fetching " kriti-url)
    (let [doc (u/url->jsoup kriti-url)]
      (scrape-kriti-page doc kriti-url))
    (catch Exception e
      (prn "Caught exception when parsing " kriti-url))))

(comment
  ;; TODO: convert these to tests
  ;; cases covered in lyrics and meaning
  (kriti-details "http://www.karnatik.com/c1373.shtml") ;; p+a+c with meanings
  (kriti-details "http://www.karnatik.com/c2254.shtml") ;; multiple charanams
  (kriti-details "http://www.karnatik.com/c1726.shtml") ;; p+c multiple meaning paras
  (kriti-details "http://www.karnatik.com/c1811.shtml") ;; no meaning
  )
