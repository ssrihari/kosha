(ns kosha.app.util
  (:require [clojure.string :as s])
  (:import [java.util.concurrent
            ThreadPoolExecutor
            TimeUnit
            ArrayBlockingQueue
            ThreadPoolExecutor$CallerRunsPolicy]))

;; public String unaccent(String s) {
;;     String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
;;     return normalized.replaceAll("[^\\p{ASCII}]", "");
;; }

(defn unaccent [s]
  (let [normalized (java.text.Normalizer/normalize s java.text.Normalizer$Form/NFD)]
    (.replaceAll normalized "[^\\p{ASCII}]" "")))

(defn ->printable [swarams & {:keys [bold?]}]
  (s/join ", " (map (comp s/capitalize name) swarams)))

(defn display-ragam-name [ragam-name]
  (s/capitalize (name ragam-name)))

(defn create-fixed-threadpool [pool-size queue-size]
  (ThreadPoolExecutor. pool-size  ;corePoolSize
                       pool-size  ;maximumPoolSize
                       60
                       TimeUnit/SECONDS
                       (ArrayBlockingQueue. queue-size true)
                       ;; TODO: ask srihari why we chose this policy -nid/sd
                       (ThreadPoolExecutor$CallerRunsPolicy.)))

(defn run-in-parallel
  "Runs tasks parallelly using a fixed threadpool and a bounded ArrayBlockingQueue,
  where tasks are 0 argument functions."
  [tasks pool-size queue-size]
  (let [pool (create-fixed-threadpool pool-size queue-size)
        rets (mapv #(.get %)
                   (.invokeAll pool tasks))]
    (.shutdown pool)
    rets))
