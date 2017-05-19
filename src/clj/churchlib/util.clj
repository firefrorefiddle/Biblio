(ns churchlib.util
  (:require [churchlib.db :refer [db-uri]]
            [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import datomic.Util))

(defn read-all [f]
  (Util/readAll (io/reader f)))

(defn transact-all [conn f]
  (doseq [txd (read-all f)]
    (d/transact conn txd))
  :done)

(defn create-db []
  (d/create-database db-uri))

(defn get-conn []
  (d/connect db-uri))

(defn load-schema []
  (transact-all (get-conn) (io/resource "data/schema.edn")))

(defn init-db []
  (create-db)
  (load-schema))
