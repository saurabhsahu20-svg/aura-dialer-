# S Caller v1.8.9 — Clean Calling Update (No Recording)

## What was removed
- All **automatic and manual call recording** controls, prompts, timers, indicators, and options.
- The in-call Record / Stop recording button and associated Compose state.
- The recording library and recording settings menu/toolbar shortcut.
- The recording foreground service, audio-capture capability checks and storage/import/share implementation.
- Android microphone and recording foreground-service permissions; removed the now-unused FileProvider and recording XML paths.
- Recording-specific tests and unused code.

**This APK never initiates call recording or requests microphone access.** No automatic or manual cellular recording functionality remains.

## Reliability and smoothness
- Fewer recording-related UI states and no recorder service start/stop during calls, reducing unexpected failure paths.
- Caller photo and caller contact lookups are handled safely so unreadable preference data or failed contact queries do not crash live call controls.
- Smooth recents/search/keypad enhancements from v1.8.8 are retained.
- Preserve incoming/outgoing calls, keypad, dual SIM settings, audio output selection, speaker, Bluetooth routing, contact photos and other existing phone controls.
- A saved navigation state that points to the removed recording page migrates back to Recents.

## Existing locally stored recordings
**No code automatically deletes previously saved or imported recordings.** If old files were stored in S Caller's private directory, removing the recording manager makes them inaccessible from the new UI. If important, export them using the old version before upgrading. An uninstall or clearing app storage can delete them.

## Validation and installation
- GitHub Actions: debug APK compile, unit tests, Android lint, emulator UI smoke tests on Android 6/8/15, app identity/signature/sha256 checks.
- The published APK is a **developer prerelease signed using a GitHub debug key**, not a production-signed stable release.
- Debug signatures may differ between builds, preventing an in-place install. Back up important data, check your device's current default Phone app, and ensure another dialer remains available before uninstalling.
- Passing emulator tests does not guarantee zero crashes on physical hardware. Earlier reported "S call keeps stopping" requires a physical-device stack trace if it still occurs.
