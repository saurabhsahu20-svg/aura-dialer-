# S call v1.7.3 — Responsive Keypad QA Report

**Version:** 1.7.3 (version code 13)  
**Package:** `com.scall.phone`  
**Minimum Android:** 8.0

## Fixed issue
The uploaded screenshot showed unused space between the `* 0 #` row and the bottom Call controls. The keypad used four fixed 75dp rows even on tall screens.

Each digit row now dynamically uses available phone height for balanced, near-square buttons. On short screens or with matching contact suggestions, number keys remain at least 72dp high and the keypad can scroll. The Call, Message and Backspace actions remain pinned to the bottom.

## Tests completed before release
- **Kotlin/JUnit unit tests:** passed, including new adaptive keypad tests for tall, short, matching-contact and invalid display heights.
- **Android Lint:** passed — [run 43743279170](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43743279170).
- **Android debug APK build:** passed — [run 43743279195](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43743279195).
- **Android 10 emulator UI smoke tests:** 5/5 passed — [run 43743279209](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43743279209). Includes a test measuring the distance between the final dial key and Call button; the previous large gap is absent.
- The release workflow again checks tests, Lint, APK build, Android package/version/signature, ZIP integrity and SHA-256 checksum against the v1.7.3 source.

## Testing limitation
The emulator cannot confirm real carrier, SIM 1 / SIM 2, incoming/ongoing cellular calls or a physical device's lock-screen behavior. This is a **debug-signed preview**. Debug signing certificates may differ across builds; back up app-specific data before replacing/uninstalling older previews. Keep the stock phone app for emergency calling.
