(ns qniform.frontend.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [ajax.core :as ajax]
            [malli.core :as m]
            [qniform.frontend.rules :refer [rules get-schema get-xform]]
            [clojure.string :as str]))

(defonce active-page (r/atom :try))
(def selected-rule (r/atom :share-issue))
(def trial-balance (r/atom nil))

(defn two-dp [num]
  (.toLocaleString num "en-UK"
                   (clj->js {"maximumFractionDigits" 2,
                             "minimumFractionDigits" 2})))

;; API calls
(defn get-rules [d]
  (ajax/GET "http://localhost:3000/api/rules"
    {:handler #(reset! d %)
     :error-handler (fn [{:keys [status status-text]}]
                      (js/console.log status status-text))}))

(defn get-trial-balance [d]
  (ajax/GET "http://localhost:3000/api/trial-balance"
    {:handler #(reset! d %)
     :error-handler (fn [{:keys [status status-text]}]
                      (js/console.log status status-text))}))

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

;; Components

(defn rule-dropdown []
  (fn []
    [:div.dropdown
     [:button.dropbtn "Select Rule"]
     [:div.dropdown-content
      (for [[k v] rules]
        [:a {:on-click #(reset! selected-rule k)}
         (:name v)])]]))

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
       #_[:p (prn-str @event)]
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

(defn nav []
  [:nav
   [:a [:p "Qniform"]]
   [:ul
    [:li {:on-click #(reset! active-page :landing)} "Landing Page"]
    [:li {:on-click #(reset! active-page :rules-tester)} "Rule Tester"]]])

(defn landing-page []
  [:div
   [:header
    [nav]
    [:h1 "Qniform"]
    [:p "Qniform is the future of accounting systems. It allows your accounting department 
        to drastically reduce the time they spend doing repetitive work like creating 
        journal entries, closing the books and running reports, freeing them up for more 
        value adding finance tasks."]
    [:p "As a modern organization, your business activity is handled by specialized upstream
              systems. Qniform will connect to them, and let you define how to generate accounting
              entries from them."]
    [:button {:on-click #(reset! active-page :try)}
     "Click here to try it out!"]

    [:figure
     [:img {:src "./images/flow.jpg"}]]]
   [:main
    [:section
     [:p "It does this by recognizing that the modern organization has specialized software 
        for almost every function, all generating activity that needs to be accounted for.
        Legacy enterprise accounting systems are built on the premise that" [:em " people "]
      "will be booking journal entries. Systematic integration with the rest of your software environment
     is often a poorly implemented afterthought.
     Qniform is built from the ground up to directly with your upstream software systems to find out what 
     activity needs accounting for, and using rules that you define, will turn those
     events into journal entries."]]
    [:section
     [:header [:h2 "Is Qniform right for my organization?"]]
     [:p "Qniform will be a good fit for you if:"]
     [:ul
      [:li "You want to minimize the time your accountants spend doing repetitive work
            like booking accounting entries."]
      [:li "Most of your accounting entries are generated by specialized upstream systems,
            like invoices systems, sales systems, or investment management systems."]
      [:li "You have an in-house technology team who are capable of building software
            which can send events from these systems to an API endpoint."]]]
    [:section
     [:header [:h2 "Features of Qniform"]]
     [:ul
      [:li "A unique event driven architecture. Book activity the second it happens"]
      [:li "A simple interface for connecting new upstream systems and creating the rules
          that turn their events into journal entries."]
      [:li "Utilities for gradual migration from an existing accounting system."]
      [:li "Close your month or year with ease using automated closing rules."]
      [:li "(something about knowledge dates)"]
      [:li "Changed your accounting rules? Rerun your entire GL with the new rules in seconds."]
      [:li "Reporting metadata allows flexible and changable reporting without polluting your chart of accounts."]
      [:li "Scripting language for advanced users without breakage risk."]]]]])

(defn rule-testing-page []
  [:div
   [:header
    [nav]
    [:h1 "Qniform Rule Tester"]
    [rule-dropdown]]
   [:main
    [event-form (get-schema rules @selected-rule)]]])

(defn trial-balance-view []
  (fn [tb]
    [:table
     [:tr [:th "Ledger"] [:th "Debit (USD)"] [:th "Credit (USD)"]]
     (for [[row-name [dr cr]] tb]
       ^{:key row-name}
       [:tr [:td row-name]
        [:td (two-dp (js/parseFloat dr))]
        [:td (two-dp (js/parseFloat cr))]])]))

(def dummy-upstream-systems (r/atom {}))

(defn systems-summary-component []
  (let [new-system-form-open (r/atom false)
        new-system-data (r/atom {})]
    (fn []
      [:div
       [:section
        (for [[id system] @dummy-upstream-systems]
          ^{:key id}
          [:aside [:h3 (:name system)] [:p (:description system)]])]

       (if-not @new-system-form-open
         [:section [:button
                    {:on-click #(swap! new-system-form-open not)}
                    "Add new System"]
          #_[:p.debug (pr-str @dummy-upstream-systems)]]
         [:section
          #_[:p.debug (pr-str @new-system-data)]
          [:form
           [:label {:for "system-name"} "System Name"]
           [:input#system-name
            {:on-change #(swap! new-system-data assoc :name (-> % .-target .-value))
             :value (:name @new-system-data)
             :type :text :name "system-name"
             :placeholder "System Name"}]
           [:label {:for "description"} "Description"]
           [:input#description
            {:on-change #(swap! new-system-data assoc :description (-> % .-target .-value))
             :value (:description @new-system-data)
             :type :text :name "description"
             :placeholder "Description"}]
           [:button {:type :submit
                     :on-click
                     #(do (.preventDefault %)
                          (swap! dummy-upstream-systems assoc
                                 (keyword (str/lower-case (str/replace (:name @new-system-data) #" " "-")))
                                 @new-system-data)
                          (reset! new-system-data {})
                          (reset! new-system-form-open false))}
            "Create"]]])])))

(defn try-page []
  (let [_tb (get-trial-balance trial-balance)]
    (fn []
      [:div
       [:header [nav]
        [:h1 "Abacus LLC"]
        [:p "A made up company that we've just set up to show you how to use Qniform"]
        [:p [:i "Book Currency: USD"]]]
       [:main
        [:hr]
        [:section
         [:header [:h2 "Trial Balance"]]
         [trial-balance-view @trial-balance]
         [:p "This is the current trial balance of Abacus.
              Right now, there is nothing in it, since we haven't booked any activity yet."]
         [:p "Let's fix that by setting up our first connected system and event rule."]]
        [:hr]
        [:header [:h2 "Upstream Systems"]
         [:p "Click the 'Add New System' button."]]
        [systems-summary-component dummy-upstream-systems]]])))

(defn app []
  [(case @active-page
     :landing      landing-page
     :rules-tester rule-testing-page
     :try          try-page)])

(defn mount []
  (rd/render [app]
             (.getElementById js/document "app")))

(defn main []
  (mount))

(defn reload []
  (mount))
