(ns qniform.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults site-defaults]]
            [ring.util.request :as rreq]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [qniform.events :refer [validate-event event->transaction]]))

(defonce server (atom nil))
@server

(defn readback [request]
  {:status  200
   :headers {"Content-Type" "text"}
   :body request})

(defn event-parser [json-event]
  (-> json-event
      (json/read-str :key-fn keyword)
      (update :originated keyword)
      (update :type keyword)))

(defn event-handler [event]
  (let [v (validate-event event)]
    (if (:valid v)
      {:status 200
       :body (json/write-str (event->transaction event))}
      {:status 400
       :body (json/write-str (merge {:event event} (dissoc v :valid)))})))

(defn event-response [request]
  (merge
   {:headers {"Content-Type" "application/json"}}
   (->> request
        rreq/body-string
        event-parser
        event-handler)))

(defroutes routes
  (GET "/" [] "<h1>Qniform</h1>")
  (GET "/readback" [] readback)
  (POST "/readback" [] readback)
  (POST "/api/event" [] event-response)
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