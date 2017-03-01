(ns kosha.app.search
  (:require [kosha.app.view :as view]
            [kosha.db.search :as db-search]
            [kosha.app.util :as util]
            [clojure.string :as s]))

(defn external-links [{:keys [k-ragams s-ragams]}
                      {:keys [raga-link melakartha mela-number]}]
  [:section.inline
   (if-not (s/blank? raga-link)
     [:span.external-link [:a {:href (str "https://en.wikipedia.org" raga-link) :target "blank"} "Wiki"]])

   (if-let [kragam (first k-ragams)]
     (let [link (format "http://www.karnatik.com/ragas%s.shtml#%s" (first kragam) kragam)]
       [:span.external-link [:a {:href link :target "blank"} "Karnatik"]]))

   (if-let [sragam (first s-ragams)]
     (let [link "http://www.sangeethapriya.org/fetch_tracks.php?ragam"]
       [:form.inline.external-link {:action link :method "POST" :target "blank"}
        [:input {:type :hidden :name "FIELD_TYPE" :value sragam}]
        [:input {:type :submit :name "Sangeethapriya" :value "Sangeethapriya"}]]))])

(defn mela-data [{:keys [melakartha mela-number] :as metadata}]
  (if melakartha
    (format "This is mela %s" mela-number)
    (format "Janya of %s" mela-number)))

(defn ragam [{:keys [params] :as request}]
  (let [ragams (db-search/ragams-with-scales (:q params) 10)]
    {:status 200
     :body (view/html-skeleton
            [:table
             [:thead [:th "Ragam"] [:th "Arohanam"] [:th "Avarohanam"] [:th "Mela"] [:th "Links"]]
             [:tbody
              (for [{:keys [ragam-name arohanam avarohanam metadata] :as ragam} ragams]
                [:tr
                 [:td (util/unaccent ragam-name)]
                 [:td arohanam]
                 [:td avarohanam]
                 [:td (mela-data metadata)]
                 [:td (external-links ragam metadata)]])]])}))
