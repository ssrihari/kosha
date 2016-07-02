(ns kosha.db.search
  (:require [clojure.java.jdbc :as j]
            [clojure.set :as set]
            [kosha.db.pool :as db-pool]
            [kosha.db :as db]
            [pg-hstore.core :as hs]
            [medley.core :as m]))

(defn ->hyphens [^String x]
  (keyword (.replace x \_ \-)))

(defn read-ragam [ragam]
  (-> ragam
      (update :s-ragams db/read-array)
      (update :k-ragams db/read-array)))

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

(defn ragam-metadata [ragam-name]
  (let [q ["SELECT * FROM wikipedia_ragas WHERE raga_name = ?"
           ragam-name]]
    (->> (j/query db-pool/conn q :identifiers ->hyphens)
         first)))

(defn scales [rows]
  (->> rows
       (group-by :ragam-name)
       (m/map-vals (fn [rs] (map #(select-keys % [:arohanam :avarohanam]) rs)))))

(defn ragams [ragam n]
  (let [q1 ["SELECT * FROM std_ragams
             WHERE str_compare(?, ragam_name)
             OR str_compare_ar(?, s_ragams::varchar[])
             OR str_compare_ar(?, k_ragams::varchar[]);"
            ragam ragam, ragam]
        q2 ["SELECT *, similarity_score(ragam_name, ?) AS score
             FROM std_ragams
             ORDER BY score
             DESC LIMIT ?; "
            ragam n]]
    (->> (j/query db-pool/conn q1 :identifiers ->hyphens)
         (concat (j/query db-pool/conn q2 :identifiers ->hyphens))
         (map read-ragam)
         (m/distinct-by (juxt :ragam-name :arohanam :avarohanam)))))

(defn ragams-with-scales [ragam n]
  (let [result-ragams (ragams ragam n)
        ragam-scales (scales result-ragams)]
    (for [r (m/distinct-by :ragam-name result-ragams)]
      (assoc r
        :scales (get ragam-scales (:ragam-name r))
        :metadata (ragam-metadata (:ragam-name r))))))
