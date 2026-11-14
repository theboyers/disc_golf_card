# Disc Golf Card — App Requirements

## Purpose & Scope

This app does **one thing well**: scoring a disc golf round, and listing and displaying past rounds in an easy-to-read and easy-to-share format.

Any feature or functionality that does not directly serve the requirements below is **out of scope** and must not be implemented.

---

## Core Functional Requirements

1. **Round Scoring**
   - Score an active disc golf round cleanly and efficiently.
   - Support recording hole-by-hole scores for a round.

2. **Past Rounds History & Display**
   - List past rounds.
   - Display completed rounds in an easy-to-read format.
   - Provide an easy-to-share format for round results.

3. **Data Export & Portability**
   - Allow the user to export their data in a standard, portable format they can use elsewhere.

4. **Seamless Device Switching & Backward-Compatible Imports**
   - Make switching devices seamless via historical data transfer.
   - Exports from **any** previous version of the app must be importable into **any** future version of the app.

---

## Non-Functional Requirements

1. **Minimal Battery Usage**
   - Limit battery usage to the bare minimum (e.g., no unnecessary background work, polling, location/GPS tracking, or heavy rendering).

2. **Strict Scope Discipline**
   - Do not implement any functionality that does not fall under:
     - Scoring a disc golf round
     - Listing, displaying, and sharing past rounds
     - Exporting data in a portable format
     - Importing/transferring historical data across devices and versions
     - Minimizing battery usage
