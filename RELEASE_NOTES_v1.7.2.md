# S call v1.7.2 Preview — Tested APK with Download Backup

This update focuses on **reliability**, without changing your familiar S call appearance.

## Quality and stability improvements
- Fixed Android 8.0 call-screen lock-screen compatibility.
- Strengthened Telecom/SIM permission checks.
- Added Android emulator UI smoke tests for the Keypad, Back navigation, Recents and SIM Settings. **4/4 passed**.
- Android Lint quality gate passed, resolving four blocking issues.
- Kotlin/JUnit automated tests cover core routing and UI interaction logic.

## Download verified APK
- **[S-call.apk — direct APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.2-preview/S-call.apk)**
- **[S-call-v1.7.2-install-package.zip — ZIP backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.2-preview/S-call-v1.7.2-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.2-preview/S-call-SHA256SUMS.txt)**

The ZIP contains S-call.apk, the checksum, installation instructions and this QA report. GitHub Actions checks the signed APK, package/version and archives before publishing.

## Existing features preserved
Light-green launcher icon, hidden SIM banner with toolbar SIM settings, interactive dialpad, long smooth swipe controls, full-screen caller photo editor, caller ID and call history management.

## Important
This is a **debug-signed preview, not a production release**. Physical phone cellular calls and device-specific incoming-call controls have not been tested. Keep the stock dialer for emergency use. Old debug APK signatures may differ and require backup before uninstall.
