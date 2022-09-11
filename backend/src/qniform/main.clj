(ns qniform.main
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults site-defaults]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.request :as rreq]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [clojure.data.json :as json]
            [qniform.events :refer [validate-event event->transaction]]
            [qniform.rules :refer [rules]]))

(defonce server (atom nil))

(defn readback [request]
  {:status  200
   :headers {"Content-Type" "text"}
   :body (pr-str request)})

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

(defn rule-request [_]
  {:status 200
   :header {"Content-Type" "text"}
   :body (pr-str (update-vals rules #(dissoc % :xform)))})

(defroutes routes
  (GET "/" [] "<h1>Qniform</h1>")
  (GET "/readback" [] readback)
  (POST "/readback" [] readback)
  (POST "/api/event" [] event-response)
  (GET "/api/rules" [] rule-request)
  (route/not-found "<h1>Page not found</h1>"))

(defn no-cors [routes]
  (-> routes
      (wrap-defaults api-defaults)))

(defn cors [routes]
  (-> routes
      (wrap-defaults site-defaults)
      (wrap-cors :access-control-allow-origin [#"http://localhost:9090"]
                 :access-control-allow-methods [:get :put :post :delete])))

(defn start [wrapper port]
  (when-not @server
    (reset! server
            (jetty/run-jetty
             (wrapper #'routes)
             {:port port :join? false}))))

(defn stop []
  (when @server
    (.stop @server)
    (reset! server nil)))

(defn -main []
  (start cors 3000))

(comment
  (start cors 3000)
  (stop))
