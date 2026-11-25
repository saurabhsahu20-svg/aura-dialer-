# S Caller v1.9.30 — Hang Up Screen Dismissal Fix

## Reported problem
After answering a real cellular call and pressing the red **End Call** button, the in-call display can remain visible even after the call ends.

## Technical fix
- Android Telecom is the source of truth for which phone calls still exist. `CallBridge.refreshFromTelecom()` now reconciles its registered callbacks with actual Telecom calls, releasing orphan callbacks that some device firmwares may fail to remove via `onCallRemoved()`.
- Never interpret a failed or unavailable Telecom query as proof that the call ended.
- While `End Call` is being processed, recheck live Telecom calls every 250ms for up to 5 seconds rather than waiting indefinitely for an OEM state callback.
- Once the previously displayed call is genuinely absent, release proximity sensor protection and close `CallUiActivity` after a short 200ms confirmation check.
- A full-screen call notification opened **after** an already ended call now dismisses itself after a short grace period instead of staying on “Phone service not connected”.
- Existing End Call retries, call controls, SIM options, call history, WhatsApp/Telegram shortcuts, contact name recognition, and missed-call badge are preserved.
- JUnit regression tests cover a live call, a truly disconnected call, an orphaned callback, and a transient disconnecting call.

## Important limits
- The app cannot unilaterally assume a cellular call ended just because the user tapped a button. If the carrier or Android Telecom still reports an active call, show the genuine state and allow another End Call attempt.
- On-device OEM call, notification and proximity testing is still necessary. Emulator tests do not prove the fix for every cellular modem.
- v1.9.30 / versionCode 130, package `com.scall.phone` unchanged.
- The existing v1.9.21 signing-key mismatch remains: a new CI debug-signed APK cannot install on top of an APK signed with a different certificate. Preserve original data and signing credentials.
