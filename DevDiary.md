# Qniform Dev Diary
## 7th Sept
Created a dummy frontend for the "Qniform Event Tester", which allows a user to select a predefined event type from a dropdown, creates a form with the relevant fields to fill in, and applies the relevant rule to turn that into a transaction / journal entries.

Created a simple webserver with an 'event' POST endpoint, which validates the event and returns the consequent journal entries.

In both the above there are two rules defined - Equity injection and Invoice recieved.

## 8th Sept
Moved the backend rule definitions into a data file, with dynamic loading and creation of transforms.

Added a API route for querying the back end for the rules (specs and transforms), so they can be applied / validated on the frontend without hardcoding.

Improved the event API, adding some sad path handling for cases where the event spec validation fails, or the event just isn't recognized.

Added some regression tests to user namespace.

## 11th September
Added build file for backend.

The frontend now calls the backend for the rules, but doesn't apply them yet (the quoted forms for the transforms are causing some issues.)

I had to deal with CORS. 
I forget how it works every time, and still don't really understand it.
But it works.

## 18th September
### Persistance and knowledge datetimes
Added persistance (SQLite DB) for journal entries, and a DB layer with functions `book-journal-entries` and `get-journal-entries`.
In addition to handling the mechanics of the DB writes and reads, it handles (and abstracts) any translation needed for turning domain models to DB schema and vice versa. 

```clojure
(def journal-trans
  {:event-id :event_id
   :je-type :je_type
   :update-of :update_of
   :dr-cr :dr_cr
   :local-amount :local_amount
   :effective-date :effective_date
   :knowledge-datetime :knowledge_datetime})

(defn domain->db [key-map domain-trans]
  (update-keys domain-trans #(or (key-map %) %)))

(defn db->domain [key-map domain-trans]
  (update-keys domain-trans #(or ((map-invert key-map) %) %)))
```

I also added an `accounting` namespace which will handle the busiess logic related to accounting.
Right now it:

* contains the specs for the different types of journal entries (new, correction, nullification) using the domain model.
* A `book-journal-entries` of its own, which handles validation and calls the DB (though it's pass-through for the DB call, it should return something better.)
* Functions for aggregation of JEs,.`filter-on-datetime`, `aggregate-je`, and `trial-balance`.

All of these are focused on an important concept for this system, and for accounting systems generally.
The 'event log' of journal entries is bitemporal[^1].
There is a date dimension for when something actually happened (called the **effective date**), and a second, totally separate one for _when you knew about it_ (called the **knowledge date**, and actually a datetime.)

[^1]: Actually you could argue _tritemporal_ when you start adding concepts of 'closing' into the mix.

I won't go into why this is so important, but it has some important consequences for implementation.
First, your log of entries has to be, in effect if not literally, immutable and append only.
If you book an expense entry on the 15th of the month for $20, and on the 5th of the _next_ month you go back and change that entry so it is $30, you can't just update in place because you'll need to know that at a point in time you _thought_ your expense was $20 even though you now know it's $30[^2].

[^2]: Many accounting systems have a weaker version of this concept, where you can update-in-place up to the point where you "Close" the books at a given date, meaning you can't change anything before that close point, and have to book 'adjusting' entries. These systems often conflate knowledge and effective date, and are generally unsuitable to anything other than trivially small usecases.

As a consequence our journal entry database is append only.
To 'change' an entry, you have to either 'correct' it (supply the correct entry, with a reference to the original transaction) or 'nullify' it, which just removes it.
But none of these entries are touched after initially being booked.
So when you query the database you're potentially going to get back multiple rows for a single journal entry, which need to be aggregated into a single entry[^3].

[^3]: Really identical to 'projection' in an event sourcing system. Which ultimately is what this is, but hopefully without the jargon and hype.

This is an important detail the accounting namespace is hiding:
The implementation of the event history.
In this namespace, everything you do should have the option to supply a 'knowledge datetime', and the values returned will be 'journal entries' showing what you knew at that time, with no exposure of the 'new, correct, nullify' concepts that actually live in the database.

```clojure
(defn filter-on-datetime [datetime jes]
  (remove #(t/date> (:knowledge-datetime %) datetime) jes))

(defn aggregate-je
  "given a set of jes and a datetime, will return the set of JEs representing the known state of the system as at that datetime."
  ([jes]
   (vals (reduce (fn [A entry]
                   (case (:je-type entry)
                     "new" (assoc A (:id entry) (assoc entry :corrections []))
                     "correct" (update A (:update-of entry) (update entry :corrections conj (:update-of entry)))
                     "nullify" (dissoc A (:update-of entry))))
                 {}
                 jes)))
  ([jes datetime]
   (aggregate-je (filter-on-datetime datetime jes))))

(defn- tb-signed->drcr [tb]
  (update-vals tb #(if (neg? %) [:cr (abs %)] [:dr %])))

(defn trial-balance [jes]
  (tb-signed->drcr (reduce (fn [A entry]
                             (update A (:account entry)
                                     (if (= "debit" (:dr-cr entry)) + -)
                                     (:local-amount entry)))
                           (zipmap (set (map :account jes)) (repeat 0))
                           jes)))
```

### MVP
I defined what the MVP for this effort is.
In short, I want a landing page which can be used for pitching.
In addition to a few paras describing the product, this means having a sort of 'try it now' feature that will guide the users through the key functionality of the program.
Here's the list.

1. User has landing page where they can see the pitch and features and get to the 'try it now' page.
2. User can set up new ledger, with name, book-ccy.
3. User can set up upstream system
4. User can define event / rule for that system
5. User can manually input and submit an event that gets turned into jes
6. User can see TB and dig into general ledger
7. User can look at JE and get to Transaction / Event
8. User can manually update an event, generating correction entries, and see the impact of that in TB/GL
9. User can close the books.

One thing I would like to have in there as well, though this is probably the next phase, is a 'simulate' option. 
The user can do all of the above, but then can hit a 'simulate' button, which will start generating events based on the rules, and closing the books.
This should highlight what is the key attribute of Qniform, which is the touchless operation.

## 21st September
Doing some mockups for the above functionality.

First, tidy up the landing page with [MVP.css](https://andybrewer.github.io/mvp/), and add a simple nav bar and page navigation.
Also, add a "Try" page, which is navigated to when the user clicks the "Try it Now!" button from the landing page.

From this page, the user should be presented with a page representing a dummy entity/general ledger[^4].

[^4]: It is possible to have multiple general ledgers for a single entity, but we avoid that complexity for now.

This is the main entity screen. From here you should be able to see, or navigate to, the TB, GL, Rules, and Events.
(Ultimately there should be some sort of period selector here, but not necessary for now)
The GL (or a GL ledger) you can get to by drilling down into the TB.
You can also get there by going through events, selecting one that will show you the transaction/entries, and from there to the ledgers.

Start with the TB.
A simple table to start with:

```
      DR    CR
acc1   -     -
acc2   -     -
acc3   -     -
acc4   -     -
```

Obviously, it will be empty at first.
We'll serve this up from the backend, which requires adding a TB endpoint.

A quick tangent: Up to now I've been starting the front and backend separately.
That's a pain, so I want to do it together, following [this guide.](https://blog.agical.se/en/posts/shadow-cljs-clojure-cljurescript-calva-nrepl-basics/)

It was actually really easy! I was _not_ expecting it to be.
One thing I couldn't get working: I have a 'build' defined in `shadow-cljs.edn` and an _alias_ (dev) defined in deps.edn. I can't figure out how to start both of them at one.
Not a problem for now.

Now, merge the branch in.

