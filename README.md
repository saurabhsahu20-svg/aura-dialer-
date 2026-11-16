# S call — Android Dialer (v1.8.1 Private Recording Library Preview)

**S call** is a privacy-focused Android phone dialer built with Kotlin and Jetpack Compose. It features a minimal Material 3 interface and the approved light-green S call launcher icon.

[![Build S call debug APK](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml/badge.svg)](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/workflows/android-debug-apk.yml)

## 🎙️ Latest — S call v1.8.1 Private Recording Library

**[⬇️ Download S call v1.8.1 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.1-preview/S-call.apk)**

**[📦 ZIP fallback download](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.1-preview/S-call-v1.8.1-install-package.zip)**

**[Official GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.8.1-preview)**

- New **Settings → Call Recording** screen provides **consented audio-file import**, searchable history, playback, rename, optional phone number association, explicit sharing and deletion.
- Private on-device storage with validation, atomic metadata and user-confirmed manual cleanup.
- The app does **not** record live cellular calls automatically or secretly. Protected SIM-call audio is inaccessible to ordinary third-party apps.
- Disabled automatic-recording controls explain limitations clearly. App-owned VoIP call recording, encryption and biometric locking have **not** been implemented.
- No microphone permission, no background audio capture and no uploads.
- Preserves dialer, history, contacts, SIM controls, caller photos, light-green launcher icon, Audio picker and Android 6+ support.
- Automated Android unit, Lint, instrumented UI/media-library tests and APK checks form the release verification.

**Preview limitation:** User-imported recordings can be managed; live carrier/VoIP recording and physical-device audio clarity are not supported or verified.

## Previous release — v1.8.0

## 🎧 Latest — S call v1.8.0 Premium Audio Routing

**[⬇️ Download v1.8.0 Android APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.0-preview/S-call.apk)**

**[📦 ZIP download backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.0-preview/S-call-v1.8.0-install-package.zip)**

**[Official GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.8.0-preview)**

- **Improved audio switching:** in-call output selector for earpiece, wired, Bluetooth and speakerphone, showing only routes reported by Android.
- **Android 14+:** modern Telecom endpoint events and route requests; Android 6–13 retains compatible routing APIs.
- **Safer speaker fallback:** prefers connected wired earphones when turning speakerphone off.
- **Voice quality information:** settings display detected device AEC/NS/AGC capabilities **for future app-controlled VoIP only**.
- **Truthful status:** S call does not claim unsupported "AI noise cancellation" or guaranteed HD Voice for ordinary SIM/cellular calls.
- **Preserves** all prior dialer, photo, SIM, notification, keypad, light/dark UI, and call history features.

**Testing:** Unit tests, Android Lint and debug APK build pass. Real carrier audio clarity, microphone DSP and physical headset switching are **not tested**. Debug-signed preview, not a Play Store release.

## Previous release — v1.7.7

## 📞 Latest — S call v1.7.7 Answered Call Screen Recovery

### **[⬇️ Download v1.7.7 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.7-preview/S-call.apk)**

### **[📦 ZIP Backup if APK Download Fails](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.7-preview/S-call-v1.7.7-install-package.zip)**

**[GitHub v1.7.7 Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.7-preview)**

- Fixes the screenshot issue where answering an incoming call may open a blank **Connecting to phone service…** screen.
- The notification Answer button now directly opens the call UI using an Activity PendingIntent, as recommended for Android 12+.
- Android Telecom call data is recovered on Activity/Service recreation; older services cannot clear replacement service call state.
- A bounded loading state with **Retry call screen** replaces never-ending loading when Android Telecom has supplied no call.
- Preserves in-call controls, full-screen caller photo, smooth swipe answer, dual SIM, light-green app icon, bold call-type symbols, responsive keypad and Android 6+ support.
- Unit tests, Android Lint, emulator recovery UI tests and APK checks are part of GitHub CI.

**Preview warning:** Real incoming carrier calls still require physical testing. This is a debug-signed APK; back up preferences before replacing older preview apps if their certificates differ.

## Previous version — v1.7.6

## 📱 Latest — S call v1.7.6 Wider Android Compatibility

**[⬇️ Direct APK — Android 6.0+ compatible](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.6-preview/S-call.apk)**

**[📦 ZIP backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.6-preview/S-call-v1.7.6-install-package.zip)**

**[Official GitHub v1.7.6 Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.6-preview)**

- Supports **Android 6.0 (API 23) and newer**, compared with the old Android 8+ minimum.
- Legacy phone-call notification fallback on Android 6/7; Android 8+ channel behavior retained.
- Android 6 avoids unsupported SIM-default APIs; cross-version guards prevent common compatibility crashes.
- Devices without cellular hardware show a safe message instead of attempting a real call.
- Cross-version UI smoke tests run on Android 6, 8 and 15, plus existing Android 10 emulator checks.
- Preserves incoming-call alerts, SIM preferences, responsive keypad and dark/light themes.

**Limitations:** No Android 5 or earlier, and no universal guarantee for every manufacturer's ROM, carrier or SIM. This is a debug-signed preview; test with your actual phone before using S call as your main dialer.

## Previous preview — v1.7.5

## 📞 Latest — S call v1.7.5 Incoming Call Screen Fix

**[⬇️ Download S call v1.7.5 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.5-preview/S-call.apk)**

**[📦 ZIP download backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.5-preview/S-call-v1.7.5-install-package.zip)**

**[Official GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.5-preview)**

