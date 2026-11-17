# S Caller v1.9.2 — Fresh Bottle-Green UI & Stability Update

## Fresh professional interface
- Refreshed Material 3 Light, Dark and AMOLED themes with the user's bottle-green identity, readable contrast and consistent surfaces.
- Calling screen now uses a deep emerald gradient with clear call/audio controls.
- Polished app bar with the existing user's green calling icon and succinct title.
- A small **Ready to connect?** shortcut opens the actual dial pad from Recents; no fake calling or unsupported features.
- More consistent navigation colors, selected-tab states, contact avatar accents and search outlines.

## Reliability and performance fixes
- Repeated external Android ACTION_DIAL requests with the same phone number now reopen the keypad correctly. This is regression-tested on emulator.
- Invalid or obsolete caller-photo preferences (wrong types, NaN or infinity) are safely defaulted/clamped before reaching Compose graphics transforms, avoiding certain crashes.
- Private per-call history notes gracefully handle malformed preference values.
- Saving caller photos and custom call notes shows an error message instead of crashing on storage errors.
- Added tests for NaN/infinite photo values and emulator UI tests for Quick Dial and repeated external dial intents.
- All previous stable performance improvements, keypad, dual-SIM choices, contacts, call history and Android Telecom service behavior are preserved.

## Explicit limitations
- This version does **not** add manual or automatic call recording, VoIP calling, spam blocking, HD noise cancellation, or any feature unsupported by the source.
- Android 6, 8 and 15 emulator UI checks, unit tests, lint and APK build do **not** prove every real SIM call works on every OEM device.
- The previously reported 'S call keeps stopping' symptom can be certified fixed only with a device-specific crash log and real phone/carrier tests.
- No code changes are made to emergency calling pathways.

## Download and signing
- The GitHub release artifact is a **public debug-signed developer preview**, not a production release.
- Debug key differences may prevent in-place upgrading. Back up app-local settings and images before considering uninstall, and keep another default Phone app available.
- APK identity, signing, SHA-256 and the unchanged no-recording manifest requirements are checked by CI.
