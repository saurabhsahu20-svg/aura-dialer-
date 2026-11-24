# S Caller v1.9.25 — Performance and Accuracy Preview

Base: full current v1.9.24 app, including the corrected keypad height and spacing, accurate contact/call-history behavior, and WhatsApp/Telegram/SMS shortcuts.

Focused improvements:
- Run Contacts and Favorites search on Dispatchers.Default instead of Compose UI thread.
- Cancel obsolete search requests and debounce non-empty search by 75ms.
- Match names regardless of case and phone numbers regardless of formatting; avoid mixed text/number false positives.
- Unit tests cover 7 search scenarios.
- Keep package name com.scall.phone and monotonically increase Android version code to 125.
- Publish successful APKs under the actual version name, not hardcoded older releases.

Validation: CI must pass unit tests and debug APK compilation, with Android Lint/cross-version tests evaluated. Real-device SIM, cellular hangup and in-place update signing tests remain outstanding. This preview does not claim completion of the entire v2 roadmap. A debug preview cannot replace an installed APK if its signing certificate differs; protect app data first.
