# S Caller v1.8.4 — Telecom reliability preview

## Fixes
- Prevent S Caller from re-opening its own dial pad when the default Phone role is lost during call placement. Only Android Telecom places outgoing calls.
- Keep phone calls available with CALL_PHONE permission when READ_PHONE_STATE is denied; Android chooses the system default SIM and SIM selection remains unavailable until permitted.
- Handle OEM Telecom callback exceptions in the call bridge and InCallService lifecycle.
- Avoid process termination if a call ends during answer, reject, hold, merge, disconnect or DTMF key actions.
- Recover from provider-specific contact and call-log query errors.
- Handle default Phone role and permission request errors without app closure.
- Add four unit tests for call-launch access decisions and run cross-version Android UI checks on pull requests.

## Verification criteria
- `gradle :app:testDebugUnitTest :app:assembleDebug` must succeed.
- Android 6 (API 23), Android 8 (API 26), and Android 15 (API 35) emulator UI smoke tests should pass.
- A successful emulator test does NOT verify cellular call placement on a real SIM. Validate outgoing and incoming calls, dual SIM selection, emergency call handoff, speaker, Bluetooth, and call disconnect on actual devices.
- The preview APK is signed with a CI debug key, may be incompatible with the previously installed signature, and is **not** a production-signed release. Back up important app-local data before uninstalling older builds.
- Recording behavior has not been changed or made covert. Recording can only be offered when supported and with appropriate notice/consent.
