(ns kosha.scrapers.util
  (:require [clojure.string :as s]
            [clj-time.core :as t]
            [clj-http.client :as client]
            [medley.core :as m])
  (:import org.jsoup.Jsoup
           [org.jsoup.nodes Document Element]
           org.jsoup.select.Elements))

(def log-file "logs/scrapers.log")

(defn init-log! []
  (spit log-file ""))

(defn log [level stage details]
  (let [log-line (-> {:level level
                      :stage stage}
                     (merge details)
                     (str "\n"))]
    (spit log-file log-line :append true)))

(defn with-time [f]
  (let [start-time (t/now)
        result (f)
        exec-time (t/in-millis (t/interval start-time (t/now)))]
    [exec-time result]))

(defn clean-str [s]
  (-> s
      (s/replace #"\\n" "")
      (s/replace #"\\r" "")
      (s/trim)))

(defn nil-if-blank [input-str]
  (if (s/blank? input-str)
    nil
    input-str))

(defn replace-str
  "Version of string/replace where s is the last argument.
  Used when threading last."
  [match replacement s]
  (s/replace s match replacement))

(defn find-first-match [^Elements elems tagname match]
  (let [match-pat (re-pattern (str "(?i)" match))
        index (->> (.select elems tagname)
                   (m/find-first #(re-find match-pat (.text %)))
                   (.indexOf elems))]
    (if (= -1 index)
      nil
      index)))

(defn response->jsoup [response]
  (-> response
      :body
      Jsoup/parse))

(defn url->jsoup [url]
  (-> url
      client/get
      response->jsoup))
