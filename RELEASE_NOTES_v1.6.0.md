# S call v1.6.0 Preview — Interactive Keypad & Smarter Back Navigation

This Android preview improves the number dialer and fixes Back button/gesture navigation from inner screens.

## New interactive keypad

- **Tactile dialpad:** gently animated number tiles with haptic feedback and clear pressed states.
- **Better layout:** dialed number in a readable panel, an always-visible Call button, and a scrollable pad on compact phones.
- **Smart matches:** instantly suggests matching contacts by phone number or T9 contact-name digits.
- **Paste** a number from the clipboard using a deliberate tap; phone-number input is filtered and limited to 40 characters.
- **Clear** the entered number with one tap or **long press Backspace**; a regular Backspace tap removes the last digit.
- **Long press 0** to insert a `+` for international dialing.
- **Message** from the keypad using the existing messaging-app and SIM-selection handoff.
- **SIM chip on Keypad** showing the currently selected calling-line preference and opening SIM & Network settings.

## Correct Android Back navigation

- Android's system **Back gesture** or **Back button** on the keypad returns to the previous screen, not straight out of the app.
- A visible **Back arrow** is also provided in the keypad header.
- Contacts, Favorites, Keypad, Settings and SIM & Network remember recent navigation history.
- Back inside a contact detail or caller-photo editor closes that page before navigating elsewhere.
- The root Recents screen can still be exited normally.
- Recents selection mode can be dismissed with Back without losing the current app screen.

## Retained

Light-green icon, 68% contact swipe threshold, long incoming-call slide, full-screen caller photos, dual-SIM routing preferences, and single/bulk call-history editing and deletion.

## Verification

GitHub Actions runs `testDebugUnitTest` (including keypad input and navigation unit tests) and `assembleDebug` before releasing.
This is a **debug-signed preview APK** for Android 8.0+. **Physical-phone testing is still required** for outgoing/incoming cellular calls, in-call features, SIM selection, Android back-gesture interactions and lock-screen presentation.

**Important:** The release is not signed for the Play Store. A debug signing-key mismatch with a previously installed preview may require uninstalling the old APK first; back up app-specific preferences before doing so. Keep your phone's preinstalled dialer available when evaluating alternative calling apps, particularly for emergency calls.

**Project:** https://github.com/saurabhsahu20-svg/aura-dialer-
