(ns kosha.db.pool
  (:require [hikari-cp.core :as hikari]
            [clojure.java.jdbc :as jdbc]))

(def datasource-options {:auto-commit        true
                         :read-only          false
                         :connection-timeout 30000
                         :validation-timeout 5000
                         :idle-timeout       600000
                         :max-lifetime       1800000
                         :minimum-idle       10
                         :maximum-pool-size  10
                         :pool-name          "db-pool"
                         :adapter            "postgresql"
                         :username           "sriharisriraman"
                         ;;:password           "password"
                         :database-name      "kosha"
                         :server-name        "localhost"
                         :port-number        5432
                         :register-mbeans    false})

(defonce conn
  {:datasource (hikari/make-datasource datasource-options)})
