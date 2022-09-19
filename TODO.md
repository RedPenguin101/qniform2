# QNIFORM TASKS
_DONE_ The Spec / Rule system and Front end
  _DONE_ User can enter data in form and (if valid) see journal entries
    _DONE_ Malli Spec for expense event
    _DONE_ User can input Event in form driven by Spec
    _DONE_ If provided data in form is valid spec, will display journal entries
    _DONE_ Tidy up form and table
    _DONE_ Add comment field
    _DONE_ Wrap journal entries in transaction abstraction
  _DONE_ User can select between two rules
    _DONE_ Write second spec

_DONE_ Backend for event API: 
  _DONE_ Return transaction when event passed in
  _DONE_ Handle schema spec not met
  _DONE_ Handle event type not found
  _DONE_ Change return body to JSON
  _DONE_ Move event handling to app namespace

Serve up specs/xforms to frontend.
  _DONE_ Route for sending specs
  _DONE_ build for backend.
  _DONE_ Connect frontend to backend
  Use Frontend to load specs and display to users (or maybe do xforms at backend - might need websockets for this)

Backend persistence and operations
  _DONE_ General Ledger Booking / persistence.
  _DONE_ Add dates/datetimes to JEs
  _DONE_ General ledger updating / append-only - new, nullify, correct

Accounting namespace
  _DONE_ Proper specs for journal entries
  _DONE_ booking new/nullify/update JEs
  _DONE_ aggregating updates
  _DONE_ trial balance

New Rules
  Sale rule
  Loan issuance rule

Landing page
Add transaction concept to backend
Book entries from Frontend
Better FE reporting on Spec fails
Websockets connections between FE and backend?
  For validation / transformation in tester
  For bookings
Activity generator / Upstream System Simulator
Month end close
Payment events and partials
Better COA handling, not strings

Event driven, no running application.

## MVP Description
1. User has landing page where they can see the pitch and features and get to the 'try it now' page.
2. User can set up new ledger, with name, book-ccy.
3. User can set up upstream system
4. User can define event / rule for that system
5. User can manually input and submit an event that gets turned into jes
6. User can see TB and dig into general ledger
7. User can look at JE and get to Transaction / Event
8. User can manually update an event, generating correction entries

## DESIGN QUESTIONS
What happens when an event comes in that fails the spec test?
