(ns kosha.scrapers.karnatik.db
  (:require [clojure.java.jdbc :as j]
            [pg-hstore.core :as hs])
  (:import [java.sql SQLException]))

(def conn {:subprotocol "postgresql"
           :subname "//localhost/kosha"
           :user "sriharisriraman"})

(defn insert-row [{:keys [lyrics] :as kriti-details}]
  (def *kd kriti-details)
  (try
    (cond
     (true? (:has-named-stanzas lyrics))
      (-> kriti-details
          (update-in [:lyrics :content] hs/to-hstore)
          (update :lyrics hs/to-hstore)
          (->> (j/insert! conn :karnatik_data))
          first
          (update :lyrics hs/from-hstore)
          (update-in [:lyrics :content] hs/from-hstore))
      (false? (:has-named-stanzas lyrics))
      (-> kriti-details
          (update :lyrics hs/to-hstore)
          (->> (j/insert! conn :karnatik_data))
          first
          (update :lyrics hs/from-hstore)
          (update-in [:lyrics :content] read-string))
      (nil? lyrics)
      (-> kriti-details
          (->> (j/insert! conn :karnatik_data))
          first))
    (catch SQLException e
      (.printStackTrace e)
      (prn {:error "unable to save kriti to DB." :kriti-details kriti-details}))
    (catch Exception e
      (prn class e)
      (prn kriti-details)
      (throw e))))
