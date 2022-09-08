(ns user
  (:require [qniform.main :as app]
            [org.httpkit.client :as http]
            [clojure.data.json :as json]))

(def event (json/write-str
            {:originated :system-x
             :id "0b31b2a2-b144-4a9b-85cd-af99175e6a0f"
             :type :share-issue
             :shares 100
             :price-per-share 12.23}))

(comment
  (app/start)
  (app/stop))

@(http/get "http://localhost:3000/api")
@(http/get "http://localhost:3000/")
@(http/get "http://localhost:3000/readback?hello=world")
@(http/get "http://localhost:3000/readback"
           {:query-params {:hello "world"}})
@(http/post "http://localhost:3000/readback"
            {:query-params {:hello "world"}
             :body "body test"})

@(http/post "http://localhost:3000/api/event"
            {:query-params {:hello "world"}
             :body event})