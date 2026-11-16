# S call Audio Architecture — Supported & Unsupported Controls

## Current calling architecture

**S call is a default Android Telecom dialer** using `InCallService`, Android `Call` objects and Telecom call-audio routing. The actual SIM/cellular uplink/downlink, microphone capture, hardware/firmware DSP, network codec (including wideband/HD voice) and modem are controlled by Android, the OEM and the mobile carrier.

As a third-party dialer, S call must **not** try to open an `AudioRecord` to capture protected SIM-call audio. It cannot attach `NoiseSuppressor`, `AcousticEchoCanceler` or `AutomaticGainControl` to another system's audio session. There is no real on-device AI filter running on the SIM call in this version.

## Supported improvements in v1.8.0

- Android 6–13: Telecom `CallAudioState` reports available routes. The app uses supported legacy `setAudioRoute` for earpiece, speakerphone, Bluetooth or wired headset.
- Android 14+: Telecom `CallEndpoint` reports device endpoints and changes. The app requests a supported endpoint via `requestCallEndpointChange`, then updates UI only after callbacks.
- Audio route failures are explained to the user, with safe return to wired or earpiece when turning off speaker.
- In-app audio capability inspection calls only `isAvailable()` for three device audio effects; it never accesses microphone input or claims an active effect.
- Standard call media and audio focus remain under Android Telecom; S call does not install unnecessary audio services.

## Roadmap for real AI/VoIP processing

A distinct, optional VoIP calling product would need secure user-authorized signaling, transport and identity, an app-controlled `AudioRecord` / `AudioTrack` stream or WebRTC Audio Processing, feature detection and conflict prevention for system AEC/AGC/NS, mic permission handling, low-latency performance tuning, and real end-to-end audio testing. Only that distinct stream could be processed by application-owned on-device audio effects. Until implemented and verified, the app must not display an "AI Noise Cancellation active" indicator.

## Privacy

No recording, background microphone access, transcript generation, call-audio upload or SIM-call interception is added by the current release.
