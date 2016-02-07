(ns kosha.db.search
  (:require [kosha.db.pool :as db-pool]
            [clojure.java.jdbc :as j]
            [pg-hstore.core :as hs]))

(defn ->hyphens [^String x]
  (keyword (.replace x \_ \-)))

(defn read-kriti [kriti]
  (-> kriti
      (update :lyrics hs/from-hstore)
      (update-in [:lyrics :content] hs/from-hstore)))

(defn renditions [kriti]
  (let [q ["SELECT * FROM (
              SELECT *,
                (10 * similarity (kriti, ?)) +
                (10 - levenshtein (kriti, ?)) +
                (10 * (difference (kriti, ?) /4 )) AS similarity_score
              FROM sangeethapriya_renditions
              ORDER BY similarity_score DESC
              LIMIT 50) AS sr
            INNER JOIN sangeethapriya_tracks st
            ON sr.concert_id = st.concert_id
            AND sr.track = st.track_number; "
           kriti kriti kriti]]
    (j/query db-pool/conn q :identifiers ->hyphens)))

(defn kriti-details [kriti]
  (let [q ["SELECT *,
                (10 * similarity (kriti, ?)) +
                (10 - levenshtein (kriti, ?)) +
                (10 * (difference (kriti, ?) /4 )) AS similarity_score
              FROM karnatik_data
              ORDER BY similarity_score DESC
              LIMIT 50;"
           kriti kriti kriti]]
    (->> (j/query db-pool/conn q :identifiers ->hyphens)
         (map read-kriti))))

(defn kritis [kriti]
  (let [q ["SELECT distinct on (sang_kriti, similarity_score) sang_kriti, *,
               (10 * similarity (sang_kriti, ?)) +
               (10 - levenshtein (sang_kriti, ?)) +
               (10 * (difference (sang_kriti, ?) /4 )) AS similarity_score
            FROM adhoc_kritis
            ORDER BY similarity_score DESC
            LIMIT 50;"
           kriti kriti kriti]]
    (j/query db-pool/conn q :identifiers ->hyphens)))
