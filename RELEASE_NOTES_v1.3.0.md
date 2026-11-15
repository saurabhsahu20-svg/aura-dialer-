# S call v1.3.0 Preview — Long Slide & Mint-Green Icon

## What changed

- **Contact-name swipe-to-call:** requires sliding **68% of the contact row width** before release to place a call. Short or accidental drags do nothing. Swipe left to message uses the same protection.
- **Incoming call slider:** requires sliding **82% of the available direction travel** to answer or reject. Visual and haptic confirmation at the threshold, smooth no-bounce spring return on cancellation.
- **New app icon:** premium light-green / mint S call icon, with matching launcher, adaptive foreground and density-specific artwork.
- **Existing features preserved:** phone account (SIM 1/2) selection, Ask Every Time, contacts, keypad, favorites, calls, photo editor, dark/AMOLED modes.

## Test and safety

Android Gradle build and swipe-threshold unit tests are executed in GitHub Actions. A successful APK build does **not** establish that calling or slide behavior has been tested on your actual Android device. Always test incoming and outgoing calls with another device before daily use. Keep the device's stock dialer available for emergencies.

SMS SIM selection is controlled by the messaging app, and mobile-data SIM selection is controlled by Android settings.

This is a **debug-signed preview APK**; signatures may differ from previous previews, which can require uninstalling the earlier build (and may erase app settings). Back up important app data before uninstalling.
