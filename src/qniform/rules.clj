(ns qniform.rules
  (:require [meander.epsilon :as e] ;; required for evaluation
            [malli.core :as m]
            [clojure.edn :as edn]))

(defn match-fn [pattern target]
  (list 'fn '[e] (list 'e/match 'e pattern target)))

(defn new-rule [{:keys [id name schema pattern target]}]
  {id {:name name
       :schema (m/schema schema)
       :pattern pattern
       :target target
       :xform (eval (match-fn pattern target))}})

(def rules (apply merge (map new-rule (edn/read-string (slurp "resources/rules.edn")))))

(defn get-schema [rules id] (get-in rules [id :schema]))
(defn get-xform [rules id] (get-in rules [id :xform]))

(comment
  ((get-xform rules :test) {:a 10 :b 15})
  ((get-xform rules :share-issue) {:id "hello"
                                   :shares 123
                                   :price-per-share 12.21
                                   :comment "comment"})
  ((get-xform rules :invoice-payable) {:id "hello"
                                       :payee "your mum"
                                       :amount 12.21
                                       :comment "comment"}))


