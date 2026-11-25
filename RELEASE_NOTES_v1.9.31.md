# S Caller v1.9.31 — Local Hang Up Button Reliability

## Reported failure
When the *other person* ends the call, S Caller closes normally. When the **S Caller user presses the red End Call button**, the call remains active or the in-call display stays visible.

## Root-cause-oriented fixes
1. The proximity touch shield installed in v1.9.26 could swallow all touch events whenever an OEM reports "near", even if the call screen is visible and the user is deliberately trying to hang up. The End Call control now reports its actual on-screen bounds to CallUiActivity. A gesture starting inside that rectangle is always delivered to the End Call button, while the shield continues protecting other call controls against accidental face touches.
2. The normal Android `Call.disconnect()` action remains the first, preferred mechanism. If a single ongoing (active/connecting/dialing) call is still present 1650ms after pressing End, Android 9+ may try `TelecomManager.endCall()` as a secondary fallback **only when the app has `ANSWER_PHONE_CALLS` permission**. This deprecated API may not be honored on every device; Android rejects emergency termination. The app never invokes it for multiple calls or incoming ringing.
3. Explicitly requests call-management permission in the default-dialer permission setup where supported.
4. Existing Telecom state verification and call-screen close-on-real-termination from v1.9.30 are retained.

## Checks and limits
- Unit tests cover red button touch bounds, rejection of non-red touches, and fallback eligibility, including denied permission and multiple active calls.
- Requires passing Android build/lint and emulator checks; **real cellular call, physical proximity sensor and Moto/OEM handsets still require actual device tests**.
- Device must set S Caller as the default Phone app and grant Phone/Answer phone calls permissions for all available supported behavior.
- Original Android package remains `com.scall.phone`. v1.9.31 / versionCode 131.
- A preview APK signed with a new CI debug key still **cannot install on top of existing v1.9.21** with a different signing certificate. Preserve previous data and signing key. Do not uninstall the current dialer without a safe alternate phone app and backup.
