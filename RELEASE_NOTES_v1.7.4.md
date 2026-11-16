# S call v1.7.4 — Bold, Easy-to-See Call Symbols

This update refreshes all **Incoming, Outgoing and Missed** symbols with **thicker arrows, stronger colors and larger badges**, especially for Light Theme.

## Before → After

| Type | New Light Theme color | Icon |
| --- | --- | --- |
| **Incoming** | **Dark Green `#006B3E`** | Bold down-left received arrow |
| **Missed** | **Dark Red `#A8172B`** | Bold kinked/missed-call arrow |
| **Outgoing** | **Dark Blue `#164B9F`** | Bold up-right made arrow |

- New 3-point rounded bold strokes, enlarged **Call History badges** and matching filter icons.
- Text labels now appear in **ExtraBold** and in the matching dark color.
- High-contrast colors work in Light Theme; bright variants continue in Dark/AMOLED mode.
- All prior keypad, caller photo, SIM, swipe and call-history features remain unchanged.
- No changes to the meaning of your real call records.

## APK downloads

- **[Direct APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.4-preview/S-call.apk)**
- **[ZIP backup package](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.4-preview/S-call-v1.7.4-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.4-preview/S-call-SHA256SUMS.txt)**

## Testing

Automated Android unit tests, explicit color contrast checks and Android Lint passed. GitHub Actions builds the versioned APK and checks package name/version, APK signature, ZIP and SHA-256 checksum. Emulator UI tests check basic navigation independently.

**Preview limitation:** Not yet verified with physical phone carrier/SIM incoming/outgoing calling. This APK is debug-signed; older preview signing certificates may differ. Back up any important app preferences before uninstalling an older version. Keep the stock phone dialer for emergencies.
