(ns user
  (:require [qniform.main :as app]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]
            [clojure.test :refer [deftest is]]
            [malli.core :as m]))

(comment
  (app/start app/no-cors 3000)
  (app/start app/cors 3000)
  (app/stop))

(def share-event
  {:originated :system-x
   :id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f"
   :type :share-issue
   :shares 100
   :price-per-share 12.23
   :comment "Test Comment"})

(def invoice-event
  {:originated :system-t
   :id "eb6371ea-9cef-4a32-9b8d-abeb8cf87f20"
   :type :invoice-payable
   :amount 123.45
   :payee "your mum"
   :comment "Test Comment on invoice"})

(def invoice-bad-schema
  {:originated :system-t
   :id "eb6371ea-9cef-4a32-9b8d-abeb8cf87f20"
   :type :invoice-payable
   :amount "not a number"
   ;; missing payee
   :comment "Test Comment on invoice"})

(def rule-schema
  (m/schema [:map
             [:name :string]
             [:schema [:vector :any]]
             [:pattern :map]
             [:target :map]]))

(deftest regression
  (is (= "<h1>Qniform</h1>" (:body @(http/get "http://localhost:3000/"))))
  (is (= 404 (:status @(http/get "http://localhost:3000/bad-route"))))
  (is (= (json/read-str (:body @(http/post "http://localhost:3000/api/event"
                                           {:query-params {:hello "world"}
                                            :body (json/write-str share-event)})))
         {"comment" "Test Comment",
          "journal-entries"
          [{"event-id" "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
            "dr-cr" "credit",
            "account" "share-capital",
            "currency" "USD",
            "local-amount" 1223.0}
           {"event-id" "0b31b2a2-b144-4a9b-85cd-af99175e6a0f",
            "dr-cr" "debit",
            "account" "cash",
            "currency" "USD",
            "local-amount" 1223.0}]}))

  (is (= (json/read-str (:body @(http/post "http://localhost:3000/api/event"
                                           {:body (json/write-str invoice-event)})))
         {"comment" "Test Comment on invoice",
          "journal-entries"
          [{"event-id" "eb6371ea-9cef-4a32-9b8d-abeb8cf87f20",
            "dr-cr" "credit",
            "account" "invoices-payable",
            "currency" "USD",
            "local-amount" 123.45}
           {"event-id" "eb6371ea-9cef-4a32-9b8d-abeb8cf87f20",
            "dr-cr" "debit",
            "account" "expenses",
            "currency" "USD",
            "local-amount" 123.45}]}))


  (is (= ((json/read-str (:body @(http/post "http://localhost:3000/api/event"
                                            {:body (json/write-str invoice-bad-schema)})))
          "error")
         "Event Schema is not valid"))

  (is (= ((json/read-str (:body @(http/post "http://localhost:3000/api/event"
                                            {:body (json/write-str {:type :bad-event})})))
          "error")
         "Event Type :bad-event not recongnized"))

  (is (every? #(m/validate rule-schema %) (vals (read-string (slurp (:body @(http/get "http://localhost:3000/api/rules"))))))))

(comment
  @(http/get "http://localhost:3000/readback"
             {:query-params {:hello "world"}})

  @(http/post "http://localhost:3000/readback"
              {:query-params {:hello "world"}
               :body "body test"})

  (read-string (slurp (:body @(http/get "http://localhost:3000/api/rules")))))
