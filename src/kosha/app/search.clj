(ns kosha.app.search
  (:require [kosha.app.view :as view]
            [kosha.db.search :as db-search]
            [kosha.app.util :as util]
            [clojure.string :as s]))

(defn show-metadata [{:keys [k-ragams s-ragams]}
                     {:keys [raga-link melakartha mela-number]}]
  [:div
   [:span (if melakartha
            (format "This is mela number %s" mela-number)
            (format "Janya of mela %s" mela-number))]
   (if-not (s/blank? raga-link)
     [:span [:span " | "]
      [:span [:a {:href (str "https://en.wikipedia.org" raga-link) :target "blank"} "Wiki"]]])
   (if-let [kragam (first k-ragams)]
     (let [link (format "http://www.karnatik.com/ragas%s.shtml#%s" (first kragam) kragam)]
       [:span [:span " | "]
        [:span [:a {:href link :target "blank"} "Karnatik"]]]))
   (if-let [sragam (first s-ragams)]
     (let [link "http://www.sangeethapriya.org/fetch_tracks.php?ragam"]
       [:span [:span " | "]
        [:form.inline {:action link :method "POST"}
         [:input {:type :hidden :name "FIELD_TYPE" :value sragam}]
         [:input {:type :submit :name "Sangeethapriya" :value "Sangeethapriya"}]]]))])

(defn ragam [{:keys [params] :as request}]
  (let [ragams (db-search/ragams-with-scales (:q params) 6)]
    {:status 200
     :body (view/html-skeleton
            [:table
             [:thead [:th "Ragam"] [:th "Arohanam"] [:th "Avarohanam"]]
             [:tbody
              (for [{:keys [ragam-name arohanam avarohanam metadata] :as ragam} ragams]
                [:tr
                 [:td (util/unaccent ragam-name)]
                 [:td arohanam]
                 [:td avarohanam]
                 [:td (show-metadata ragam metadata)]])]])}))
