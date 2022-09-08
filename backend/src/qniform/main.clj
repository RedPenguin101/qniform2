(ns qniform.main
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults site-defaults]]
            [ring.util.request :as rreq]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [qniform.main :as app]
            [clojure.pprint :refer [pprint]]
            [clojure.data.json :as json]
            [malli.core :as m]
            [qniform.rules :refer [rules get-schema get-xform]]))

(defonce server (atom nil))
@server

(defn readback [request]
  {:status  200
   :headers {"Content-Type" "text"}
   :body (with-out-str (pprint request))})

(defn validate-event [event rules]
  (if (m/validate (get-schema rules (:type event))
                  event)
    ((get-xform rules (:type event)) event)
    (throw (ex-info "Event is invalid" {:explain (m/explain (get-schema rules (:type event))
                                                            event)}))))

(defn event-parser [json-event]
  (-> json-event
      (json/read-str :key-fn keyword)
      (update :originated keyword)
      (update :type keyword)
      (validate-event rules)))

(defn event-handler [request]
  {:status  200
   :headers {"Content-Type" "text"}
   :body (pr-str (event-parser (rreq/body-string request)))})

(defroutes routes
  (GET "/" [] "<h1>Qniform</h1>")
  (GET "/readback" [] readback)
  (POST "/readback" [] readback)
  (POST "/api/event" [] event-handler)
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