- Corrects a **ringtone-only incoming-call issue**: the call notification refreshes when Telecom changes the call into RINGING state.
- Proper high-priority **CallStyle** incoming alert provides **Answer** and **Decline** controls and Android-managed full-screen visibility.
- **S call → Settings → Incoming call display → Fix** checks default-phone role, notification settings, channel, and Android 14+ full-screen permission.
- Call screen displays **PHONE IS RINGING** and readable caller information.
- Prior light-green icon, bold call symbols, adaptive keypad, smooth slider, SIM selection, history editor and Back navigation are retained.
- Android unit tests and Lint verify the code. Real-device incoming cellular calls still require physical testing.

**Android note:** Unlocked devices normally show heads-up call alerts; locked/off-screen devices may show the full-screen call UI, depending on user/system permissions.

## Previous release — v1.7.4

## 🟢🔴🔵 Latest — S call v1.7.4 Bold Call Direction Icons

**[⬇️ Direct APK — S call v1.7.4](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.4-preview/S-call.apk)**

**[📦 ZIP backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.4-preview/S-call-v1.7.4-install-package.zip)**

**[Official GitHub v1.7.4 release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.4-preview)**

- **Incoming: Dark Green `#006B3E`**, **Missed: Dark Red `#A8172B`**, **Outgoing: Dark Blue `#164B9F`** for excellent visibility on Light Theme.
- Thicker hand-drawn rounded direction arrows, increased icon sizes, bordered avatar badges and bold call-type labels.
- Directions are also identified by distinct arrow shapes and words, not color alone.
- Stronger filters on the Call History screen; the improved adaptive keypad, SIM controls, photo editor and mint-green launcher icon remain intact.
- The three light-theme colors have >6:1 contrast on white, verified with unit tests. Android lint and APK compilation are verified in CI.

**Testing status:** Debug-signed preview. Automated checks pass; physical SIM/cellular and incoming-call lock-screen behavior still needs device validation.

## Previous version — v1.7.3

## ⌨️ Latest — S call v1.7.3 Full-height Keypad

**[⬇️ Direct APK Download](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.3-preview/S-call.apk)**

**[📦 ZIP Backup Download](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.3-preview/S-call-v1.7.3-install-package.zip)**

**[Official GitHub v1.7.3 Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.3-preview)**

- Fixed the large empty space below the dialpad. The 4 rows of number buttons now resize based on screen height.
- Larger, near-square keys on regular phones; min 72dp touch targets and scrolling on small displays.
- Call, Message and Backspace remain at the bottom; no changes to SIM preferences or existing call history.
- Automated unit and Lint checks plus **5/5 emulator UI smoke tests** passed, including a test for excessive keypad-to-call-button spacing.
- This is a debug-signed testing APK; cellular calls require real-phone verification.

## Previous version — v1.7.2

## ✅ Latest Release — S call v1.7.2 Quality Tested Preview

### **[⬇️ Download Verified APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.2-preview/S-call.apk)**

### **[📦 ZIP Backup Download](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.2-preview/S-call-v1.7.2-install-package.zip)**

**[GitHub v1.7.2 Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.2-preview)** · **[SHA-256 Checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.2-preview/S-call-SHA256SUMS.txt)**

**Quality:** Unit tests, Android Lint, and Android 10 emulator UI tests (4/4) passed. Fixed Android 8.0 incoming-call lock-screen compatibility and improved SIM permission checks. The release APK is checked for package identity, version, signature and integrity.

**No features removed:** Light-green icon, Recents history filters, hidden SIM home banner, SIM choice through toolbar/Settings, Keypad, back navigation, call sliders and caller photos remain.

**Limitation:** This is a debug-signed test APK. Cellular call/dual-SIM/real lock-screen functionality needs physical-device testing. Certificate mismatch with older previews may prevent in-place upgrades. Back up app data before uninstalling.

## Previous version — v1.7.1

## ✅ Latest — S call v1.7.1 Clean Home Screen

**[⬇️ Download S call v1.7.1 APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.1-preview/S-call.apk)**

**[📦 ZIP fallback if direct APK fails](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.1-preview/S-call-v1.7.1-install-package.zip)**

**[Official v1.7.1 Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.1-preview)**

- Removed the purple **SIM & Network** banner from the main pages, making the screen cleaner.
- All SIM controls remain available through the **top toolbar SIM icon**, **Settings → SIM & Network**, and the Keypad.
- Preserves call history filters and directional icons, call history editing, the updated keypad, and light-green app icon.
- Tested by Android CI; preview debug APK, with real-device calling tests still needed.

## Previous release — v1.7.0

## 🟢🔵🔴 Latest — S call v1.7.0 Incoming / Outgoing / Missed Call Icons

**[⬇️ DOWNLOAD S call v1.7.0 Direct APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.0-preview/S-call.apk)**

**[⬇️ DOWNLOAD S call v1.7.0 ZIP Fallback](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.0-preview/S-call-v1.7.0-install-package.zip)**

**[Official S call v1.7.0 GitHub Release](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/tag/v1.7.0-preview)**

- **Green Incoming:** call-received arrow, clear Incoming label and circular avatar badge.
- **Blue Outgoing:** call-made arrow, clear Outgoing label and badge.
- **Red Missed:** missed-call arrow, Missed label and highlighted missed entries.
- **Call History filter chips** for All / Incoming / Outgoing / Missed, each with counts.
- Cleaner time and connected-duration metadata; personal notes, swipe gestures and history deletion remain available.
- Accessibility: the symbol and label identify the direction even without the color.

**Build checks:** automated call classification, keypad/navigation tests, APK package/version and ZIP/checksum integrity. **Real-phone calling is still to be tested.** Debug-signed Android preview for testing only.

## Previous release — v1.6.1

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
