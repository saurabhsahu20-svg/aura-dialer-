# S call — Android Dialer (v1.0 Preview)

**S call** is a privacy-focused Android phone dialer built with Kotlin and Jetpack Compose. It features a minimal navy-blue Material 3 interface and the approved blue S call launcher icon.

[![Build S call debug APK](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml/badge.svg)](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml)

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

A successful APK build has already been verified in [this workflow run](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43687891267).

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

- This is a **first implementation**: it **compiled successfully in GitHub Actions**, but has **not been tested on a physical Android phone**. Verify calling and incoming-call behavior before everyday use.
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

Current delivery: **public source code + verified GitHub Actions APK + public GitHub Preview Release**. **Actual-device calling and reliability tests are still pending**.
