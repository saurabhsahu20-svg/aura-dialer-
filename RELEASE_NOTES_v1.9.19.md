# S Caller 1.9.19 — Hangup response preview

- The red End call button now sends an immediate Android Telecom disconnect request.
- UI stays responsive; accidental double taps are ignored for only 400 ms.
- For the SAME still-active call, controlled follow-up requests at 450 and 1200 ms; never hang up an unrelated incoming call.
- A visible status appears while Telecom processes the request or if the call is still active.
- Select active call before waiting call, including during call waiting.
- Existing chat shortcuts remain in the project.

## Verification
CI runs unit tests and assembles a debug APK. The Android telephony modem/network owns final disconnect timing, so a real call test is still necessary. Debug APK signing is not guaranteed to match the certificate installed on the handset.
