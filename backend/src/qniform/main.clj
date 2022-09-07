(ns qniform.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults site-defaults]]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [qniform.main :as app]
            [clojure.pprint :refer [pprint]]))

(defonce server (atom nil))
@server

(defn readback [request]
  {:status  200
   :headers {"Content-Type" "text"}
   :body (with-out-str (pprint request))})

(defroutes routes
  (GET "/" [] "<h1>Hello, World!</h1>")
  (GET "/readback" [] readback)
  (POST "/readback" [] readback)
  (GET "/api" [] {:status  200
                  :headers {"Content-Type" "application/json"}
                  :body "{\"hello\": \"World\"}"})
  (route/not-found "<h1>Page not found</h1>"))

(defn start []
  (when-not @server
    (reset! server
            (jetty/run-jetty
             (wrap-defaults #'routes api-defaults)
             {:port 3000 :join? false}))))

(defn stop []
  (when @server
    (.stop @server)
    (reset! server nil)))

(comment
  (start)
  (stop))