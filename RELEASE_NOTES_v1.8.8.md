# S Caller v1.8.8 — Smooth, Stable Calling UI (Public Developer Preview)

## Performance updates
- Recent-call search waits briefly during typing, cancels outdated searches, and searches custom history notes on a background dispatcher. The dialpad and scrolling stay responsive.
- Recording audio playback uses asynchronous MediaPlayer preparation instead of blocking the Compose main thread for large files.
- Rapid playback taps during loading are ignored; playback errors and file-corruption conditions release resources and show clear feedback instead of terminating the UI.
- Recording-library search results are cached instead of recalculated during unrelated UI updates.
- Permission changes, unavailable contacts, and private-storage failures are handled independently while reading the recording library.
- Rename, delete, and retention cleanup failures are handled with user-visible messages and safe cancellation propagation.

## Preserved behavior
- S Caller's current design, contacts, keypad, SIM selection, call history, audio controls, recording manager and incoming/outgoing call UI remain unchanged in appearance.
- Automatic recording stays OFF. Manual recording is capability-gated; ordinary third-party Android APKs **cannot** capture both sides of a cellular call without system authorization. No fake recording state is used.
- No background uploads, stealth recording, or Android permission bypass.

## Validation
- GitHub Actions build and unit tests, Android lint, Android API 23/26/35 emulator UI tests, signature/identity checks, and SHA-256 checksum verification are required for the public preview.
- Automated tests cannot guarantee crash-free operation on every phone. The user's previously reported 'S call keeps stopping' issue is not independently verified on a physical SIM.
- No device-specific HD call recording is claimed.

## Installation cautions
**This is a debug-signed public developer preview, not a production or Play Store release.**
Its debug signature may differ from prior versions and prevent in-place installation. Back up app-private data before considering any uninstall; keep a working system Phone app available. Do not distribute it as a production-signed stable build.
