(ns qniform.frontend.rules
  (:require [meander.epsilon :as e]
            [malli.core :as m]))

(defn match-fn [pattern target]
  (list 'fn '[e] (list 'e/match 'e pattern target)))

(defn make-rule [{:keys [name schema pattern target]}]
  {:name name
   :schema (m/schema schema)
   :xform (match-fn pattern target)})

(def t '{:name "test", :schema [:map [:a :int] [:b :int]], :pattern {:a ?a, :b ?b}, :target {:new-a ?a, :new-b ?b}})

(def r (make-rule t))

(m/validate (:schema r) {:a 10 :b 15})
#_(eval (:xform r)) ;; Doesn't work! Need an alternative way
#_((:xform r) {:a 10 :b 15})

(defn round-2dp [num]
  (/ (Math/round (* 100 num)) 100))

(defn ccy-amnt? [num]
  (and (number? num)
       (= num (/ (Math/round (* 100 num)) 100))))

(def money (m/schema [:fn {:display "Money amount"
                           :coercion :double}
                      ccy-amnt?]))

(def share-issue-event
  (m/schema [:map
             [:event-id :string]
             [:shares :int]
             [:price-per-share money]
             [:comment :string]]))

(defn share-xform [event]
  (e/match {:event event}
    {:event {:event-id ?event-id
             :shares ?shares
             :price-per-share ?price-per-share
             :comment ?comment}}
    {:comment ?comment
     :journal-entries [{:event-id ?event-id
                        :dr-cr :credit
                        :account :share-capital
                        :currency "USD"
                        :local-amount (round-2dp (* ?price-per-share ?shares))}
                       {:event-id ?event-id
                        :dr-cr :debit
                        :account :cash
                        :currency "USD"
                        :local-amount (round-2dp (* ?price-per-share ?shares))}]}))

(def invoice-payable-event
  (m/schema [:map
             [:event-id :string]
             [:payee :string]
             [:amount money]
             [:comment :string]]))

(defn invoice-xform [event]
  (e/match {:event event}
    {:event {:event-id ?event-id
             :payee ?payee
             :amount ?amount
             :comment ?comment}}
    {:comment ?comment
     :journal-entries [{:event-id ?event-id
                        :dr-cr :credit
                        :account :invoices-payable
                        :currency "USD"
                        :local-amount ?amount}
                       {:event-id ?event-id
                        :dr-cr :debit
                        :account :expenses
                        :currency "USD"
                        :local-amount ?amount}]}))

(def rules {:share-issue     {:name "Share Issuance"
                              :schema share-issue-event
                              :xform share-xform}
            :invoice-payable {:name "Invoice payable"
                              :schema invoice-payable-event
                              :xform invoice-xform}})

(comment
  (def dummy-si-event {:event-id "hello"
                       :shares 123
                       :price-per-share 12.21})
  (share-xform dummy-si-event))

(defn get-schema [rules name] (get-in rules [name :schema]))
(defn get-xform [rules name] (get-in rules [name :xform]))
