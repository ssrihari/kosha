(ns kosha.scrapers.karnatik.kriti
  (:require [clojure.string :as s]
            [kosha.scrapers.util :as u]
            [medley.core :as m])
  (:import org.jsoup.Jsoup
           [org.jsoup.nodes Document Element]
           org.jsoup.select.Elements))

(defn kriti-name [^Elements elems]
  (->> (.select elems "b")
       (map #(.text %))
       (m/find-first #(re-find #"Song:" %))
       (u/replace-str #"Song:" "")
       u/clean-str
       s/lower-case))

(defn remove-prop-names [prop-value]
  (when prop-value
    (u/replace-str #"(?i)language|composer|click to view in| (- click to listen).*|pallavi"
                   ""
                   prop-value)))

(defn get-prop [^Elements elems prop-name]
  (let [find-pat (re-pattern (str prop-name ": [\\w. ]+"))
        remove-pat (re-pattern (str prop-name ": "))]
    (some->> (.select elems "p")
             (map #(.text %))
             (m/find-first #(re-find find-pat %))
             (re-seq find-pat)
             first
             (u/replace-str remove-pat "")
             remove-prop-names
             u/clean-str
             s/lower-case
             u/nil-if-blank)))

(defn raga-name [^Elements elems]
  (or
   (some->> (.select elems "a")
            (m/find-first #(re-find #"ragas" (.attr % "href")))
            .text
            u/clean-str
            s/lower-case)
   (get-prop elems "raagam")))

(defn language [elems]
  (get-prop elems "Language"))

(defn taalam [elems]
  (get-prop elems "taaLam"))

(defn composer [elems]
  (or (some->> (.select elems "a")
               (m/find-first #(re-find #"co\d\d\d\d.shtml" (.attr % "href")))
               .text)
      (get-prop elems "Composer")))

(defn lyrics [elems kriti-url]
  (let [main-text (.select elems "font font > p")
        pallavi-index (u/find-first-match main-text "p" "pallavi")
        language-index (when-let [li (u/find-first-match main-text "p" "language")] (inc li))
        notation-index (u/find-first-match main-text "p" "notation:")
        meaning-index (u/find-first-match main-text "p" "meaning:")
        tamil-transliteration (u/find-first-match main-text "p" "transliteration:")
        other-info-index (u/find-first-match main-text "p" "other information:")
        start-index (->> [pallavi-index
                          language-index]
                         (remove nil?)
                         (apply min))
        until-index (->> [notation-index
                          meaning-index
                          other-info-index
                          tamil-transliteration
                          (count main-text)]
                         (remove nil?)
                         (apply min))
        lyrics-plist (->> (.subList main-text start-index until-index)
                          (map #(.html %))
                          (map u/clean-str)
                          (remove s/blank?))
        has-named-stanzas? (even? (count lyrics-plist))]
    (u/log :INFO :scrape {:has-named-stanzas has-named-stanzas? :kriti-url kriti-url})
    (if has-named-stanzas?
      {:has-named-stanzas true :content (apply sorted-map lyrics-plist)}
      {:has-named-stanzas false :content lyrics-plist})))

(defn meaning [elems]
  (let [meaning-index (u/find-first-match elems "p" "meaning:")
        other-info-index (u/find-first-match elems "p" "other information:")
        bottom-index (u/find-first-match elems "center" "first|previous|next")
        until-index (if other-info-index
                      (dec other-info-index)
                      bottom-index)]
    (when meaning-index
      (->> (.subList elems meaning-index until-index)
           (map #(.html %))
           (remove #{"Meaning:"})
           (map u/clean-str)
           (remove s/blank?)
           (s/join " ")))))

(defn notation [elems]
  (some->> (.select elems "p")
           (m/find-first #(re-find #"(?i)notation:" (.text %)))
           (.html)
           (u/replace-str #"<b>Notation:</b><br>" "")
           u/clean-str
           u/nil-if-blank))

(defn scrape-kriti-page [^Document doc url]
  (let [main-content (.select doc "*")]
    {:kriti    (kriti-name main-content)
     :ragam    (raga-name main-content)
     :composer (composer main-content)
     :language (language main-content)
     :taalam   (taalam main-content)
     :lyrics   (try
                 (lyrics main-content url)
                 (catch clojure.lang.ArityException e
                   (u/log :WARN :scrape {:kriti-url url :msg "Unable to get lyrics"})))
     :meaning  (meaning main-content)
     :notation (notation main-content)
     :url      url}))

(defn get-kriti-details [kriti-url]
  (try
    (let [[exec-time doc] (u/with-time #(u/url->jsoup kriti-url))]
      (u/log :INFO :fetch {:kriti-url kriti-url :exec-time exec-time})
      (scrape-kriti-page doc kriti-url))
    (catch Exception e
      (def *ex e)
      (.printStackTrace e)
      (u/log :ERROR :fetch-and-scrape {:kriti-url kriti-url}))))
