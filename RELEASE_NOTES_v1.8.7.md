# S Caller v1.8.7 — Smoother Calling & Manual Recording UI (Public Preview)

### What's improved
- Contacts and call history stay visible during background refresh, avoiding repeated blank/loading flicker.
- Search results are cached across unrelated Compose recompositions, reducing list-filtering and private history lookups during UI updates.
- Keypad SIM labels are cached so pressing digits does not repeatedly query Android Telecom over Binder.
- The main Activity avoids duplicated provider refresh during immediate start/resume and quick transitions.
- The in-call Activity no longer resets its reconnect timers for every audio-route/call-detail update.
- Contact images load off the main thread so incoming/outgoing call controls remain responsive.
- Existing call UI and all working features are retained.

### Manual Call Recording
- User-controlled Record/Stop UI with recording timer and consent confirmation is present.
- **Automatic recording is fully OFF.**
- On standard third-party Android builds the system will **not** permit S Caller to capture both sides of cellular calls; the app clearly reports unsupported rather than making silent/fake recordings.
- On an OEM/system-authorized build only, manual capture could be available if all permissions and native two-way audio capture are supported and the device has been validated.
- Private recording library: import, pause/resume playback, rename, share, delete, contact name, date, duration.

### Verification
- Android 6, 8 and 15 emulator UI smoke checks; unit tests, Android lint and APK build are part of CI.
- These tests do not reproduce every manufacturer Telecom issue or validate two-way SIM call capture.
- **The user's previous 'S call keeps stopping' report is not confirmed resolved on their actual handset.** Obtain the device model, Android version and relevant crash stack trace before calling this stable.

### Preview & installation cautions
- This GitHub prerelease contains a **debug-signed** APK. It is intended for test installation and not a Play Store or production release.
- A previous APK signed with a different certificate cannot be updated in place. Back up any app-local private recordings or settings before uninstalling, and keep another working dialer available.
- The APK is not asserted to provide standard third-party SIM call recording.
- Production release requires an owner-controlled stable signing key, supported-device tests and actual calling/crash verification. Do not distribute a debug APK as production-signed software.
