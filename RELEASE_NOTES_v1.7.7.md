# S call v1.7.7 Preview — Reliable Screen After Answering

The user-reported problem was an incoming notification with **Answer / Decline**, followed by a mostly empty S call screen stuck on **"Connecting to phone service…"**.

## Call-screen reliability changes

- **Direct Answer-to-Activity navigation**: notification's Answer PendingIntent now launches the real call Activity directly and invokes Android Telecom's Answer action there. This avoids the Android 12+ notification broadcast-to-activity trampoline restriction.
- **Recover live Telecom call objects**: opening the call screen attempts to reconnect the app's CallBridge to existing calls already provided by Android Telecom; it does not fabricate a connected-call state.
- **Fix service-rebinding race**: when an older InCallService is destroyed after a replacement has bound, it can no longer erase the new service's live call state.
- **Correctly dismiss ended calls**: after Telecom removes a call, the call UI closes instead of showing an endless loading state.
- **Better fallback**: if a call screen is opened without a live Telecom call, it shows **"Phone service not connected"**, a **Retry call screen** action and instructions to verify S call is the default phone app. The spinner is no longer indefinite.
- **Preserved smooth calling UI**: full-screen caller images, incoming swipe controls, in-call mute/speaker/keypad/End Call, SIM selection, call history, green app icon and high-contrast icons remain.
- **Android 6+ support** retained.

## Download
- **[Direct APK — S-call.apk](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.7-preview/S-call.apk)**
- **[ZIP backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.7-preview/S-call-v1.7.7-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.7-preview/S-call-SHA256SUMS.txt)**

## Validation
- Automated unit tests including service-lifecycle regression tests.
- Android Lint, APK compilation, signing, APK version/minSdk and ZIP checksum checks in the release workflow.
- Android emulator UI smoke tests include a **no-live-call recovery** test; emulator does not simulate a carrier SIM call.

**Important:** Actual carrier calls on the user's phone and OEM lock-screen behavior cannot be verified from GitHub Actions. This is a **debug-signed preview**, not a production-ready replacement for your phone's stock dialer. Keep the stock dialer available, particularly for emergency calls. Back up local app preferences before uninstalling older debug builds; certificates may differ across builds.
