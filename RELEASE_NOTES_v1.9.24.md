# S Caller v1.9.24 — Smooth Search & Stability Preview

Built on the complete v1.9.23 source, including keypad spacing, real call history, contact identity and deletion protection, Android Telecom-based dialing/hang-up, and WhatsApp / Telegram / SMS shortcuts.

## Changes
- Contacts/Favorites search now runs on a background dispatcher instead of inside a Compose UI recomposition.
- Cancels obsolete searches as users type; short 75ms debounce avoids unnecessary repeated scans.
- Normalizes formatted phone-number searches, case-insensitively matches names, and guards against mixed text/number false positives.
- Adds unit tests covering seven positive/negative search cases.
- Version `1.9.24`, code `124`, original app ID `com.scall.phone` unchanged.
- Publishes build artifacts under the actual version tag, not an outdated hardcoded release.

## Quality and limits
Android CI must pass `:app:testDebugUnitTest` and `:app:assembleDebug`; a successful build is not a substitute for real device and telecom network tests. This is a stability-focused incremental update, not completion of every optional future feature in the v2.0 roadmap. Debug signing keys may vary between CI builds, so in-place installation over an existing APK is not guaranteed without matching signing keys. Backup app data before attempting to replace an incompatible build; do not uninstall your current default dialer without a safe alternative.
