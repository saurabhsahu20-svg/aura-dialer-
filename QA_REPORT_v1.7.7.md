# S call v1.7.7 — Call Screen Recovery QA

**Package:** `com.scall.phone`  
**Version:** `1.7.7` (versionCode `17`)  
**Android support:** API 23+ (Android 6.0+)

## Original bug
From two real-device screenshots, Android displayed the app's **Answer / Decline** notification. After opening the call interface, the user saw **"Connecting to phone service…"** without caller info or normal controls.

## Implemented
1. The notification Answer action is a direct Activity PendingIntent, avoiding notification trampolines introduced in Android 12.
2. The call Activity receives Answer intents exactly once, asks CallBridge to reconnect to Android's actual InCallService call list, and displays the real call snapshot if available.
3. CallBridge rejects delayed onDestroy from older services rather than wiping the newer binding's calls.
4. CallBridge excludes disconnecting/disconnected calls from its active-call selection.
5. If Telecom does not deliver a live call object, the activity shows a bounded loading state and clear Retry/Close actions instead of a misleading endless spinner.
6. When an observed live call ends, its activity closes promptly without leaving an empty caller screen.

## Verification
- Kotlin/JUnit checks include the stale-service binding regression.
- Android emulator UI tests verify no-live-call recovery and retry; they **do not place actual cellular calls**.
- Workflow validates Android Lint, app builds, package name, minSdk, version and APK signature plus ZIP/SHA-256 integrity.
- Emulator checks run for Android 6/8/15 and the existing Android 10 smoke configuration.

## Real-device verification still required
**Answering real SIM calls**, speaker/mute audio, incoming notifications on specific Android skins, dual SIM switching and lock-screen full-screen visibility must be verified on the user's real phone. The screenshots alone cannot establish which Android Telecom service-binding event originally failed.

If still stuck, ensure S call is selected as the default Phone app, allow notification/full-screen privileges, and capture a fresh screen or device log when the call is active.

This is a debug-signed Preview APK, not a Google Play production release.
