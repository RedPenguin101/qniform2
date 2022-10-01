(ns qniform.frontend.main
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [ajax.core :as ajax]
            [malli.core :as m]
            [qniform.frontend.rules :refer [rules get-schema get-xform]]
            [clojure.string :as str]))

;; utils

(defn acceptable-name [string]
  (keyword (str/lower-case (str/replace string #" " "-"))))

(defn num->2dp-str [num]
  (.toLocaleString num "en-UK"
                   (clj->js {"maximumFractionDigits" 2,
                             "minimumFractionDigits" 2})))

;; translating from/to Malli specs

(defn type-display
  "Given clojure data describing a type 
   (in a loosely the Malli sense)
   will convert that to a word that's printable to the browser"
  [typ]
  (cond
    (string? typ) typ
    (symbol? typ) (name typ)
    (keyword? typ) (name typ)
    (and (vector? typ) (= :fn (first typ)))
    (:display (second typ))))

(defn type->html-input [typ]
  (cond (and (vector? typ) (= :fn (first typ)))
        (recur (:coercion (second typ)))

        (#{:double :int} typ) "number"
        :else "text"))

(defn type-coerce [typ value]
  (cond (and (keyword? typ) (= :string typ))    value
        (and (keyword? typ) (= :int typ))       (if (empty? value) 0 (js/parseInt value))
        (and (keyword? typ) (= :double typ))    (if (empty? value) 0 (js/parseFloat value))
        (and (vector? typ) (= :fn (first typ))) (recur (:coercion (second typ)) value)
        :else "COERCE FAIL"))

(defn event->spec
  "Given the data from the event creation form, will turn that
   into a format that can be used by Malli to create a spec."
  [event-data]
  (let [event-row-types {"Text"     :string
                         "Integer"  :int
                         "Currency" :double}]
    (conj (mapv #(vector (acceptable-name (first %))
                         (event-row-types (second %)))
                (remove empty? event-data))
          [:comment :string])))

;; Tutorial state and helper

(def tutorial-status (r/atom #{:at-start}))

(defn transition-tutorial [state action]
  (case action
    :set-up-first-system
    (-> state (disj :at-start) (conj :created-system))))

;; global state

(defonce active-page (r/atom :try))
(def selected-rule (r/atom :share-issue)) ;; doesn't need to be global
(def trial-balance (r/atom nil))

(def dummy-upstream-systems
  (r/atom #_{:corp-act {:name "CorpAction", :description "A system for manageing corporate actions"}}
   {}))

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
        [:td (num->2dp-str (js/parseFloat dr))]
        [:td (num->2dp-str (js/parseFloat cr))]])]))

(defn new-event-component [system-name show-atom]
  (let [event-name (r/atom "")
        event-data (r/atom [[] [] [] [] [] [] [] []])
        types ["<Select>" "Text" "Integer" "Currency"]
        num-rows (r/atom 1)]
    (fn []
      [:form
       #_[:p.debug (pr-str @event-name) ": " (pr-str @event-data)]
       #_[:p.debug (pr-str (event->spec @event-data))]
       [:label "Event Name"]
       [:input {:type :text :placeholder "Event Name"
                :on-change #(reset! event-name (-> % .-target .-value))}]
       [:table
        (for [row-num (range 0 @num-rows)]
          [:tr
           [:td [:input {:type :text :placeholder "field name"
                         :on-change #(swap! event-data assoc-in [row-num 0] (-> % .-target .-value))}]]
           [:td [:select
                 {:on-change #(swap! event-data assoc-in [row-num 1] (-> % .-target .-value))}
                 (for [t types]
                   [:option t])]]
           [:td [:button.small {:on-click #(do (.preventDefault %) (swap! num-rows inc))}
                 "new row"]]])]
       [:button {:on-click #(do (.preventDefault %)

                                (swap! dummy-upstream-systems assoc-in [system-name :events (acceptable-name @event-name)]
                                       (into [:map [:event-id (acceptable-name @event-name)]] (event->spec @event-data)))
                                (reset! show-atom false))}
        "save"]])))

(defn systems-summary-component []
  (let [new-system-form-open (r/atom false)
        new-system-data (r/atom {})
        selected-system (r/atom nil)
        new-event-form (r/atom true)]
    (fn []
      [:div
       ;; existing systems
       [:section
        #_[:p.debug
           "selected: " (pr-str @selected-system) " "
           (pr-str @dummy-upstream-systems)]
        (doall
         (for [[sid system] @dummy-upstream-systems]
           ^{:key sid}
           [:aside.system
            {:on-click #(if (count (:events system))
                          (reset! selected-system sid)
                          (reset! selected-system sid))
             :class (cond (= sid @selected-system) :selected
                          @selected-system :hidden)}
            [:h3 (:name system) " "
             (when (= @selected-system sid)
               [:a {:on-click #(do (.stopPropagation %)
                                   (reset! selected-system nil))} "x"])]
            [:p (:description system)]
            (if (not= sid @selected-system)
              [:p [:small
                   "Events: " (or (count (:events system)) "0")]]
              [:div
               (when @new-event-form
                 [new-event-component @selected-system new-event-form])
               [:button
                {:on-click #(swap! new-event-form not)}
                "New event"]])]))]

       ;; new system
       (if-not @new-system-form-open
         [:section [:button
                    {:on-click #(swap! new-system-form-open not)}
                    "Add new System"]]
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
                          (reset! new-system-form-open false)
                          (swap! tutorial-status transition-tutorial :set-up-first-system))}
            "Create"]]])])))

(defn try-page []
  (let [_tb (get-trial-balance trial-balance)]
    (fn []
      [:div
       [:header [nav]
        (when (:at-start @tutorial-status)
          [:div
           [:p.tutorial
            "We've created a company and general ledger for you called 'Abacus LLC'. 
          We'll use this to go through some of the concepts of Qniform"]
           [:p.tutorial
            "This page will look very similar to your 'main' entity page in production,
          except that this blue, italic tutorial text won't be there."]
           [:p.tutorial
            "If you ever don't know what to do next, look for this blue tutorial text somewhere on the page."]])
        [:h1 "ENTITY: Abacus LLC"]
        [:p "A made up company that we've just set up to show you how to use Qniform"]
        [:p [:i "Book Currency: USD"]]]
       [:main
        [:hr]
        [:section
         [:header [:h2 "Trial Balance"]]
         (when (:at-start @tutorial-status)
           [:p.tutorial "This is the current trial balance of Abacus.
              Right now, there is nothing in it, since we haven't booked any journal entries yet."])
         [trial-balance-view @trial-balance]
         (when (:at-start @tutorial-status)
           [:div
            [:p.tutorial "In Qniform, you don't book journal entries yourself.
                       Instead, you set up 'Upstream Systems' (representing the software your company uses to manage its business),
                       and then set up 'events' for those systems, which Qniform will turn into journal entries based on rules that
                       you create."]
            [:p.tutorial "Let's set up our first System and Event."]])]
        [:hr]
        [:header [:h2 "Upstream Systems"]]
        (if-not (:created-system @tutorial-status)
          [:div
           [:p.tutorial
            "The first thing a Company does is usually to issue some Equity.
                  So we'll set up a hypothetical upstream system for corporate actions, 
                  which will be sending you information about equity issuance.
                  "]
           [:p.tutorial "(Don't worry if you don't have a system that does this, 
                  Qniform will work fine)"]
           [:p.tutorial "Click the 'Add New System' button. 
                         In reality the name would be the name of the actual upstream application.
                         Here, just call it something sensible so you remember what it is."]]
          [:p.tutorial "Great. Now click on the system to select it."])

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
