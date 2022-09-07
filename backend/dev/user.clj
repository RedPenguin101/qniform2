(ns user
  (:require [qniform.main :as app]
            [org.httpkit.client :as http]))

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