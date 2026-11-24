# S Caller v1.9.23 — Speed & Accuracy Preview

## Preserved
All existing v1.9.22 features and the existing `com.scall.phone` application ID:
- Actual Android Telecom call handling and recent hang-up improvements.
- Existing dial pad, contacts, favourites and complete call history.
- Direct WhatsApp, Telegram and SMS options.
- Prior keypad layout, contact identity and deletion-safety fixes.

## Changes
- Contacts and Favorites filtering is no longer performed synchronously on the Compose UI thread. Searches run on `Dispatchers.Default`.
- New search requests cancel old in-flight searches; a short 75 ms debounce reduces repeated work while typing.
- Contacts search matches formatted phone numbers after normalizing digits and avoids mixed-text false matches.
- Adds repeatable unit tests covering case-insensitive names, formatted phone numbers, blank queries and invalid mixed queries.
- Version code increased to 123 (`1.9.23`).
- APK publishing now determines its release tag from the actual Gradle version instead of uploading new builds to stale tags.
- Obsolete 1.9.18 workflows are manual-only to avoid overwriting old release artifacts.

## Validation and limits
- Android CI must pass `:app:testDebugUnitTest` and `:app:assembleDebug` before this build is marked ready.
- Tests that require a real cellular network, multiple SIMs, Bluetooth headsets and different OEM handsets have **not** been performed by this source-only change.
- Debug-signed preview APKs may use a different signing certificate from previous versions; an in-place update is possible only with an unchanged package name **and** compatible signing certificate.
- Never uninstall an existing dialer without backing up settings and verifying a safe fallback/default phone app.
- S Caller does not and cannot check registration on WhatsApp/Telegram from a number alone.

This release is a focused stability improvement, not an unsupported guarantee that every requested feature from the S Caller v2.0 roadmap is complete.
