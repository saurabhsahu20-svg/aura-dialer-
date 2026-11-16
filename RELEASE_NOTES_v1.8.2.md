# S call v1.8.2 — Recording Library with Emulator-Tested Import

## What's new
S call's **Settings → Call Recording** page is now easier to use. The **Import button, searchable recording library and playback controls** are placed near the top, ahead of advanced retention and privacy options.

### Functional recording-library features
- Import audio using Android's document picker after participants have received appropriate notification/consent.
- Validate audio duration, file size and playable format before saving.
- Keep a **private local audio copy** with crash-resistant metadata.
- Search, play/stop, rename, optionally associate a phone number, share explicitly or delete imported files.
- Manually clean up copies older than 30 or 90 days after explicit confirmation.
- Avoid broad storage permissions, background microphone capture and automatic sharing.

### Crucial call-recording limitation
**S call does not record ordinary SIM calls.** Automatic incoming/outgoing recording and in-call manual two-way capture are **UNAVAILABLE**, not silently enabled. Android reserves protected cellular audio capture for system-privileged components. This app is a third-party `InCallService` dialer without `CAPTURE_AUDIO_OUTPUT`. No separate VoIP recording pipeline has been implemented.

Use the device manufacturer's own lawful recording feature where supported, then import user-selected audio files into S call to manage and play them. S call does not claim AI/HD recording, live microphone enhancement, hidden capture, encrypted backups or biometric locks.

### Why v1.8.2?
In an earlier preview, Android UI tests found that the Import shortcut appeared too far down the screen for convenient access. v1.8.2 moves the recording library above the advanced configuration cards and adds a **release-blocking Android emulator test** for both UI navigation and actual WAV import/rename/playback/deletion. A failed test prevents the APK from being published.

### Preserved features
Call screen, Answer/Decline, SIM selection, responsive keypad, caller photos, Android 6+ compatibility, audio device picker, light-green icon, call history and all earlier improvements.

### Downloads
- **[Direct S-call.apk](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.2-preview/S-call.apk)**
- **[ZIP backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.2-preview/S-call-v1.8.2-install-package.zip)**
- **[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.2-preview/S-call-SHA256SUMS.txt)**

### Testing and caution
The release workflow performs unit tests, Android Lint, Android 10 emulator UI+WAV import tests, APK build/signature/identity validation, ZIP integrity and checksum verification. Other GitHub Actions run emulator tests on Android 6/8/15.

**No real carrier-call recording or live SIM audio clarity testing was performed.** Recording compatibility of third-party OEM recorders is not guaranteed. This is a debug-signed Preview; back up important imported files before uninstalling or replacing older builds (different debug signing keys may reject an in-place update). Keep the stock phone dialer available for emergencies.
