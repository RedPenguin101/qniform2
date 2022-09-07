(ns qniform.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [malli.core :as m]
            [meander.epsilon :as e]))

(defn ccy-amnt? [num]
  (and (number? num)
       (= num (/ (Math/round (* 100 num)) 100))))

(def money (m/schema [:fn {:display "Money amount"
                           :coercion :double}
                      ccy-amnt?]))

(defn round-2dp [num]
  (/ (Math/round (* 100 num)) 100))

(def share-issue-event
  (m/schema [:map
             [:event-id :string]
             [:shares :int]
             [:price-per-share money]]))

(m/form share-issue-event)

(m/validate share-issue-event
            {:event-id "hello"
             :shares 123
             :price-per-share 12.21})

(def journal-entry
  (m/schema [:map
             [:entry-id :string]
             [:event-id :string]
             [:transaction-id :string]
             [:knowledge-datetime :string]
             [:event-date :string]
             [:account :string]
             [:dr-cr :string]
             [:currency :string]
             [:local-amount money]
             [:book-amount money]]))

(defn share-xform [event]
  (e/match {:event event}
    {:event {:event-id ?event-id
             :shares ?shares
             :price-per-share ?price-per-share}}
    {:journal-entries [{:event-id ?event-id
                        :dr-cr :credit
                        :account :share-capital
                        :currency "USD"
                        :local-amount (round-2dp (* ?price-per-share ?shares))}
                       {:event-id ?event-id
                        :dr-cr :debit
                        :account :cash
                        :currency "USD"
                        :local-amount (round-2dp (* ?price-per-share ?shares))}]}))

(comment
  (def dummy-si-event {:event-id "hello"
                       :shares 123
                       :price-per-share 12.21})
  (share-xform dummy-si-event))

(defn rule-dropdown []
  (fn []
    [:div.dropdown
     [:button.dropbtn "Select Rule"]
     [:div.dropdown-content
      [:a "Share Issuance"]
      [:a "Tmp 1"]
      [:a "Tmp 2"]]]))

(defn type-display [typ]
  (cond
    (string? typ) typ
    (symbol? typ) (name typ)
    (keyword? typ) (name typ)
    (and (vector? typ) (= :fn (first typ)))
    (:display (second typ))))

(defn fn-spec? [typ] (and (vector? typ) (= :fn (first typ))))
(defn fn-spec-coerce [typ] (:coercion (second typ)))

(defn type->input [typ]
  (cond (fn-spec? typ) (type->input (fn-spec-coerce typ))
        (#{:double :int} typ) "number"
        :else "text"))

(defn type-coerce [typ value]
  (cond
    (= :string typ) value
    (= :int typ) (if (empty? value) 0 (js/parseInt value))
    (= :double typ) (if (empty? value) 0 (js/parseFloat value))
    (fn-spec? typ) (type-coerce (fn-spec-coerce typ) value)

    :else "COERCE FAIL"))

(defn jes->table [jes]
  [:table
   [:tr
    (for [header (keys (first jes))] [:th header])]
   (for [row (map vals jes)]
     [:tr (for [v row] [:td v])])])

(defn transaction-display [transaction]
  [:div
   [:h2 "Transaction"]
   [:h3 "Journal Entries"]
   (jes->table (:journal-entries transaction))])

(defn event-form [schema]
  (let [event (r/atom {:event-id "hello" :shares 12 :price-per-share 12.34})]
    (fn [schema]
      [:div
       [:h2 "Share Issuance Event"]
       [:div#event-form
        [:form {:on-submit #(.preventDefault %)}
         (doall (for [[nm typ] (rest (m/form schema))]
                  [:div
                   [:label {:for nm} (str (name nm) " (" (type-display typ) "): ")]
                   [:input {:type (type->input typ)
                            :id nm
                            :step 0.01
                            :value (get @event nm)
                            :on-change #(swap! event assoc nm (type-coerce typ (-> % .-target .-value)))}]]))]
        [:p (if (m/validate schema @event) (transaction-display (share-xform @event)) "not valid")]]])))

(defn app []
  [:div
   [:h1 "Qniform Rule Tester"]
   [rule-dropdown]
   [event-form share-issue-event]])

(defn mount []
  (rd/render [app]
             (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))