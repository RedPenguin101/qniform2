(ns qniform.main
  (:require [reagent.core :as r]
            [clojure.edn :as edn]
            [reagent.dom :as rd]
            [ajax.core :refer [GET]]
            [malli.core :as m]
            [qniform.rules :refer [rules get-schema get-xform]]))

(def selected-rule (r/atom :share-issue))
(def rules2 (r/atom nil))

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
       [:p (prn-str @event)]
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

(defn get-rules [d]
  (GET "http://localhost:3000/api/rules"
    {:handler #(reset! d %)
     :error-handler (fn [{:keys [status status-text]}]
                      (js/console.log status status-text))}))

(defn get-print []
  (get-rules rules2)
  (fn []
    [:p (pr-str (:test (edn/read-string @rules2)))]))

(defn landing-page []
  [:div [:h1 "Qniform"]
   [:p "Qniform is the future of accounting systems. It allows your accounting department 
        to drastically reduce the time they spend doing repetitive work like creating 
        journal entries, closing the books and running reports, freeing them up for more 
        value adding finance tasks."]
   [:p "It does this by recognizing that the modern organization has specialized software 
        for almost every function, all generating activity that needs to be accounted for.
        Legacy enterprise accounting systems are built on the premise that" [:em " people "]
    "will be booking journal entries. Systematic integration with the rest of your software environment
     is often a poorly implemented afterthought.
     Qniform is built from the ground up to directly with your upstream software systems to find out what 
     activity needs accounting for, and using rules that you define, will turn those
     events into journal entries."]
   [:button "Click here to try it out!"]
   [:h2 "Features of Qniform"]
   [:ul
    [:li "A unique event driven architecture. Book activity the second it happens"]
    [:li "A simple interface for connecting new upstream systems and creating the rules
          that turn their events into journal entries."]
    [:li "Utilities for gradual migration from an existing accounting system."]
    [:li "Close your month or year with ease using automated closing rules."]
    [:li "(something about knowledge dates)"]
    [:li "Changed your accounting rules? Rerun your entire GL with the new rules in seconds."]
    [:li "Reporting metadata allows flexible and changable reporting without polluting your chart of accounts."]
    [:li "Scripting language for advanced users without breakage risk."]]])

(defn rule-testing-page []
  [:div
   [:p (pr-str @selected-rule)]
   [get-print]
   [:h1 "Qniform Rule Tester"]
   [rule-dropdown]
   [event-form (get-schema rules @selected-rule)]])

(defn app []
  [landing-page])

(defn mount []
  (rd/render [app]
             (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))
