# S Caller v1.9.32 — Incoming Calls Vibrate But Caller Screen Missing

## Reported behavior
Sometimes the device vibrates/rings but S Caller does not display who is calling or the Answer / Decline UI.

## Fixes
1. Do not publish an "ongoing" notification for `Call.STATE_NEW`, `STATE_DISCONNECTING`, or other unready states delivered before `STATE_RINGING`. Such early notifications could reuse the same ID and suppress a fresh full-screen incoming call alert on some Android versions.
2. Use a **dedicated incoming notification ID (4102)** separate from ongoing calls (4103). A transition into RINGING generates a fresh high-priority incoming CallStyle notification and full-screen intent. Cancel the previous type only as part of the type switch. Do not accidentally cancel a live second call because an unrelated older callback is disconnected.
3. When an OEM delays or skips a Telecom state callback, briefly recheck active call states at 120/350/800/1500ms, and refresh the notification. Rechecks stop when the call is removed, and all pending tasks are cleared when the service ends.
4. Continue to offer a functional notification with **Answer / Decline** actions even if a vendor CallStyle implementation throws an exception. The full-screen PendingIntent is still attached.
5. Initialize the call alert notification channel on initial app launch, without overriding a user-customized channel. Detect missing/blocked channels accurately; a blocked channel must not be reported as READY.
6. On all main tabs, show a small notification warning icon (without moving the keypad) if Android call alert settings need attention. Tap it to open S Caller settings, then the Android settings for notifications or full-screen access.
7. Regression tests cover initial not-yet-ringing state, late transition to ringing, incoming priority over ongoing, and ignoring disconnected callbacks.

## Android restrictions and device setup
- Android may show a heads-up notification instead of full-screen Activity while the device is unlocked. The platform controls this behavior.
- For Android 14+, check Settings > Apps > Special app access > Full screen intents for S Caller, plus app notification and incoming call channel settings. Set S Caller as the default Phone app.
- Notification channel or full-screen permissions explicitly denied by the user cannot be bypassed by the app. No notification should be falsely described as delivered without a device test.
- The user should retain a working stock Phone app for emergency use while testing.
- Real mobile-network, physical ringtone/vibration and locked-screen tests require an actual phone. Emulator success does not guarantee every handset.

## Existing features preserved
- Hang-up touch reliability, proximity protection, exact contact name matching, stable keypad, missed badge, call history, SMS/WhatsApp/Telegram integrations.
- Package `com.scall.phone`; version `1.9.32`, versionCode `132`.
- The older v1.9.21 signing certificate mismatch remains: a debug build signed with a different key cannot be installed in-place over the old app.
