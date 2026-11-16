# S call v1.7.4 — High-Contrast Call Icons QA Report

**Version:** 1.7.4 (versionCode 14) | **Package:** `com.scall.phone` | **Android:** 8.0+

## Visible change requested

Incoming, Outgoing and Missed symbols in Call History were too thin and difficult to recognize on Light Theme.

- Incoming: **Dark Green** `#006B3E` (white contrast **6.62:1**)
- Missed: **Dark Red** `#A8172B` (white contrast **7.44:1**)
- Outgoing: **Dark Blue** `#164B9F` (white contrast **8.26:1**)

Arrows now have bold rounded canvas strokes rather than tiny material icons, 20dp row symbols, 19dp avatar symbols inside 30dp bordered badges, and **ExtraBold** direction labels. Filter chips use the same stronger symbols and type. A custom kinked arrow differentiates Missed from Incoming and Outgoing.

Dark Theme keeps high-contrast brighter greens, reds and blues without sacrificing visibility.

## Verification

- **Kotlin/JUnit checks** added for three distinct direction colors and light-theme contrast **>= 6.0:1**, plus the existing call classification, SIM, dialpad and navigation tests.
- **[Android build + unit tests](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43786142541): passed** on the change commit.
- **[Android Lint](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43786142527): passed**.
- The v1.7.4 release workflow repeats unit tests, Android Lint, APK compilation, signature check, package/version check, APK/ZIP archive integrity and SHA-256 verification.
- Existing Android emulator UI smoke tests are also executed on the bumped version. They cover startup, keypad, Back and SIM settings; this doesn't replace physical phone testing.

## Limits

Real cellular SIM calling, lock-screen incoming calls and actual users' contacts/call logs have not been tested on physical devices. This is a **debug-signed preview**, not a store release. Signing keys can differ across GitHub Actions preview builds; back up app settings/caller photos before uninstalling prior previews. Keep your stock phone app available for emergency calls.
