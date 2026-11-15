# S call Changelog

## v1.2.0 — Smart SIM selection & swipe refresh (2026-11-15)
- Add **SIM & network** screen with stored per-line **voice call** preferences: Ask every call or a particular Telecom account.
- A selected voice line is matched using the precise PhoneAccountHandle, not unreliable SIM list ordering; if unavailable, S call asks again.
- Improve dual-SIM call chooser and make contact-name swipe right launch the selected SIM route.
- Add an **SMS SIM suggestion** picker and preferences. SMS is composed in the system messaging app, which controls the actual sending SIM.
- Show active Android defaults for SMS and Mobile Data, with a shortcut to official Android network settings. A regular dialer cannot change the device's default mobile-data SIM itself.
- Redesign incoming call slider with finger-following movement, spring reset, threshold haptics and accessible tap-to-answer/decline alternatives.
- Smooth contact swipe with clear reveal, one-time haptic feedback and deterministic action threshold to reduce accidental calls.
- Show actual Telecom account label on the call screen when the OS provides it.
- Keep blue S call icon and minimal themes.
- This is an **APK preview**: actual SIM routing and calling need tests on real dual-SIM devices.

# S call — Changelog

## v1.1.0 — Photo Studio Preview

- Redesigned full-screen **Caller Photo Studio** with a clean, premium live incoming-call preview.
- Pick a contact photo from the Android system document picker and preview before saving.
- **Pinch to zoom** and **drag to reposition** a caller image; reset the framing any time.
- Adjust the photo's **text-readability gradient** using the Tone slider.
- **Save look** applies the chosen photo, zoom, position and tone to actual call screens.
- New contact-list photo shortcut next to the call action.
- Refined Material 3 typography using Android's lightweight, offline-capable sans-serif font with tuned weights, sizes and line spacing.
- Preserved the approved blue S call launcher icon, core calling features and swipe-to-call gestures.
- Improved explicit photo-save flow and version display.

> This is a preview build. Compilation is checked in GitHub Actions; mobile-device calls and lock-screen behavior require real-device testing before daily use.
