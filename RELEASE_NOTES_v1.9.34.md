# S Caller v1.9.34 — One Missed Call = One New Alert

## Customer-reported bugs addressed
- One missed call was sometimes represented repeatedly or with a higher-than-expected badge count.
- Notifications could be rebuilt multiple times during Android Telecom's repeat state callbacks.
- New missed calls sometimes appeared late because Recents only refreshed when returning to the app.
- Users wanted the count to reset after reading Recents without losing the actual call log.
- Repeated full data scans could cause UI jank on contact-heavy devices.

## Changes
- The missed-call badge now counts **unique call-log events**, avoiding duplicated provider rows with the same ID or near-identical phone number and time (within 1.5 seconds). Distinct real missed calls remain separate; full call history is never truncated or merged.
- The missed-call read marker is still persisted locally across app restarts; on the Recents page, new missed alerts remain briefly visible and then are acknowledged as viewed.
- If S Caller is the authorized default Phone app with WRITE_CALL_LOG, **only newly viewed missed rows** are marked `NEW=0`, `IS_READ=1` in the Android call log, asynchronously. No call-log entry is deleted.
- Android Call Log change observer with a 400 ms debounce refreshes Recents without needlessly reloading contacts every time, reducing UI lag and multiple duplicate refreshes.
- CallStyle notification publishing is deduplicated by call identity, call state and caller name. Retries of the same incoming state do not repeatedly post the same notification, while a new call or real caller identity change still refreshes it.
- Existing v1.9.33 caller visibility, v1.9.31 hangup safety, stable keypad, exact contact matching, WhatsApp and Telegram shortcuts remain.
- Added regression tests for single/duplicate missed events, distinct actual calls, cross-country phone numbers, viewed history and repeated Telecom notification callbacks.

## Quality and important limits
This is a targeted reliability update, not a claim that every OEM-specific bug is eliminated. On-device cellular calling, SIM, proximity, notification-shade, lock-screen incoming-call and Bluetooth testing remain necessary. Android system notifications and full-screen behavior remain subject to user permissions and manufacturer settings.

Version: **1.9.34** (Android versionCode **134**); package: `com.scall.phone`.

**APK signing compatibility:** New CI debug APKs may have different signing certificates from the user's currently installed v1.9.21; they cannot update one another in place without a matching signing key. Do not uninstall an existing default dialer until data is backed up and a safe alternative is configured.
