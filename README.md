# S call — Android Dialer (v1.6.1 Verified Download Preview)

**S call** is a privacy-focused Android phone dialer built with Kotlin and Jetpack Compose. It features a minimal Material 3 interface and the approved light-green S call launcher icon.

[![Build S call debug APK](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml/badge.svg)](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml)

## 📥 Download S call v1.6.1 — Direct APK + ZIP backup

### **[⬇️ Download S-call.apk — DIRECT ANDROID APP](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.6.1-preview/S-call.apk)**

### **[⬇️ Download ZIP — fallback if APK download fails](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.6.1-preview/S-call-v1.6.1-install-package.zip)**

**[Official GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.6.1-preview)**

This build passes Android unit tests and package/ZIP integrity checks, with the expected S call package and version. If an in-chat file link fails, open the **official GitHub Release** in your mobile browser and tap `S-call.apk` under **Assets**. You can also extract `S-call.apk` from the ZIP fallback. Full instructions are in [INSTALL_S_CALL.txt](INSTALL_S_CALL.txt).

**Important:** This is a debug-signed preview. Its signing certificate may differ from older GitHub Actions builds; installing it over a previous preview could require uninstalling that version, which can remove app-specific settings. Back up data first. Physical phone/calling tests remain outstanding.

## Previous preview — v1.6.0

## ⌨️ Latest — S call v1.6.0 Interactive Keypad & Smart Back

**[Download the latest S call v1.6.0 Android APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.6.0-preview/S-call-v1.6.0-preview.apk)**

**[Open S call v1.6.0 GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.6.0-preview)**

- **Premium tactile keypad:** interactive animated number keys with subtle haptic feedback.
- **Better back behavior:** Android Back now navigates to the preceding screen rather than unexpectedly closing the app; Keypad includes its own visible back arrow.
- **Smart contact suggestions:** number/T9 lookup; tap to fill the dialed number.
- **Quick controls:** paste, clear, message, call, long-press zero for +, long-press delete to clear.
- **SIM shortcut on Keypad:** quick access to calling SIM preferences.
- The Call button remains accessible even when the keypad scrolls on small displays.
- Retains light-green icon, SIM settings, smooth call sliders, photo editor and call-history manager.

**Testing:** Automated keypad/back navigation/gesture tests and APK build verified by GitHub Actions. Physical-device calling and back-gesture tests are still pending. The APK is debug-signed for testing, not a production Play Store release.

## Previous release — v1.5.0

## 📲 S call v1.5.0 SIM Setup & Smart Calling

**[Download S call v1.5.0 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.5.0-preview/S-call-v1.5.0-preview.apk)**

**[Open S call v1.5.0 GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.5.0-preview)**

- A visible **SIM & Network** quick-access card on Recents, Contacts, Keypad and Favorites.
- Shows **SIM 1 / SIM 2** subscriptions, labels, and missing-phone-permission/default-dialer instructions.
- Refreshes SIM detection when returning from Android Settings.
- Offers **Ask each time**, **Android default**, or an available, exact Telecom calling account.
- Tests prevent silently falling back to a different SIM when the chosen line disappears.
- Preserves mint-green icon, smooth slide-to-call, caller photos and call-history management.

**Android restrictions:** SIM subscriptions are not necessarily exposed as selectable Telecom phone accounts. SMS sending SIM and default mobile-data SIM remain controlled by Android or the messaging app. **This APK is a debug-signed preview for device testing.**

## Previous Release — v1.4.0

## 📞 Latest — S call v1.4.0 Call History Manager

**[Download S call v1.4.0 Preview APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.4.0-preview/S-call-v1.4.0-preview.apk)**

**[View v1.4.0 GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.4.0-preview)**

- **Edit history:** Save an individual call's custom display name and private note locally, without changing real Android call details.
- **Delete one call:** Each recent call has a Delete control with an irreversible-action confirmation.
- **Bulk delete:** Select multiple calls or select all filtered entries, then explicitly confirm deletion.
- **Safe system integration:** Uses exact Android call-log entry IDs and requires the default-phone role plus `WRITE_CALL_LOG`. Reports blocked deletion honestly.
- **Existing features preserved:** mint-green icon, longer smooth swipe gestures, SIM preferences, photo editor and dark/light themes.

