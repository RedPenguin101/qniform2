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

Backend for event API: 
  _DONE_ Return transaction when event passed in
  _DONE_ Handle schema spec not met
  _DONE_ Handle event type not found
  _DONE_ Change return body to JSON
  _DONE_ Move event handling to app namespace

Serve up specs/xforms to frontend.

New Rules
  Sale rule
  Loan issuance rule
General Ledger Booking / persistence.
Better FE reporting on Spec fails

## DESIGN QUESTIONS
What happens when an event comes in that fails the spec test?

