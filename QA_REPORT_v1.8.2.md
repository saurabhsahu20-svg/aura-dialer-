# S call v1.8.2 — Recording Library Release QA

**Package** `com.scall.phone` | **Version** 1.8.2 / versionCode 20 | **Minimum Android** API 23

## Release-blocking QA checks
- Kotlin/JUnit unit tests cover imported file title bounds, retention 30-day threshold and accurate unsupported cellular auto-recording status.
- Android Lint checks required SDK compatibility and application resource usage.
- **Android 10 instrumentation tests** include:
  1. Open S call Settings → Call Recording, see the unavailable SIM-recording status, find the Import button and return to Settings.
  2. Generate an on-device silent WAV fixture (not captured by microphone), import with Android's private FileProvider, validate playback duration, rename and associate an example phone number, open in MediaPlayer and delete the private copy.
  3. Preserve established Keypad, Back, SIM settings and call-screen recovery UI smoke tests.
- Release workflow refuses to publish until all these checks, APK build, signature, package/version/minSdk, ZIP and SHA-256 validation succeed.

## Architecture and security
Files are selected one at a time using Android's system document picker. Imported audio copies and atomic metadata are in app-private storage. A non-exported FileProvider permits sharing only after the user explicitly chooses Share. No broad external storage scan, recording background worker, audio interception, hidden recorder, or microphone permission is added.

## Not supported
- Automatic incoming/outgoing SIM recording, live two-way SIM audio, Android's privileged `VOICE_CALL` audio capture and active AI noise filtering.
- App-controlled VoIP recording, because the app has no VoIP pipeline.
- Automatic retention/backup, encryption at rest, biometric unlock or secure erasure.
- Validating OEM recorder output formats on real Samsung/Xiaomi/OnePlus/etc. devices.

**Real-device testing still needed:** voice-call lifecycle, carrier-specific recording permissions, imported files from actual phone recorders, Bluetooth and speakerphone.

**Debug signing:** separate CI runs may use different signing keys. Export important imported audio before uninstalling previous S call builds.