**Note:** This is a debug-signed preview. Physical phone tests remain necessary for call history deletion and in-call behavior.

## Previous release — v1.3.0

## 🟢 Latest — S call v1.3.0 Longer Swipe & Mint Green Icon

**[Download S call v1.3.0 Preview APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.3.0-preview/S-call-v1.3.0-preview.apk)**

**[View v1.3.0 Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.3.0-preview)**

- **Contact swipe-to-call/message:** drag at least **68% of the full contact row width** before releasing; shorter swipes are cancelled.
- **Incoming-call answer/decline:** slide the central thumb at least **82% of its travel toward the chosen action**; incomplete swipes spring back with no action.
- **Smooth, no-bounce release** and one haptic confirmation on reaching the threshold.
- **New mint/light-green S call icon** across standard launcher icons and adaptive icons.
- Keeps SIM 1 / SIM 2 calling preference, full-screen caller photo, contacts, recent calls and themes.

Android preview build and automated gesture unit tests are verified by GitHub Actions. **Real-phone call and lock-screen behavior still require physical-device testing.** This is a debug-signed testing APK.

## Previous release — v1.2.1

## ✅ Download — S call v1.2.1 Preview

**[Download latest S call v1.2.1 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.2.1-preview/S-call-v1.2.1-preview.apk)**

**[GitHub v1.2.1 Release and installation notes](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.2.1-preview)**

This version retains smooth call-answer and swipe-to-call gestures, SIM 1 / SIM 2 preference, and adds a safeguard: **if the preferred calling SIM becomes unavailable, the app does not silently place calls on another SIM**. Android still controls the real mobile-data SIM and the messaging application controls the actual SMS SIM.

Built with GitHub Actions (JDK 17, Android SDK 35, Gradle 8.10.2) with automated gesture unit tests. Preview APK is debug-signed; real-device testing remains required.

## Previous Preview — v1.2.0

## ✨ NEW — S call v1.2.0 Smart SIM & Smooth Calling

**[⬇️ Download S call v1.2.0 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.2.0-preview/S-call-v1.2.0-preview.apk)**

**[View S call v1.2.0 Preview Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.2.0-preview)**

- **Calls:** Pick a particular Telecom phone account, or ask before each call. Applies to the keypad, call shortcut and contact-name swipe; if the selected line is unavailable, the app asks again rather than silently using the other line.
- **SMS:** Suggest a SIM or ask each time, then check the actual SIM in the messaging app. S call doesn't send SMS itself and cannot force that app to honor the preference.
- **Mobile data:** View Android's data SIM and open Android network settings to choose another. Standard third-party dialers cannot directly change the system's mobile-data SIM.
- **Swipe controls:** Smoother touch-following animations, spring-back, thresholds, clearer haptic and visual feedback, and accessible tap alternatives for incoming-call accept/decline.
- **Accuracy:** Automated threshold unit tests and Android APK build have passed in [GitHub Actions](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43705223431).

**Preview warning:** The APK is debug-signed; incoming calls, SIM routing and lock-screen UI still require testing on a real Android phone.

## Previous Preview — S call v1.1

## 📸 NEW — S call v1.1.0 Caller Photo Studio

**[⬇️ Download S call v1.1.0 Android APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.1.0-preview/S-call-v1.1.0-preview.apk)**

**[View S call v1.1.0 Photo Studio Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.1.0-preview)**

**What's new:** Full-screen caller photo live preview, gallery selection before saving, pinch-to-zoom, drag positioning, customizable gradient tone, Reset and Save look, plus one-tap caller-photo shortcut in Contacts and Favorites. Typography has been refined with Android's lightweight offline sans-serif family.

**Build:** [v1.1.0 GitHub Actions success](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43689815334). Debug-signed preview APK; physical phone/lock-screen calling tests are still required.

## Previous Preview — S call v1.0

## 📲 Download S call v1.0 Preview

**[Download Android APK (GitHub Release)](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.0.0-preview/app-debug.apk)**

**[View S call v1.0 Preview Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.0.0-preview)**

