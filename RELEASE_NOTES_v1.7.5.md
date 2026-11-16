# S call v1.7.5 Preview — Incoming Call Screen & Ringing Alert Fix

### Main fix: phone rings, but caller screen does not appear
The original `InCallService` posted a call notification only when Android first added a call. Some devices report a different initial state and update to **RINGING** a moment later. The old implementation did not update the notification when this happened, so only ringtone audio could be heard.

**Updated:**
- **Android Telecom state observer** refreshes the call alert when a new call becomes **RINGING**, and when caller details change.
- Proper **high-priority CallStyle** notification with caller number/name, **Answer / Decline**, and a full-screen intent for appropriate situations.
- **Android 14+ full-screen notification access** now has an in-app check and **Fix** shortcut.
- **Notification channel/permissions** status is visible under **S call → Settings → Incoming call display**.
- Clearer call screen: **PHONE IS RINGING** heading, caller information, and existing swipe/tap Answer/Decline controls.
- Prevents double notification ringtone: Android Telecom manages the actual ringtone.

### Android behavior
With the proper permissions, **unlocked phones normally display a heads-up incoming-call notification**, while **locked/off-screen phones may launch the full-screen call UI**. Android and device manufacturers control this behavior. If a user disables notification alerts or full-screen access, S call cannot bypass that choice. See Android's [official FSI guidance](https://source.android.com/docs/core/permissions/fsi-limits).

### Setup after installation
Open **S call → Settings → Incoming call display → Fix** and follow Android permission screens, especially **Default Phone**, **Notifications**, and **Full-screen notifications** on Android 14+.

### Direct downloads
- **[S-call.apk — direct APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.5-preview/S-call.apk)**
- **[ZIP fallback](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.5-preview/S-call-v1.7.5-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.5-preview/S-call-SHA256SUMS.txt)**

### Verification and limitations
The Android workflow checks unit tests (including new incoming notification readiness rules), Lint, APK compilation, identity, version, signing, ZIP and SHA-256 integrity. Emulator smoke tests verify the user interface can be opened and navigated.

**Physical incoming cellular calls have not been tested here.** The behavior must be verified with a real phone/SIM and different lock-screen states. This is a debug-signed preview, not a production dialer. Back up app-specific data before uninstalling older debug versions; signing certificates may differ. Keep the built-in Phone app available especially for emergency use.
