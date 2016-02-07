(ns kosha.scrapers.sangeethapriya.db
  (:require [clojure.java.jdbc :as j]
            [kosha.db.pool :as db-pool])
  (:import [java.sql SQLException]))

(defn insert-track [{:keys [concert-id concer-url track-number track-url] :as track}]
  (j/insert! db-pool/conn
             :sangeethapriya_tracks
             {:concert_id concert-id
              :concert_url concer-url
              :track_number track-number
              :track_url track-url}))

(defn insert-rendition [rendition]
  (j/insert! db-pool/conn
             :sangeethapriya_renditions
             {:concert_id (:concert-id rendition)
              :concert_url (:concert-url rendition)
              :track (:track rendition)
              :kriti (:kriti rendition)
              :ragam (:ragam rendition)
              :composer (:composer rendition)
              :main_artist (:main-artist rendition)}))
