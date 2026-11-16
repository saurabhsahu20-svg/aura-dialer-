# S call v1.8.0 — Audio & Calling QA Report

**Source repository:** https://github.com/saurabhsahu20-svg/aura-dialer-  
**Package:** `com.scall.phone`  
**Version:** `1.8.0` (versionCode 18)  
**Minimum supported Android:** 6.0 (API 23) | `targetSdk 35`

## Changes covered by automated checks

1. **Legacy audio route safety (API 23–33).** The call UI offers only output devices reported in Telecom's available-route mask. Speakerphone-off prefers an attached wired headset and avoids unavailable Bluetooth routes.
2. **New Android audio endpoints (API 34+).** The app observes `onAvailableCallEndpointsChanged`, `onCallEndpointChanged` and `onMuteStateChanged`; requests are made using Android's `requestCallEndpointChange` and the actual endpoint supplied by Android.
3. **Error handling and state.** Route-switch errors are shown on the call UI; a user request does not assume that a device switched before Android confirms it.
4. **Voice capabilities only.** The Settings panel checks `NoiseSuppressor.isAvailable`, `AcousticEchoCanceler.isAvailable`, `AutomaticGainControl.isAvailable` without creating any audio-session effects or requesting microphone capture. These capabilities are labeled as NOT ACTIVE for standard SIM calls.
5. **Preserved regression coverage.** Tests continue to cover default SIM route-selection logic, keypad, incoming call UI settings, Back navigation, call history directions and answered-call recovery.

## Results / verification sources
- **Unit tests and APK build:** [GitHub Actions build](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml) — passed for the audio-routing change commit.
- **Android Lint:** [GitHub Actions quality check](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-lint.yml) — passed for the audio-routing change commit.
- **Cross-version emulators:** [Android 6 / 8 / 15 workflow](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-cross-version-tests.yml). Check that workflow's latest run for the exact v1.8.0 commit; device audio tests are not available in this emulator.
- **Release build:** re-runs unit tests and Lint; checks APK signing, `com.scall.phone` ID, version `1.8.0`, minSdk `23`, ZIP integrity and SHA-256 of the release APK.

## Not tested, not claimed
- Actual call speech recording/transmission quality on physical phones.
- Noise suppression under traffic, wind, crowd, echo and fan conditions.
- Speaker/Bluetooth/wired route switching with physical devices.
- Carrier HD Voice activation, real VoIP audio enhancement, end-to-end latency or battery effect.
- Full real-device matrix of Samsung/Xiaomi/OPPO/Vivo/Pixel/other OEM dialers.

These tests need real physical handsets, supported cellular networks and, for VoIP work, an app-owned audio pipeline. **No fake AI activation badge or microphone interception has been implemented.**

**Signing:** Debug signing certificates created on separate GitHub Actions runners may differ; older Preview APKs may require uninstalling first, which can erase local app preferences. Back up important settings and keep the original phone dialer available for emergencies.
