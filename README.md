# Qniform

Qniform is an accounting system premised on the idea that transactions are booked, not by accountants booking journal entries, but by the system receiving events from upstream systems.

Modern companies have many, many specialised software systems for performing their business tasks: sales, invoice management, asset management systems, payroll systems.
All of these generate activity which needs to be accounted for.
Qniform provides an interface for these systems to talk to your accounting systems by sending it their 'events', and methods for setting up rules which translate those events to journal entries.

## Quickstart
In this quickstart guide, we will spin up an accounting system from scratch, define several upstream systems which emit events for Qniform to turn into journal entries, and simulate activity from those upstream systems to generate a general ledger.

### Setting up a new accounting system
### Getting initial entries booked
### Setting up an upstream system: Invoicing
### Simulating events from the invoicing system
### Reviewing the general ledger

## Gradual migration
Moving a legacy accouting system to a new system is incredibly onerous, to the point where you might consider it not worth the effort.
Qniform offers tools for gradual migration, general ledger mirroring, and switchover.

### Interfacing with an existing accounting system
### General Ledger mirroring
### Historical entry migration

