# S call v1.2.0 — Smart SIM & Smooth Calls Preview

## New

- Smooth horizontal swipe-to-answer (right) and swipe-to-decline (left), with threshold feedback, thumb movement following the finger and spring return.
- Improved swipe right on contact to call and left to message, including accidental swipe protection, clear status icons and haptic feedback.
- Voice calls can use a saved Telecom phone account, or ask which line to use each time. Saved preferences use the account identity rather than arbitrary list position.
- SIM & network preferences inside S call: default outgoing call line, messaging SIM suggestions, Android system SMS and mobile-data SIM status, shortcut to Android's own mobile-network settings.
- SMS suggestions open the phone's messaging app. **The actual SIM used to send SMS depends on that app and must be checked there.**
- The mobile-data SIM **cannot be changed directly by a normal dialer**; use Android system Settings.
- Display Telecom account label in the active calling UI, where supported.
- Same minimalist S call icon, light/dark/AMOLED themes and HD caller-photo editor as v1.1.

## Test preview

This is an unsigned-by-Play-Store **debug-signed APK**, built with Android Gradle and made available for testing. A successful build **does not verify** real cellular calls, carrier services, lock-screen UI, SIM management or hardware reliability on your phone. For emergency calls, use the device's preinstalled phone app until testing is complete.
