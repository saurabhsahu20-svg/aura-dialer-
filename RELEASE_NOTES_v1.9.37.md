# S Caller v1.9.37 — Full Incoming / Outgoing Call Details

## Bug fixed
The Recents > Details button previously opened the generic contact/photo screen rather than the selected call-log event. Users could not reliably see when they received, made, missed, rejected or blocked a call, how long it lasted, or previous calls to the same number.

## New Call Details screen
- Details from a Recents entry opens its **specific Android CallLog row**, tracked by stable row ID.
- Displays caller's locally saved name, number and photo (when available).
- Clearly labels incoming, outgoing, missed, rejected, blocked, voicemail and other event types.
- Displays the **full locally formatted date and time, including seconds**, the recorded call duration and, for completed calls with recorded duration, an **approximate end time**.
- Shows a scrollable **complete history for this number**, newest first, with each call's type, date/time and duration, plus incoming/outgoing/missed totals. Tapping an earlier row switches to its details.
- Uses country-aware full-number matching for Indian 10-digit / +91 formats, never merges unrelated international country codes. Never combines separate private/unknown callers as one contact.
- Carries over local custom caller photo, call note, custom display title and Call/Message actions. No call-history data are removed.
- Back returns to the same Recents page. Contact details and caller photo editor remain separate, unchanged.
- History rows render lazily to avoid placing the entire call list onscreen at once.

## Honesty about the Android CallLog
The standard CallLog records a single event timestamp and call duration; Android does not guarantee separate, exact ringing, answered and end timestamps for every SIM call. The displayed end time is explicitly labelled **approximate**, not invented precise data. Information is shown only if the device's Android call history contains it and READ_CALL_LOG is granted.

## Quality and install limits
Unit tests cover international phone matching, distinct unknown contacts, chronological sorting, call types, event timing, duration units and overflow. Android Compose test verifies that actual event details are displayed.

Version 1.9.37 / versionCode 137, unchanged package `com.scall.phone`. Prior navigation, missed-call alerts, caller display, flip-to-silence and hangup code is preserved.

A passing CI build/emulator test is not proof of correct behavior for every mobile SIM and firmware; real-device testing is still required. In-place installation over v1.9.21 requires the original matching signing certificate/private key; do not uninstall the old app without a safe backup.
