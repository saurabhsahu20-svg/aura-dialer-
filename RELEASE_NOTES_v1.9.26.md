# S Caller v1.9.26 — In-call accidental-touch protection

**Issue:** During an incoming or answered phone call, accidental ear/face touches could open Android Quick Settings (including the Do Not Disturb control), or press call controls.

## Focused fix
- New `CallProximityGuard` listens to the hardware proximity sensor **only when the in-call Activity is visible and a real earpiece call is active, connecting or dialing**.
- Uses Android's supported `PROXIMITY_SCREEN_OFF_WAKE_LOCK` on devices where available so the screen can turn off when near the face; releases immediately on call end, routing away from the earpiece, or Activity stop.
- Safely consumes near-face **in-app touch gestures** on devices without screen-off wakelock support.
- Incoming ringing call remains fully answerable. Speaker, Bluetooth, wired headsets and held calls remain touchable.
- Hides the in-call status bar while Call UI is foreground, reducing accidental Quick Settings entry; an intentional system swipe still has access.
- Adds explicit Android Telecom audio route kind and automated proximity policy tests.

## Existing features retained
- Current Contacts/Favorites search optimization, keypad height fix, accurate history, WhatsApp/Telegram/SMS shortcuts, and Android Telecom hang-up controls.
- Android package remains `com.scall.phone`. New version `1.9.26`, version code `126`.

## Validation and limitations
- A CI build and unit tests must succeed before sharing a new APK.
- Full incoming/outgoing calls, notifications, sensor response, Audio-route changes and DND-related behavior require real-phone testing because an emulator cannot reproduce all OEM hardware conditions.
- **Android controls the system Quick Settings shade**: this app can reduce accidental touches, but cannot and should not prevent intentional OS notification shade gestures or directly manage Do Not Disturb mode.
- **Signing matters:** a preview APK signed with a different certificate cannot update S Caller v1.9.21 in place. Back up app data and preserve the original key for future in-place updates.
