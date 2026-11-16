# S call — Recording Capability and Privacy

This repository implements an Android `InCallService` default dialer. **InCallService has no public API for tapping the two-sided audio of ordinary SIM/cellular calls.** Android's `MediaRecorder.AudioSource.VOICE_CALL`, `VOICE_UPLINK` and `VOICE_DOWNLINK` require the system-reserved `CAPTURE_AUDIO_OUTPUT` permission, unavailable to this app.

**Current capability:** import, manage, listen to and explicitly share recordings created by a device/system recording feature where available and where users have appropriate participant consent.

**Automatic SIM recording:** unavailable and never silently enabled.

**Manual SIM recording:** unavailable; a third-party app may not bypass protected telephony audio. Capturing an ordinary microphone as a workaround would be unreliable and could interfere with the live call. This app does not attempt that.

**VoIP recording:** not yet available because S call does not currently operate its own VoIP media pipeline. Adding VoIP recording in the future would require separately implemented and tested capture/encode/media transport, clear participant notice, permission handling and active-recording indicators.

**Local file security:** imports are copied into app-private `filesDir/recordings` with atomic metadata updates. Audio sharing uses a non-exported Android FileProvider and user-triggered temporary permission grants. There is no automatic external upload or device microphone access. This does not constitute biometric protection or full file encryption at rest.

**Cleanup:** explicit, confirmation-based manual cleanup; no silent automated deletion.

**Backup:** files are private and Android backup is disabled for this app. Use explicit user-initiated sharing/export before uninstalling if a recording must be retained.

For details, see `QA_REPORT_v1.8.1.md` and `RELEASE_NOTES_v1.8.1.md`.
