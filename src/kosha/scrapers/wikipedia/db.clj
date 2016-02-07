(ns kosha.scrapers.wikipedia.db
  (:require [clojure.java.jdbc :as j]
            [kosha.db.pool :as db-pool])
  (:import [java.sql SQLException]))

(defn insert-scales [raga-name arohanams avarohanams]
  (doall
   (map (fn [arohanam avarohanam]
          (j/insert! db-pool/conn
                     :wikipedia_scales
                     {:raga_name raga-name
                      :arohanam arohanam
                      :avarohanam avarohanam}))
        arohanams avarohanams)))

(defn insert-raga [{:keys [raga-link mela-number melakartha
                           raga-name arohanams avarohanams] :as raga}]
  (j/insert! db-pool/conn
             :wikipedia_ragas
             {:raga_name raga-name
              :melakartha melakartha
              :raga_link raga-link
              :mela_number mela-number})
  (insert-scales raga-name arohanams avarohanams))
