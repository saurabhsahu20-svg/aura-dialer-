# S call — Android Dialer (v1.0 source preview)

**S call** is a privacy-focused Android dialer project with a restrained, navy-blue Material 3 interface. Includes the user's approved blue call icon, imported as launcher and adaptive assets.

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

1. Upload the contents of this directory to a GitHub repository, retaining `.github/workflows/android-debug-apk.yml`.
2. Open your repository's **Actions** tab.
3. Choose **Build S call debug APK** and click **Run workflow**.
4. When the workflow completes successfully, open that run and download **S-call-debug-apk** under Artifacts.
5. The ZIP artifact contains `app-debug.apk`.

A successful workflow is required before describing the APK as built; the source archive is **not an APK**.

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

- This is a **first implementation**, not yet compile-verified or physically tested in the present environment: Android SDK/Gradle and external network were unavailable while it was created. Resolve any build/device issues before everyday use.
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

Current delivery: **source project + build workflow**, **APK not built or device-tested**. This distinction is intentional.
