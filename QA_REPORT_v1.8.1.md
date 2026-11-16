# S call v1.8.1 — Recording Library QA Report

**App:** S call / `com.scall.phone`  
**Version:** `1.8.1` (versionCode 19)  
**Android:** 6.0+ / API 23+  
**Audio capture scope:** **None**. No Android SIM call recording pipeline or VoIP call recorder exists in this app.

## Recording architecture

1. Android system audio picker gives S call one explicit user-selected URI at a time; no broad device-storage scan.
2. `RecordingStore` copies selected media to app-private `filesDir/recordings` with a 180 MB limit.
3. Audio duration is validated via `MediaMetadataRetriever` before marking the import successful.
4. JSON metadata (title, optional user-supplied number, date, duration, extension, size) is written using Android `AtomicFile`.
5. Playback uses `MediaPlayer`; app-private audio is shared ONLY upon a user action through a non-exported `FileProvider` granting temporary read URI access.
6. Delete and optional older-file cleanup require explicit user confirmation. Default retention is to keep all; no automatic retention worker runs.

## Automated tests

- **Unit tests:** unavailable SIM auto recording is never misrepresented as supported; title sanitizer and retention boundary conditions.
- **Emulator UI smoke:** user can navigate Settings → Call Recording, read truthful capability status, find the Import control and return to Settings.
- **Emulator recording-library test:** creates a local WAV fixture without using the mic, imports via FileProvider, validates audio duration, renames/adds a phone number, opens playback metadata with MediaPlayer and deletes its private copy.
- **Cross-version emulator smoke:** tests on Android 6, 8 and 15 in GitHub Actions.
- **Release pipeline:** unit tests, Android Lint, package/version/minSdk verification, signed APK, ZIP archive and SHA-256 checksum checks.

## Not tested, not claimed

- Genuine carrier/SIM call recording, either side's real voice, background capture, HD/speech enhancement, wind/echo reduction, Bluetooth call-media capture.
- Live VoIP recording, because no authorized VoIP media pipeline exists.
- OEM call-recorder integration, physical Samsung/Xiaomi/etc. compatibility for private recordings, backup encryption or secure wiping.
- Any guarantee that all imported formats will decode on every Android build.

## Consent and privacy

Recording files come from the user's explicit selection; S call does not ask for `RECORD_AUDIO` or `CAPTURE_AUDIO_OUTPUT` and does not run a recording service. Files are private to this app; sharing is explicit, and people should only record/share conversations with appropriate notice and consent.

**Preview only:** Android's debug signing across CI runs may differ. Export important imported audio before uninstalling an older version or switching to a differently signed APK.
