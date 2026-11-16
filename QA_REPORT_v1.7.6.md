# S call v1.7.6 — Android Compatibility QA

**Android package:** `com.scall.phone`
**Version:** `1.7.6` / `versionCode 16`
**Minimum Android:** **6.0 (API 23)**

## Compatibility checks

| API | Android version | Test |
| --- | --- | --- |
| 23 | 6.0 Marshmallow | Install/launch + Compose keypad/navigation/settings emulator smoke tests |
| 26 | 8.0 Oreo | Install/launch + Compose keypad/navigation/settings emulator smoke tests |
| 29 | 10 | Existing maintained emulator smoke tests |
| 35 | 15 | Install/launch + Compose keypad/navigation/settings emulator smoke tests |

**Cross-version GitHub Actions workflow:** [Android 6 / 8 / 15](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-cross-version-tests.yml). The cross-version suite passed for all three configured emulators on the minSDK compatibility changes. Check the latest workflow run for the exact release commit's result.

Other quality checks: Android Lint; unit tests for calling permissions, swipe thresholds, SIM routing and low-API fallbacks; APK package name/version/minSdk, signature, ZIP archive and SHA-256 checksum in Release workflow.

## Changes
1. Guard NotificationChannel creation and channel permission checks for pre-Android 8.
2. Use compatible Android notification settings destinations for Android 6/7.
3. Guard default voice/SMS/data subscription calls introduced in API 24.
4. Avoid Telecom null-service crashes and detect devices without cellular calling.
5. Retain default-dialer onboarding, caller screen and notification/FULL_SCREEN intent controls.

## Unverified
Actual SIM card calling on real devices, incoming ringing/lock-screen heads-up on specific OEM phones, emergency calls, carrier restrictions, in-place upgrades across independently debug-signed versions. This APK is a testing preview, not a Google Play production release.
