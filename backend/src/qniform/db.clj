(ns qniform.db
  (:require [clojure.set :refer [map-invert]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.result-set :as rs]))

(def db {:dbtype "sqlite" :dbname "qniform.db"})
(def ds (jdbc/get-datasource db))

;; setup and helper
(comment
  "table definitions"
  (jdbc/execute! ds ["create table journal_entries (
                      id integer primary key autoincrement,
                      event_id text,
                      account text,
                      dr_cr text,
                      currency text,
                      local_amount real,
                      effective_date text,
                      knowledge_datetime text)"])
  (jdbc/execute! ds ["drop table journal_entries"])

  (sql/insert! ds :journal_entries {:event_id "test"
                                    :account "test"
                                    :dr_cr "debit"
                                    :currency "USD"
                                    :local_amount 123.23
                                    :effective_date "2022-09-18"
                                    :knowledge_datetime "2022-09-18 10:14:46.123"})
  (sql/query ds ["select * from journal_entries"]
             {:builder-fn rs/as-unqualified-maps})
  (jdbc/execute! ds ["delete from journal_entries"]))

;; Tranformations between DB schema and domain models
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def journal-trans
  {:event-id :event_id
   :dr-cr :dr_cr
   :local-amount :local_amount
   :effective-date :effective_date
   :knowledge-datetime :knowledge_datetime})

(defn domain->db [key-map domain-trans]
  (update-keys domain-trans #(or (key-map %) %)))

(defn db->domain [key-map domain-trans]
  (update-keys domain-trans #(or ((map-invert key-map) %) %)))

;; Recording
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn book-journal-entries! [jes]
  (sql/insert-multi! ds :journal_entries (map #(domain->db journal-trans %) jes)))

(defn get-journal-entries! []
  (map #(db->domain journal-trans %)
       (sql/query ds ["select * from journal_entries"]
                  {:builder-fn rs/as-unqualified-maps})))

(comment
  (book-journal-entries! [{:event-id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
                           :dr-cr "credit",
                           :account "share-capital",
                           :currency "USD",
                           :local-amount 1223.0
                           :effective-date "2022-09-18"
                           :knowledge-datetime "2022-09-18 10:14:46.123"}
                          {:event-id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
                           :dr-cr "debit",
                           :account "cash",
                           :currency "USD",
                           :local-amount 1223.0
                           :effective-date "2022-09-18"
                           :knowledge-datetime "2022-09-18 10:14:46.123"}])

  (get-journal-entries!))