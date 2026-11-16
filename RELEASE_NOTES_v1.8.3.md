# S Caller v1.8.3 — Call-start stability preview

This source update focuses on preventing app crashes and premature in-call screens when starting a cellular call.

## Changes
- The outgoing call screen opens only after Android Telecom reports an actual call.
- Rapid repeated taps while awaiting Telecom are ignored, with a 10-second timeout.
- Telecom call-state snapshot exceptions are logged instead of crashing the app.
- InCallService call-monitor registration, foreground UI launching, and notification errors are contained and logged.
- Existing contacts, call history, UI and SIM routing logic are retained.

## Verification and limitations
- GitHub Actions unit tests and APK compilation are required before distributing this build.
- Successful APK compilation does **not** prove real SIM calling works on every manufacturer/device. Verify outgoing/incoming SIM calls, dual-SIM selection, answer, hang-up, Bluetooth/speaker and lock-screen behavior on physical hardware.
- If the app still closes, capture the relevant Android `FATAL EXCEPTION` log and Android model/version.
- Build artifacts from GitHub Actions use a CI-generated **debug signing key**. Its certificate can differ from previously installed builds; an APK with a mismatched certificate cannot install over an existing package. Back up app-local content before considering a reinstall.
- No covert or automatic cellular call recording is enabled. Recording requires a supported capability and appropriate notice/consent.
