# S Caller v1.9.35 — Smart Calling Convenience (Opt-In)

Based on the verified v1.9.34 release, preserving working call handling, stabilized keypad, contact name display, accurate missed-call badge, incoming CallStyle notification and hang-up fixes.

## New fully optional features (default OFF)
1. **Flip to Silence:** In Settings, enable "Flip to silence incoming calls". When Android Telecom reports a ringing SIM call, the accelerometer is registered at a low sampling rate. Turning the phone from screen-up to face-down **for 750ms** invokes Android TelecomManager.silenceRinger(). It silences the *ringtone only* and does not reject the call or change persistent device ring/DND settings. Unregisters immediately after silence, answer, rejection or call end. Requires S Caller to be the default Phone app; may vary across OEMs.
2. **Announce Saved Caller Name:** In Settings, enable "Announce saved caller name". During ringing, Android's installed TTS engine speaks the name **only for a contact actually saved in the phone**, once per call. No raw phone numbers, no remote lookup, no call recording. Stops speaking when the call is answered/ended. Disabled by default to protect privacy; depends on TTS availability/voice routing.
3. **One-Handed Keypad:** Settings > One-handed keypad > Off / Left / Right. Number keys occupy about 82% width when enabled and align to the requested side. Preserves full-size Call and SMS actions and the stable keypad layout that prevents keys moving during typing. Default Off keeps the familiar full-width keypad.

## Existing capabilities preserved, not reimplemented
- T9 contact search; local caller-name match; dual SIM and messaging options; dark/AMOLED themes; Favorites, call history and call notes; proximity touch protection; incoming-call notification; missed-call read state; Telegram/WhatsApp shortcuts.
- Fixed outdated About text to show the actual app version.

## Unsupported/not misrepresented
- CNAP/KYC name display requires carrier and regulatory support and cannot be guaranteed through a regular dialer API.
- Live identity/spam labels require an authorized, reliable data source and appropriate Android screening role; never invent a risk score.
- Protected SIM call recording, stealth recording, AI noise removal or mandatory recording announcements cannot be bypassed via standard third-party Android APIs.
- Dialer secret codes for hiding unrelated apps are not part of the core calling app and should not be used as a covert feature.

## QA & Installation
- Version: 1.9.35, versionCode: 135, same application ID: `com.scall.phone`.
- Source-only tests validate stable face-down recognition and the opt-in/once-per-call caller-announcement policy.
- Android CI build, lint and emulator tests must pass; *real hardware* proximity/gravity, call audio routing, TTS and end-to-end cellular calling still require physical device tests.
- CI debug signing certificate differs from previously installed v1.9.21 on this device. No in-place update without the ORIGINAL v1.9.21 signing private key. Back up data and retain a safe default Phone application before changing installations.
