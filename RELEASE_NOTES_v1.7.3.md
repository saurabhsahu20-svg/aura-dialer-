# S call v1.7.3 Preview — Full-height Adaptive Keypad

This version fixes the empty space below the 12 dialpad numbers shown in the screenshot.

## Improvements
- **Responsive, balanced 1–9 / * 0 # keys** expand to fill available height instead of being limited to 75dp rows.
- Spacious near-square key targets, consistent 3-column arrangement and existing tactile feedback.
- Compact-screen support: minimum 72dp keys and vertical scrolling if contact suggestions take additional space.
- The bottom Call, Message and Delete controls stay visible.
- Preserves the light-green icon, preferred SIM, swipe calling, editable call history and Android Back behavior.

## Downloads
- **[Direct Android APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.3-preview/S-call.apk)**
- **[ZIP fallback](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.3-preview/S-call-v1.7.3-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.3-preview/S-call-SHA256SUMS.txt)**

**QA:** Android unit tests, Android Lint and emulator UI smoke tests all passed; the emulator suite includes a new test that rejects an excessive gap below the digit keys (5/5). The release build also verifies APK identity, version, signature, and ZIP/checksum integrity. Refer to `QA_REPORT_v1.7.3.md`.

**Important:** Debug-signed preview, not a physically tested cellular dialer. Real SIM calls and lock-screen scenarios require device testing. Keep the stock dialer for emergency calls.
