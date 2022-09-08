(ns user
  (:require [qniform.main :as app]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(comment
  (app/start)
  (app/stop))

@(http/get "http://localhost:3000/")
@(http/get "http://localhost:3000/bad-route")
@(http/get "http://localhost:3000/readback?hello=world")
@(http/get "http://localhost:3000/readback"
           {:query-params {:hello "world"}})
@(http/post "http://localhost:3000/readback"
            {:query-params {:hello "world"}
             :body "body test"})

(def share-event
  {:originated :system-x
   :id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f"
   :type :share-issue
   :shares 100
   :price-per-share 12.23
   :comment "Test Comment"})

(read-string (:body @(http/post "http://localhost:3000/api/event"
                                {:query-params {:hello "world"}
                                 :body (json/write-str share-event)})))

(def invoice-event
  {:originated :system-t
   :id "eb6371ea-9cef-4a32-9b8d-abeb8cf87f20"
   :type :invoice-payable
   :amount 123.45
   :payee "your mum"
   :comment "Test Comment on invoice"})

(read-string (:body @(http/post "http://localhost:3000/api/event"
                                {:body (json/write-str invoice-event)})))

(def invoice-bad-schema
  {:originated :system-t
   :id "eb6371ea-9cef-4a32-9b8d-abeb8cf87f20"
   :type :invoice-payable
   :amount "not a number"
   ;; missing payee
   :comment "Test Comment on invoice"})

(read-string (:body @(http/post "http://localhost:3000/api/event"
                                {:body (json/write-str invoice-bad-schema)})))
