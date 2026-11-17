# S Caller v1.9.1 — User-selected Bottle-Green Calling Icon

## Icon update
- Uses the user's submitted glossy bottle-green handset with three signal arcs as the actual home-screen app icon.
- Updates legacy Android launcher, rounded launcher, modern adaptive launcher, and the app's small brand image to the same phone-only mark.
- Adds a dark bottle-green adaptive icon background; keeps the handset within Android's safe zone to prevent the phone symbol from being cropped.
- No added letters, slogans, rainbow or extra graphic on the icon.
- Source image is supplied by the user. A resampled compressed WebP is packaged in the app for lightweight loading.

## What did not change
- Calling functionality, contacts, call history, SIM selection, speaker/Bluetooth controls and current UI remain as in v1.9.0.
- Both manual and automatic call recording remain removed; no microphone permission or recording service is added.

## Verification and limitations
- GitHub CI unit tests, Android lint, APK build, Android API 23/26/35 emulator tests, and APK identity/signature/checksum verification are required.
- This is a **public debug-signed developer preview**, not a production-signed release; Android may refuse an in-place update when the prior APK has a different signing certificate.
- Back up app-local data before considering uninstalling an older version. Real-SIM calling compatibility still requires physical-device testing.
