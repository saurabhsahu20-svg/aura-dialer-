# S Caller v1.9.23 — Keypad viewport correction

All features and visual branding remain unchanged. This revision corrects a known Android 6/8/10/15 emulator failure in the v1.9.22 keypad layout test. Compose verticalScroll measures content with unbounded height: the previous Arrangement.SpaceBetween could not fill the viewport, creating a ~532dp gap under the last digit. The keypad now calculates bounded spacing between its four digit rows. Small device layouts retain vertical scrolling. 

Carries forward the v1.9.22 fixes: faster T9 contact suggestions, cross-country contact-number isolation, safe partial call-history deletion, direct WhatsApp/Telegram chat shortcuts, accurate full call history, expandable action menus and guarded end-call retries.

Require unit tests, Android Lint and emulator smoke tests; don't describe an APK as bug-free without physical call verification. Signing is still CI debug-generated and may not upgrade a differently signed version.
