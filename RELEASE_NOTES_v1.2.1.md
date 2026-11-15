# S call v1.2.1 Preview — Latest Smart SIM & Smooth Calling APK

This preview includes the S call v1.2 features plus the latest Android build and safety improvement.

## What's included

- **Preferred SIM for voice calls:** choose a specific Android Telecom calling account or **Ask Every Time**.
- **Safer SIM handling:** if your chosen SIM is unavailable, S call will **not automatically switch to the other SIM**; it asks you to check the SIM settings or choose an available line.
- **Smooth contact swipe:** swipe right to call, left to message, with thresholds, haptic/visual feedback and spring return.
- **Improved incoming call control:** sliding answer/reject interactions and accessible tap alternatives.
- **SMS SIM guidance:** optional selection prompt and messaging app handoff. The messaging app still decides which SIM actually sends a text.
- **Mobile data settings:** convenient Android network settings access. Third-party dialers cannot directly change the default mobile-data SIM.
- Full-screen caller photo editor, clean typography, official S call icon, contacts, recents, favorites, dialpad, Light/Dark/AMOLED modes.

## Build details

- Android application ID: `com.scall.phone`
- Version name: `1.2.1`, version code: `4`
- APK: `S-call-v1.2.1-preview.apk` (debug-signed)
- Built from the exact GitHub commit referenced by the release.
- Android CI runs unit tests and a Gradle debug APK build before publishing.

## Install and testing

1. Download the attached APK to an Android 8+ phone.
2. Install it, grant necessary permissions and choose S call as the default phone app if you want to test incoming call handling.
3. In **S call → Settings → SIM & Network**, set the calling SIM or Ask Every Time.
4. Test SIM 1 and SIM 2 separately for outgoing calls, and incoming call controls with another phone.

**IMPORTANT:** Previous preview APKs may use a different debug signing certificate. An installation-over-existing-app error may require uninstalling the older preview first, which can remove app-specific settings and photo preferences. Back up any important app data first.

**NOT PRODUCTION TESTED:** GitHub CI success is not proof of real cellular-call, multi-SIM, lock-screen, SMS or hardware reliability. Keep the device's stock phone app available, particularly for emergency calls. SMS SIM is controlled by the messaging app; mobile-data SIM selection is handled by Android system settings.

Source: https://github.com/saurabhsahu20-svg/aura-dialer-
