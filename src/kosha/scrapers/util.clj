(ns kosha.scrapers.util
  (:require [clojure.string :as s]
            [clj-http.client :as client]
            [medley.core :as m])
  (:import org.jsoup.Jsoup
           [org.jsoup.nodes Document Element]
           org.jsoup.select.Elements))

(defn clean-str [s]
  (-> s
      (s/replace #"\\n" "")
      (s/replace #"\\r" "")
      (s/trim)))

(defn replace-str
  "Version of string/replace where s is the last argument.
  Used when threading last."
  [match replacement s]
  (s/replace s match replacement))

(defn find-first-match [^Elements elems tagname match]
  (let [match-pat (re-pattern (str "(?i)" match))]
    (->> (.select elems tagname)
         (m/find-first #(re-find match-pat (.text %)))
         (.indexOf elems))))

(defn response->jsoup [response]
  (-> response
      :body
      Jsoup/parse))

(defn url->jsoup [url]
  (-> url
      client/get
      response->jsoup))
