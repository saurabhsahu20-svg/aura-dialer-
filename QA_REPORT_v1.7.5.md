# S call v1.7.5 — Incoming Call UI QA Notes

**App:** S call / `com.scall.phone`  
**Version:** 1.7.5 (versionCode 15)  
**Minimum Android:** 8.0 (API 26)

## Defects addressed
1. `SCallInCallService` now observes `Call.Callback.onStateChanged` and `onDetailsChanged`, updating the real Telecom call notification **when a call transitions to RINGING** instead of only on `onCallAdded`.
2. Android call alerts use `NotificationCompat.CallStyle` and high-priority incoming-call notification, with visible Answer/Decline action buttons and a full-screen PendingIntent.
3. New setup checks test whether the user has granted the default Phone role, notification permission, notification channel, and Android 14+ full-screen access. A **Fix** button opens appropriate Android settings.
4. Improved incoming call screen visual clarity, with prominent ring status.
5. The notifications use a dedicated monochrome status icon. Android Telecom remains responsible for the audible ringtone.

## Automated testing
- 5 new notification-readiness unit tests cover default dialer, notifications, blocked channel, full-screen permission, and ready state.
- Existing call classification, SIM, navigation, dialpad and swipe unit tests are retained.
- Verified Kotlin/JVM unit tests, Gradle APK build and Android Lint for the source changes before release.
- The v1.7.5 GitHub release build repeats all unit tests, Lint and APK archive/signature/package/version checks.
- Android emulator UI suite tests keypad, navigation, and new incoming-display Settings card.

## Known limitation
An emulator has no physical carrier incoming-call signaling. Automated tests cannot confirm real SIM ringing, heads-up ranking, lock-screen full-screen display, or an OEM's notification settings. Users should verify incoming-call behavior on a real Android phone with S call explicitly selected as the default Phone app, granting all required alerts/permissions. If only a ringtone is heard, inspect **S call → Settings → Incoming call display**.

This is a **debug-signed preview**, not Play Store production-ready. Keep the preinstalled dialer available for emergency calls. Save app data/preferences before removing older preview builds.
