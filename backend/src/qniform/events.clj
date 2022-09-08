(ns qniform.events
  (:require [qniform.rules :refer [rules get-schema get-xform]]
            [malli.core :as m]))

(defn validate-event [event]
  (let [schema (get-schema rules (:type event))]
    (cond
      (not schema)
      {:valid false
       :error (str "Event Type " (:type event) " not recongnized")
       :recognized-events (keys rules)}

      (m/validate schema event) {:valid true :event event}
      :else {:valid false
             :error "Event Schema is not valid"
             :explain (map #(update % :schema m/form) (:errors (m/explain schema event)))})))

(defn event->transaction [event]
  ((get-xform rules (:type event)) event))
