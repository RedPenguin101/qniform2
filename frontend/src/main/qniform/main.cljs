(ns qniform.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [malli.core :as m]
            [meander.epsilon :as e]))

(def app-state (r/atom {:dropdown-selected :share-issue}))

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
             [:price-per-share money]
             [:comment :string]]))

(defn share-xform [event]
  (e/match {:event event}
    {:event {:event-id ?event-id
             :shares ?shares
             :price-per-share ?price-per-share
             :comment ?comment}}
    {:comment ?comment
     :journal-entries [{:event-id ?event-id
                        :dr-cr :credit
                        :account :share-capital
                        :currency "USD"
                        :local-amount (round-2dp (* ?price-per-share ?shares))}
                       {:event-id ?event-id
                        :dr-cr :debit
                        :account :cash
                        :currency "USD"
                        :local-amount (round-2dp (* ?price-per-share ?shares))}]}))

(def invoice-payable-event
  (m/schema [:map
             [:event-id :string]
             [:payee :string]
             [:amount money]
             [:comment :string]]))

(defn invoice-xform [event]
  (e/match {:event event}
    {:event {:event-id ?event-id
             :payee ?payee
             :amount ?amount
             :comment ?comment}}
    {:comment ?comment
     :journal-entries [{:event-id ?event-id
                        :dr-cr :credit
                        :account :invoices-payable
                        :currency "USD"
                        :local-amount ?amount}
                       {:event-id ?event-id
                        :dr-cr :debit
                        :account :expenses
                        :currency "USD"
                        :local-amount ?amount}]}))

(def rules {:share-issue {:name "Share Issuance"
                          :schema share-issue-event
                          :xform share-xform}
            :invoice-payable {:name "Invoice payable"
                              :schema invoice-payable-event
                              :xform invoice-xform}})

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
      (for [[k v] rules]
        [:a {:on-click #(swap! app-state assoc :dropdown-selected k)}
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
   [:p [:strong "Comment: "] (:comment transaction)]
   [:h3 "Journal Entries"]
   (jes->table (:journal-entries transaction))])

(defn event-form []
  (let [event (r/atom {})]
    (fn []
      [:div
       [:h2 (get-in rules [(:dropdown-selected @app-state) :name])]
       [:div#event-form
        [:form {:on-submit #(.preventDefault %)}
         (doall (for [[nm typ] (rest (m/form (get-in rules [(:dropdown-selected @app-state) :schema])))]
                  [:div
                   [:label {:for nm} (str (name nm) " (" (type-display typ) "): ")]
                   [:input {:type (type->input typ)
                            :id nm
                            :step 0.01
                            :value (get @event nm)
                            :on-change #(swap! event assoc nm (type-coerce typ (-> % .-target .-value)))}]]))]
        [:p (if (m/validate (get-in rules [(:dropdown-selected @app-state) :schema]) @event)
              (transaction-display ((get-in rules [(:dropdown-selected @app-state) :xform]) @event))
              "Event is not valid")]]])))

(defn app []
  [:div
   [:p (pr-str @app-state)]
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