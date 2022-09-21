(ns qniform.accounting
  (:require [malli.core :as m]
            [qniform.db :as db]
            [qniform.time :as t]))

(def spec-new-journal-entry
  (m/schema
   [:map
    [:event-id :string]
    [:dr-cr [:fn (fn [x] (#{"credit" "debit"} x))]]
    [:je-type [:fn (fn [x] (= "new" x))]]
    [:currency [:string {:min 3 :max 3}]]
    [:local-amount :double]
    [:effective-date [:re #"\d{4}-\d{2}-\d{2}"]]
    [:knowledge-datetime [:re #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}"]]]))

(def spec-correct-journal-entry
  (m/schema
   [:map
    [:event-id :string]
    [:dr-cr [:fn (fn [x] (#{"credit" "debit"} x))]]
    [:je-type [:fn (fn [x] (= "correct" x))]]
    [:update-of :int]
    [:currency [:string {:min 3 :max 3}]]
    [:local-amount :double]
    [:effective-date [:re #"\d{4}-\d{2}-\d{2}"]]
    [:knowledge-datetime [:re #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}"]]]))

(def spec-nullify-journal-entry
  (m/schema [:map
             [:je-type [:fn (fn [x] (= "nullify" x))]]
             [:update-of :int]
             [:knowledge-datetime [:re #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}"]]]))

(defn book-journal-entries! [jes]
  (when (every? #(m/validate [:or spec-new-journal-entry
                              spec-correct-journal-entry
                              spec-nullify-journal-entry] %) jes)
    ;; should return something sensible here, not pass through DB return
    (db/book-journal-entries! jes)))

(defn filter-on-datetime [datetime jes]
  (remove #(t/date> (:knowledge-datetime %) datetime) jes))

(defn aggregate-je
  "given a set of jes and a datetime, will return the set of JEs representing the known
   state of the system as at that datetime."
  ([jes]
   (vals (reduce (fn [A entry]
                   (case (:je-type entry)
                     "new" (assoc A (:id entry) (assoc entry :corrections []))
                     "correct" (update A (:update-of entry) (update entry :corrections conj (:update-of entry)))
                     "nullify" (dissoc A (:update-of entry))))
                 {}
                 jes)))
  ([jes datetime]
   (aggregate-je (filter-on-datetime datetime jes))))

(defn- tb-signed->drcr [tb]
  (update-vals tb #(if (neg? %) [:cr (abs %)] [:dr %])))

(defn trial-balance [jes]
  (tb-signed->drcr (reduce (fn [A entry]
                             (update A (:account entry)
                                     (if (= "debit" (:dr-cr entry)) + -)
                                     (:local-amount entry)))
                           (zipmap (set (map :account jes)) (repeat 0))
                           jes)))

(comment
  (book-journal-entries! [{:id 1
                           :event-id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
                           :dr-cr "credit",
                           :je-type "new"
                           :account "share-capital",
                           :currency "USD",
                           :local-amount 1223.0
                           :effective-date "2022-09-18"
                           :knowledge-datetime "2022-09-18 10:14:46.123"}
                          {:id 2
                           :event-id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
                           :dr-cr "debit",
                           :je-type "new"
                           :account "cash",
                           :currency "USD",
                           :local-amount 1223.0
                           :effective-date "2022-09-18"
                           :knowledge-datetime "2022-09-18 10:14:46.123"}])

  (book-journal-entries! [{:event-id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
                           :dr-cr "credit",
                           :je-type "new"
                           :account "share-capital",
                           :currency "USD",
                           :local-amount 1223.0
                           :effective-date "2022-09-18"
                           :knowledge-datetime "2022-09-18 10:14:46.123"}
                          {:event-id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
                           :dr-cr "debit",
                           :je-type "new"
                           :account "cash",
                           :currency "USD",
                           :local-amount 1223.0
                           :effective-date "2022-09-18"
                           :knowledge-datetime "2022-09-18 10:14:46.123"}]))