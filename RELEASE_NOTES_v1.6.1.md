# S call v1.6.1 — Download & Packaging Fix Preview

This release improves how **S call** is delivered and verified. The in-app experience retains the v1.6 tactile keypad and corrected Back navigation, the mint-green icon, call history manager, SIM controls, and smooth call sliders.

## Choose a download

**Direct APK:** [S-call.apk](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.6.1-preview/S-call.apk)

**ZIP fallback:** [S-call-v1.6.1-install-package.zip](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.6.1-preview/S-call-v1.6.1-install-package.zip)

Both contain the **same Android APK**. Use the direct APK first; try the ZIP if your browser or ChatGPT attachment link does not download normally.

## Verification

The release workflow checks:
- Kotlin/Android unit tests (`testDebugUnitTest`)
- Android APK compilation (`assembleDebug`)
- APK ZIP integrity (`unzip -t`)
- Android package identity `com.scall.phone` and **version 1.6.1**, using Android SDK tooling
- APK SHA-256 checksum; the fallback ZIP includes a checksum file
- ZIP integrity and the presence of the APK before publication

The release is based on the tagged GitHub source, not on an older build artifact.

## Install help

See the attached ZIP's `INSTALL_S_CALL.txt` and the [step-by-step guide in the repository](https://github.com/saurabhsahu20-svg/aura-dialer-/blob/main/INSTALL_S_CALL.txt).

**Debug-signed preview warning:** Installing on top of another debug APK may fail if its signing certificate differs. Back up caller photos and app preferences before removing older builds. Do not rely on an untested alternative dialer for emergency calls. Actual device, SIM and lock-screen calling behavior remains unverified.

## App version
- Name: S call
- Version: 1.6.1
- Version code: 9
- Package: `com.scall.phone`
- Android minSdk 26 (8.0)
