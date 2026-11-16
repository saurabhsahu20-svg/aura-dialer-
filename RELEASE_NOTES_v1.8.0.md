# S call v1.8.0 Preview — Premium Audio Routing & Honest Voice Capabilities

This release improves calling usability and safe audio output management across Android 6.0+ devices. Existing **S call** branding, light-green icon, contacts, dialer, caller photos, call history, high-contrast call-type symbols, SIM preferences and call-screen recovery remain unchanged.

## Improvements shipped

### Reliable route switching during a SIM call

- **Android 14+ (API 34+):** Added Telecom `CallEndpoint` callbacks for available earpiece, speakerphone, wired headset, Bluetooth and external audio endpoints. Uses Android's `requestCallEndpointChange` to select a device, then awaits the system's actual route-change event.
- **Android 6–13:** Supported Bluetooth, wired, earpiece and speakerphone routes continue to use the compatible Telecom `CallAudioState` and `setAudioRoute` APIs.
- **New in-call Audio picker:** Choose the output actually reported by Android; available routes change when a headset is connected/disconnected.
- **Safer speaker toggle:** Switching speakerphone off returns to an available wired headset first, then earpiece or Bluetooth—not blindly to an unavailable route.
- **Visible current output and errors:** The in-call screen displays the current audio device. Failed routing attempts show a useful message rather than pretending success.
- **Mute state and available outputs:** New Android mute/endpoint callbacks update the active-call screen; no fabricated audio state is displayed.

### Voice capability status (transparent and private)

- **Settings → Voice quality & audio capabilities** reports availability of system noise suppression, echo cancellation and automatic gain control **for app-owned microphone capture sessions**.
- The capability check **does not access the microphone, record voice, open an AudioRecord or enable AudioEffects**.
- Standard SIM/cellular calling audio is owned by Android, the phone's modem and carrier. **S call does not claim to enable AI noise cancellation, HD Voice or DSP effects on SIM calls**, as doing so would not be supported by the Android Telecom dialer architecture.
- No hidden recording, remote audio upload, VoIP backend or background microphone capture was introduced.

### Prior stability improvements retained
- Reconnect Android Telecom calls after answering and avoid endless "Connecting to phone service…" spinner.
- Keep CallScreen, mute/speaker/keypad/End Call controls and full-screen caller photos.
- Support Android 6.0+ with version-appropriate notification and SIM API fallbacks.

## Verified download files

**[S-call.apk — Direct Android APK](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.0-preview/S-call.apk)**

**[ZIP installation backup](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.0-preview/S-call-v1.8.0-install-package.zip)**

**[SHA-256 checksum](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.8.0-preview/S-call-SHA256SUMS.txt)**

## Testing and limitations

GitHub Actions performs Android unit tests (including audio route-planning and truthful indicator checks), Lint, APK compilation, signature/package/version/minSDK verification, ZIP integrity and SHA-256 validation. Android emulator tests provide UI and compatibility smoke coverage.

**Not physically tested:** real SIM microphone speech clarity, live wind/traffic/crowd noise suppression, echo reduction, Bluetooth/wired headset hardware switching, carrier HD Voice or end-to-end phone-call latency. Actual audio improvements vary by phone hardware, OS and network. **This is a debug-signed Preview APK, not a Play Store release.** Existing debug certificates may differ between CI builds; back up local app data before uninstalling old versions. Keep the manufacturer's stock dialer available for emergencies.

## Next architecture milestone
To add real WebRTC Audio Processing, S call would need a separate **opt-in, app-controlled VoIP feature** with its own audio capture/playback, call signaling, secure media transport and user-facing privacy controls. This release intentionally does not present non-existent VoIP processing as active.
