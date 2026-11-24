# S Caller v1.9.22 — Stability and Accuracy

No visual redesign, branding or feature removal. Package ID remains com.scall.phone.

- Fixed the oversized gap below keypad digits seen in Android 6, 8, 10 and 15 emulator smoke tests by ensuring scrolling keypad content fills its viewport. Retains compact-device scrolling.
- T9 matches now stop at three suggestions, improving digit-by-digit responsiveness.
- Correct contact-name matching for national Indian (+91/0/local) formats while preventing wrong international last-ten-digit matches.
- Safely handle partial call-log deletion; do not erase notes for calls which may remain.
- Ensure emulator UI tests trigger after source changes, not only Gradle changes.
- Disable automatic obsolete v1.9.18 rebuild/reupload workflows that were generating stale red errors; history and manual triggers remain.
- Preserve existing green S Call visual theme, Call History unlimited display, expandable actions, WhatsApp/Telegram shortcuts, SIM controls, call photos, and guarded red hangup button.

Build and unit tests must pass. Android emulator tests and real SIM-call testing are separate checks. The app can update without uninstall only if its APK certificate matches the installed version; CI debug signing alone does not guarantee compatibility.
