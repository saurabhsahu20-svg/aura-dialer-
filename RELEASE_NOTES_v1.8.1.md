# S call v1.8.1 Preview — Private Recording Library

## What actually works

**S call → Settings → Call Recording** now offers a transparent, consent-focused recording library:

- **Import permitted audio recordings** from your Android device using the system audio picker. Import works with supported MP3, M4A, WAV, OGG, FLAC, AAC, AMR and related audio files when the Android media decoder can read them.
- **Secure, private local copies** in S call's own application files folder. No background upload, microphone recording or call interception.
- **Search** recordings by title or manually entered phone number.
- **Play / Stop** audio inside the app using Android MediaPlayer.
- **Rename** recordings, add an optional phone number, and retain the original audio bytes.
- **Share on demand** via a FileProvider with temporary read permissions; nothing is shared automatically.
- **Delete an imported copy** with an explicit confirmation; the original source file in the other recorder remains unchanged.
- **Optional manual cleanup** of private copies older than 30 or 90 days, with explicit confirmation. The default is to keep all files; there is no hidden automatic deletion.
- **Storage hygiene**: size cap (180 MB per import), rejects short/invalid media, requires readable audio duration, atomically stores recording metadata and removes partial imports on failure.
- **Recording disclosure**: the screen explains Android restrictions and reminds users to notify and obtain consent from call participants.

## Important: SIM recording is NOT enabled

**Automatic incoming/outgoing SIM call recording, live two-way capture and in-call Start/Stop recording were NOT implemented.** S call is a third-party Android Telecom dialer. Android reserves protected voice-call audio sources (including `VOICE_CALL`, `VOICE_UPLINK`, `VOICE_DOWNLINK`) for system-privileged applications holding `CAPTURE_AUDIO_OUTPUT`. Normal Android dialers cannot obtain this permission. There is **no app-controlled VoIP audio pipeline** in this project yet.

The disabled options are deliberately labeled **Unavailable**, not advertised as recording. This release does not turn on recording silently, intercept calls, suppress announcements, or claim unsupported HD/AI recording.

If the phone's built-in call recorder supports informed, lawful recording, users can **manually import its finished audio file** with this feature.

## Existing features preserved
- Android 6.0+ compatibility and caller screen recovery.
- High-contrast incoming/outgoing/missed icons, light-green icon, responsive keypad, SIM line selection and call history.
- S call v1.8.0 Android Telecom audio-device routing improvements.

## Download

**[Download S-call.apk](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.1-preview/S-call.apk)**

**[ZIP backup package](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.1-preview/S-call-v1.8.1-install-package.zip)**

**[APK SHA-256](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.1-preview/S-call-SHA256SUMS.txt)**

## Tests and limitations

GitHub Actions runs Kotlin/JUnit tests, Android Lint, APK build, signature, version/package ID, ZIP and SHA-256 checks. Android emulator UI tests cover the new Call Recording page and a generated local WAV fixture for import/rename/playback/deletion. **Physical phones, genuine SIM-call recordings, live mic/VoIP capture and original vendor recording apps are not tested.**

No storage encryption-at-rest, biometric lock, automatic backup, live recording, contact auto-recording or background cleanup service was added. These would require separate supported implementations and validation. App-private files are sandboxed; removal of the app deletes the private imported copies.

This is a **debug-signed preview**, not a Play Store production release. Certificates may differ between CI runs. Back up imported audio or share/export your important private copies before uninstalling S call or replacing an incompatible older debug build. Keep the stock Phone app available for emergency calling.
