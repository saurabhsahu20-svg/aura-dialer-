# S Caller v1.9.33 — Incoming Caller Visibility

## Reported issue
The phone sometimes vibrates for an incoming cellular call but does not show the caller's name/number or incoming Answer/Decline controls.

## Changes
- The app's incoming-call readiness check now requires a **HIGH-priority Phone calls notification channel**. A channel set to LOW or DEFAULT can allow vibration but may not show a heads-up caller alert. S Caller cannot silently override a user-modified Android channel; its existing Settings > Incoming call display > Fix control leads to the platform notification-channel settings.
- Android 14+ full-screen notification permission continues to be checked and links to Android's Full-screen alerts permission. Android, the phone manufacturer's notification settings, lock-screen rules and user permissions ultimately control whether a full-screen Activity may launch.
- Resolve saved contact names using an asynchronous Contacts Provider lookup on an incoming call. Show the phone number immediately until the name is available; then update the existing CallStyle notification without blocking the incoming call.
- Retain existing onCallAdded/onStateChanged notification retry logic, lock-screen intent, Answer/Decline actions, call screen, keypad, contact matching, missed-call badge and Hang Up fixes.
- Unit regression tests verify low/default/high channel priorities and full-screen permissions readiness.
- Package name unchanged, v1.9.33 (version code 133).

## Limits
This is a bug-fix preview pending real cellular network and device-specific call/notification tests. A passing Android emulator test cannot guarantee Android OEM full-screen behavior. On the test device, choose S Caller as the default Phone app, enable notifications for S Caller, set the Phone calls channel to High / Pop on screen, and enable Full-screen alerts where available.

APK signing certificate must match an existing installed copy for an in-place update; an unrelated debug signing key will not update v1.9.21 without uninstalling. Preserve data and have the phone's standard dialer available as fallback.
