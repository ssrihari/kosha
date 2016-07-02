(ns kosha.db
  (:require [clojure.java.jdbc :as j])
  (:import java.sql.Connection
           org.postgresql.jdbc4.Jdbc4Array))

(defn read-array [^org.postgresql.jdbc4.Jdbc4Array jdbc-4-array]
  (when jdbc-4-array
    (into [] (.getArray jdbc-4-array))))

(defn create-array [db-conn coll]
  (j/with-db-connection [conn db-conn]
    (.createArrayOf ^Connection (:connection conn) "varchar" (into-array String coll))))
