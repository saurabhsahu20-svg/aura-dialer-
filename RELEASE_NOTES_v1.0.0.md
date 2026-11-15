# S call v1.0.0 — Android Preview

**S call** is a minimal Android phone dialer project, built with Kotlin and Jetpack Compose.

## Included in this preview
- Navy-blue minimal design with the approved S call launcher icon
- Default dialer role request and Android Telecom in-call service
- Contacts, Favorites, Recents, T9 keypad and contact search
- Swipe right on a contact row to call; swipe left to open a text message
- Full-screen contact photos with swipe answer/decline controls
- Light, Dark and AMOLED themes

## Download
The attached **S-call-v1.0.0-preview.apk** is a **debug-signed APK** built by [GitHub Actions](https://github.com/saurabhsahu20-svg/aura-dialer-/actions/runs/43687891267). It is provided for testing, not as a production release.

## Install
Download the attached APK on an Android device, allow the installation in Android system settings when prompted, launch **S call**, and grant the supported default-phone role and permissions.

## Important limitations
- A **successful Android build is verified**, but **physical-device calling tests have not yet been completed**.
- Incoming-call full-screen behavior depends on Android version, permission status and lock-screen policy.
- Dual SIM, conference calls, call merging and audio routing depend on device/carrier support.
- Online unknown-number identification, call recording and advanced image crop editing are **not included** in this preview.
- Emergency calling must not rely on an untested alternative dialer. Keep the phone's preinstalled dialer available.

**Source code:** https://github.com/saurabhsahu20-svg/aura-dialer-
