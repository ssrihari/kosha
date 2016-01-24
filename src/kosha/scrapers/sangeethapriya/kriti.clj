(ns kosha.scrapers.sangeethapriya.kriti
  (:require [clojure.string :as s]
            [medley.core :as m]
            [clj-http.client :as client]
            [kosha.scrapers.util :as u])
  (:import org.jsoup.Jsoup
           [org.jsoup.nodes Document Element]
           org.jsoup.select.Elements
           clojure.lang.ExceptionInfo))

(def cookies
  [["PHPSESSID" {:domain "www.sangeethamshare.org" :value "n1qr2vb2hqojq6vd5n217042n6"}]
   ["G_ENABLED_IDPS" {:domain ".www.sangeethamshare.org" :value "google"}]
   ["_ga" {:domain ".sangeethamshare.org" :value "GA1.2.1679019818.1435463904"}]
   ["_gat" {:domain ".sangeethamshare.org" :value "1"}]
   ["PHPSESSID" {:domain "www.sangeethapriya.org" :value "gap4p9r5r7h2cerslncsei1vr6"}]
   ["G_ENABLED_IDPS" {:domain ".www.sangeethapriya.org" :value "google"}]
   ["G_AUTHUSER_H" {:domain ".www.sangeethapriya.org" :value "0"}]
   ["_ga" {:domain ".sangeethapriya.org" :value "GA1.2.1035688716.1435464084"}]
   ["_gat" {:domain ".sangeethapriya.org" :value "1"}]])

(defn get-relative-urls [^Document doc]
  (keep (fn [track-link]
          (try
            (-> track-link
                (.attr "onmousedown")
                (s/split #",")
                (nth 2)
                (s/replace #"[\\\"]" ""))
            (catch IndexOutOfBoundsException e
              (u/log :ERROR :relative-url {:msg "index out of bounds"})
              nil)))
        (.select doc "a.download")))

(defn fetch-concert [concert-url]
  (try (->> {:cookies cookies}
            (client/get concert-url)
            u/response->jsoup)
       (catch Exception e
         (when (instance? ExceptionInfo e)
           (u/log :ERROR :relative-url (ex-data e)))
         nil)))

(defn fetch-rendition-urls [{:keys [concert-id concert-url] :as concert}]
  (try
    (when-let [^Document doc (fetch-concert concert-url)]
      (let [base-url (->> (.html (last (.select doc "head script")))
                          (u/split-str #"\n")
                          (filter #(re-find #"mirror1" %))
                          first
                          (re-find #"http://.*\?"))
            relative-urls (get-relative-urls doc)]
        (map-indexed
         (fn [i url]
           {:concert-id concert-id :concer-url concert-url
            :track-number (inc i) :track-url (str base-url url)})
         relative-urls)))
    (catch Exception e
      (u/log :ERROR :fetch-concert {:msg "error fetching concert" :concert-url concert-url}))))

(defn rendition-row [^Element row]
  (let [[concert-id track kriti ragam composer main-artist] (.select row "td")]
    {:concert-id (.text (.select concert-id "a"))
     :concert-url (.attr (.select concert-id "a") "href")
     :track (.text track)
     :kriti (.text kriti)
     :ragam (.text ragam)
     :composer (.text composer)
     :main-artist (.text main-artist)}))

(defn renditions-in-ragam [ragam]
  (try
    (let [ragam-post-url "http://www.sangeethapriya.org/fetch_tracks.php?ragam"
          ^Document doc (->> {:form-params {"FIELD_TYPE" ragam}}
                             (client/post ragam-post-url)
                             u/response->jsoup)]
      (for [row (drop 1 (.select doc "tr"))]
        (rendition-row row)))
    (catch Exception e
      (u/log :ERROR :fetch {:ragam :ragam})
      (.printStackTrace e)
      nil)))
