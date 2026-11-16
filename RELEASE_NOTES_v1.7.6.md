# S call v1.7.6 Preview — Android 6+ Cross-Version Support

**Goal:** make S call install and run on a much wider range of Android devices, while respecting Android Telecom and each phone's settings.

## Compatibility updates
- **Minimum Android reduced from 8.0 to 6.0** (`minSdk 23`), the first Android version providing the full InCallService API needed by a default phone app.
- **Android 6 and 7:** incoming and ongoing call alerts use NotificationCompat's high-priority legacy notification pathway; Android 8+ continues to use a high-importance notification channel.
- **Android 6:** three Android 7+ SIM-default APIs are guarded; unavailable default SIM IDs return "Android default/unknown" instead of crashing or selecting another SIM.
- **Android 6/7:** notification settings open the app's Android settings screen instead of an Android 8+ channel page.
- **Tablets without cellular hardware:** calling attempts show a clear unsupported-device message instead of crashing; Telecom service lookup is null-safe.
- **Android 13+:** post-notification runtime permission handling retained.
- **Android 14+:** full-screen incoming-call permission checking retained.
- **Older and newer phones:** the light-green icon, high-contrast call symbols, adaptive keypad, SIM preferences and call history controls remain.

## Verified emulator platforms
- Android 6.0 / API 23 — compatible UI startup, keypad, Back navigation and settings smoke tests.
- Android 8.0 / API 26 — same automated smoke tests.
- Android 15 / API 35 — same automated smoke tests.
- Android 10 / API 29 — existing emulator smoke test workflow.

GitHub Actions also runs unit tests, Android Lint and signing/APK-integrity checks.

**Important:** Not every device has a SIM, supports default-dialer replacement, or exposes the same dual-SIM/account-routing APIs. Emulator tests do not confirm real carrier calling; particular phone models, OEM interfaces, Android security settings and physical SIM setups can behave differently. Android older than 6.0 is not supported because InCallService begins at API 23. Newer Android releases may also need updated validation.

## Download
- **[S-call.apk — direct APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.6-preview/S-call.apk)**
- **[ZIP fallback](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.6-preview/S-call-v1.7.6-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.6-preview/S-call-SHA256SUMS.txt)**

This is a **debug-signed preview**. Different GitHub Actions builds may use different debug signing certificates; back up any app-specific caller-photo/customization data before uninstalling older builds. Keep the phone's built-in dialer available, particularly for emergency calling.
