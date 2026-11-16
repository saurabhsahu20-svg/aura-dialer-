# S Caller v1.8.6 — Manual Recording Controls (Preview)

## What's new
- **Record Call** appears during connected calls.
- Every start requires a manual tap and confirmation that participants know and agree.
- **Stop Recording** with live timer is available only when the OS has actually started native capture.
- Recording automatically stops on call termination and will be saved only if the resulting M4A audio has valid duration and sufficient data.
- An ongoing Android foreground notification shows when recording is active and provides a Stop action.
- New **Recordings** shortcut on the main screen. Recording library supports contact display, date, phone number, duration, pause/resume playback, rename, share, and deletion.
- AAC audio intended for OEM-authorized native capture; app-private storage, no cloud upload, no per-contact automation and no automatic recording.

## Android limitations — important
The normal S Caller APK is a third-party Android Telecom dialer. Android does **not** expose the two-way cellular audio stream to ordinary apps, and does not grant the system-only CAPTURE_AUDIO_OUTPUT permission. Therefore **the standard downloadable APK cannot record two-way SIM calls**, even though it shows the Record Call button and explains the limitation. It never creates a microphone-only substitute and never pretends a recording was saved.

The internal native capture implementation is intentionally gated for partner/OEM system builds with legitimate permission grants and supported hardware. OEM-specific integration, required participant notifications, and real-device audio verification are prerequisites to enabling it. This is **not** a workaround for restricted audio sources and does not modify or suppress any platform announcements.

The repository has no VoIP calling engine; authorization and integration with an actual app-owned VoIP media pipeline are required before VoIP recording can be offered.

## Verification
- GitHub Actions: unit tests, debug APK compilation, Android lint and cross-version emulator UI tests.
- Emulator UI tests cannot validate an actual cellular audio stream, both participants' audio quality or device-specific permission policy.
- Real outgoing and incoming SIM calls, dual-SIM handling, speaker and Bluetooth, call-end cleanup, and manufacturer recording support require physical-device QA.
- Existing 'S call keeps stopping' on an affected phone requires its crash stack trace to confirm the underlying problem is fixed.

## Installation
This is a **debug-signed preview** and must not be represented as a production release. The debug certificate may differ from earlier APKs and may not allow an in-place upgrade. Back up private recordings and other app-local data before considering an uninstall/reinstall. Never uninstall an existing dialer without ensuring another default Phone app is available.

## Release gate
Publish as a development prerelease only after all automated checks pass. Production release requires a stable signing key, tested devices, privacy disclosure, participant-consent compliance and verified supported audio capture.