The APK is **debug-signed**, and is provided for evaluation. **GitHub Actions compilation passed**, but physical-phone call handling and lock-screen behavior still require testing before everyday use.

## What's implemented in the source

- Native Kotlin + Jetpack Compose, Android 8+ (min SDK 26), target SDK 35.
- Android default phone role request (`RoleManager.ROLE_DIALER`; legacy fallback).
- Required `ACTION_DIAL` intent filters and `InCallService` registration.
- Telecom-backed incoming, outgoing, active-call flows: answer, decline, end, mute, speaker, DTMF keypad, conditional hold, merge and add-call controls.
- Full-screen caller screen with custom contact photo, dark gradient overlay and swipe-to-answer/reject; accessible tap actions as an alternative.
- Contacts, Favorites, Recents, searchable lists, T9 dial pad, contact details.
- Swipe **right on a contact row to call**; swipe **left to message**; easy on/off setting.
- Dual-SIM account chooser when Android exposes more than one calling account.
- Phone's system image document picker for persistent caller photo selection.
- Light, Dark, AMOLED, System appearance themes.
- Notification actions and full-screen intent for incoming calls (subject to Android policy and permission).

## Build the debug APK

### With GitHub Actions (recommended if you lack a local Android SDK)

1. Open this repository's **Actions** tab.
2. Choose **Build S call debug APK** and select **Run workflow**.
3. After a successful run, download **S-call-debug-apk** under Artifacts.
4. The ZIP artifact contains `app-debug.apk`.

A v1.2.0 APK build and swipe unit tests were verified in [this workflow run](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43705223431).

The source project and the downloadable APK are separate files.

### With Android Studio

1. Open this folder in Android Studio (JDK 17, Android SDK platform 35).
2. Let Android Studio configure Gradle 8.10.2, Android Gradle Plugin 8.7.3, and synchronize the project.
3. Run Gradle task `:app:assembleDebug`.
4. Find the debug APK at `app/build/outputs/apk/debug/app-debug.apk`.

Note: The source bundle does not include the Gradle wrapper JAR. Android Studio can use a configured Gradle installation, or run with Gradle 8.10.2 installed. GitHub Actions installs Gradle automatically.

## Setup on an Android device

1. Install the APK and launch **S call**.
2. Choose **Set up** to grant the **default phone app** role; grant phone, contacts, call-log, and notification permissions as appropriate.
3. Open Contacts or Recents, or use Keypad.
4. Open a contact and choose **Caller photo** to select a long-term image.
5. Test incoming calls with a second device, particularly lock-screen presentation.

> Emergency calls are routed by Android Telecom; the device's preinstalled phone app handles them according to Android rules. Never rely exclusively on an untested custom dialer for urgent/emergency calling.

## Important caveats

- This is a **preview implementation**: it **compiled successfully in GitHub Actions**, but has **not been tested on a physical Android phone**. Verify calling and incoming-call behavior before everyday use.
- A default dialer cannot promise call recording, online unknown-number identification or full-screen behavior under every OS, permission and carrier combination. These services are **not included** in this build.
- Photo selection is supported; pinch-to-crop and repositioning editor are future enhancements.
- Android 14+ may limit full-screen intent activation. Users may need notification/full-screen intent approval; Android chooses when to present heads-up instead of full-screen UI.
- Some features (dual SIM, merge, hold, audio routing) are phone/carrier specific.
- Android 14+ audio-routing changes deprecate older methods; this first version uses compatible legacy call-audio APIs and should be modernized for production.
- User numbers and selected photo URIs are kept on the device. No server, remote caller lookup, contact upload or analytics integration is included.

## Repo tree

```
app/src/main/java/com/scall/phone/
├── MainActivity.kt
├── data/PhoneRepository.kt
├── telecom/{SCallInCallService,CallBridge,CallActionReceiver,CallUiActivity}.kt
└── ui/{SCallApp,Widgets,CallScreen}.kt
    └── theme/SCallTheme.kt
```

## Project status

Current delivery: **public v1.2 source code + verified APK + published GitHub preview release**. **Actual-device calling and reliability tests are still pending**.
