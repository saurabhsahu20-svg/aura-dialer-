# S call v1.7.2 — Automated QA Report

**Source:** https://github.com/saurabhsahu20-svg/aura-dialer-  
**Android package:** `com.scall.phone`  
**Version:** `1.7.2` (versionCode `12`)  
**Minimum Android:** 8.0 / API 26

## Verified test results

1. **Unit tests passed** — automated Kotlin/JUnit checks for SIM routing, strict swipe thresholds, Back navigation, dial-pad sanitization, and call history classification.
2. **Android emulator UI smoke tests passed: 4/4** — GitHub Actions [emulator run 43741043853](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43741043853). Tested on Android 10 emulator (API 29):
   - The app launches and renders the Recents screen.
   - Keypad navigation and Back arrow return to Call History.
   - Digit input and Clear work.
   - SIM & Network settings open from toolbar even though the large banner is hidden.
3. **Android Lint passed** — [lint run 43740932458](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43740932458). Resolved four blocking issues: explicit SIM permission checks and Android 8.0 call-screen lock-screen support.
4. **CI APK verification** — the new release job tests the exact v1.7.2 source again and validates APK package/version, signing, ZIP archive integrity, SHA-256 checksum and published asset names.

## Fixed issues

- Runtime permission checks for Android Telecom / SubscriptionManager before reading phone line details.
- Incoming call activity now handles Android 8.0 (API 26) lock-screen presentation through supported window flags.
- Added emulator UI tests for keypad, navigation and SIM settings.

## Important limitations

**The automated emulator has no real SIM or carrier network.** Actual phone calls, incoming/outgoing cellular routing, multiple SIMs, the lock screen with a real incoming call and emergency calling have **NOT** been tested on physical phones. Do not treat this preview as a fully proven replacement for your device's stock dialer.

This APK is **debug signed**. Debug signing certificates can differ across GitHub Actions builds: installing over an older preview may cause a certificate mismatch. Back up custom caller photos and preferences before uninstalling the old app. A production signing keystore is required for a stable in-place upgrade path.

The test report does not claim that runtime permissions, phone carrier functionality, or Play Store readiness have been comprehensively validated.
