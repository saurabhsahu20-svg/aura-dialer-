# S Caller v1.9.0 — Green S + Rainbow Call Logo

## New S Caller app icon
- Replaces the Android launcher and round icon with the user's selected logo: vivid **green S**, **rainbow-colored phone handset** and glowing dark-blue circular badge.
- Updates the S Caller app header to the same brand image.
- Adds an optimized native Android WebP resource, with adaptive launcher safe-zone insets so the emblem is not cropped on modern devices.
- Preserves compatibility with older Android versions through a non-adaptive icon drawable.

## Functionality
- No change to outgoing/incoming calls, SIM selection, contacts, call history, calling controls, permissions or data storage.
- Both automatic and manual call recording remain fully removed, as in v1.8.9.
- No new permissions or background recording components.

## Verification required
- Build/compile, unit tests, Android lint, emulator UI tests on Android API 23, 26, 35.
- GitHub preview workflow verifies version, package and APK signature, and includes SHA-256 checksum.
- A real-device check is still needed to confirm the home-screen icon appearance on individual launchers.
- This is a debug-signed **developer preview**, not a production-signed stable release; APK certificate mismatch may require a backup and reinstall, potentially losing private app data.
