(ns qniform.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [malli.core :as m]
            [qniform.rules :refer [rules get-schema get-xform]]))

(def selected-rule (r/atom :share-issue))

(defn rule-dropdown []
  (fn []
    [:div.dropdown
     [:button.dropbtn "Select Rule"]
     [:div.dropdown-content
      (for [[k v] rules]
        [:a {:on-click #(reset! selected-rule k)}
         (:name v)])]]))

(defn type-display [typ]
  (cond
    (string? typ) typ
    (symbol? typ) (name typ)
    (keyword? typ) (name typ)
    (and (vector? typ) (= :fn (first typ)))
    (:display (second typ))))

(defn fn-spec? [typ] (and (vector? typ) (= :fn (first typ))))
(defn fn-spec-coerce [typ] (:coercion (second typ)))

(defn type->html-input [typ]
  (cond (fn-spec? typ) (recur (fn-spec-coerce typ))
        (#{:double :int} typ) "number"
        :else "text"))

(defn type-coerce [typ value]
  (if (keyword? typ)
    (cond (= :string typ) value
          (= :int typ) (if (empty? value) 0 (js/parseInt value))
          (= :double typ) (if (empty? value) 0 (js/parseFloat value)))
    (cond
      (fn-spec? typ) (type-coerce (fn-spec-coerce typ) value)

      :else "COERCE FAIL")))

(defn journal-entries->table [jes]
  [:table
   [:tr
    (for [header (keys (first jes))] [:th header])]
   (for [row (map vals jes)]
     [:tr (for [v row] [:td v])])])

(defn transaction-display [transaction]
  [:div
   [:h2 "Transaction"]
   [:p [:strong "Comment: "] (:comment transaction)]
   [:h3 "Journal Entries"]
   (journal-entries->table (:journal-entries transaction))])

(defn event-form []
  (let [event (r/atom {})]
    (fn []
      [:div
       [:h2 (get-in rules [@selected-rule :name])]
       [:div#event-form
        [:form {:on-submit #(.preventDefault %)}
         (doall (for [[nm typ] (rest (m/form (get-schema rules @selected-rule)))]
                  [:div
                   [:label {:for nm} (str (name nm) " (" (type-display typ) "): ")]
                   [:input {:type (type->html-input typ)
                            :id nm
                            :step 0.01
                            :value (get @event nm)
                            :on-change #(swap! event assoc nm (type-coerce typ (-> % .-target .-value)))}]]))]
        [:p (if (m/validate (get-schema rules @selected-rule) @event)
              (transaction-display ((get-xform rules @selected-rule) @event))
              "Event is not valid")]]])))

(defn app []
  [:div
   [:p (pr-str @selected-rule)]
   [:h1 "Qniform Rule Tester"]
   [rule-dropdown]
   [event-form (get-schema rules @selected-rule)]])

(defn mount []
  (rd/render [app]
             (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))