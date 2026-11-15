# S call — Android Dialer (v1.2 Smart SIM & Smooth Calls Preview)

**S call** is a privacy-focused Android phone dialer built with Kotlin and Jetpack Compose. It features a minimal navy-blue Material 3 interface and the approved blue S call launcher icon.

[![Build S call debug APK](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml/badge.svg)](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml)

## ✨ NEW — S call v1.2.0 Smart SIM & Smooth Calling

**[⬇️ Download S call v1.2.0 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.2.0-preview/S-call-v1.2.0-preview.apk)**

**[View S call v1.2.0 Preview Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.2.0-preview)**

- **Calls:** Pick a particular Telecom phone account, or ask before each call. Applies to the keypad, call shortcut and contact-name swipe; if the selected line is unavailable, the app asks again rather than silently using the other line.
- **SMS:** Suggest a SIM or ask each time, then check the actual SIM in the messaging app. S call doesn't send SMS itself and cannot force that app to honor the preference.
- **Mobile data:** View Android's data SIM and open Android network settings to choose another. Standard third-party dialers cannot directly change the system's mobile-data SIM.
- **Swipe controls:** Smoother touch-following animations, spring-back, thresholds, clearer haptic and visual feedback, and accessible tap alternatives for incoming-call accept/decline.
- **Accuracy:** Automated threshold unit tests and Android APK build have passed in [GitHub Actions](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43704804958).

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

A v1.2.0 APK build and swipe unit tests were verified in [this workflow run](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43704804958).

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
