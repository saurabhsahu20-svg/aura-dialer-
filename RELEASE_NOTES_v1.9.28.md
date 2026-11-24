# S Caller v1.9.28 — New Missed Calls Only

## Bug reported
The Call History heading repeatedly displayed the **lifetime missed-call total** as though it were a new missed-call alert. The count did not return to normal after viewing calls.

## Fix
- The Recents header now says **“N calls”** by default. Only when there are unread, newly missed calls does it show **“N calls · M new missed”**.
- The Recents navigation tab shows a numeric **badge for new missed calls only**; no historical missed-call total is used for notifications.
- Opening the Recents tab, opening the Missed history filter, or choosing **“Mark missed as viewed”** acknowledges the currently loaded missed calls and immediately clears the badge.
- A private, persistent marker (latest seen missed-call timestamp + call-log row ID) prevents old missed calls from reappearing after app restarts, page changes, refreshes, or call-log reorderings.
- On the first run after this update, old missed calls establish the baseline rather than flooding the badge; subsequent missed calls are counted normally.
- Call History and Missed history **retain all actual older entries**. The history filter is intentionally labelled `Missed history` so its total cannot be mistaken for unread alerts.
- New incoming, outgoing or rejected calls never increase the missed badge; tests cover repeated refreshes, tied timestamps, and cleared/viewed state.

## Platform and device limits
- This is S Caller's in-app badge. Android system-owned missed-call notifications and their clearing behavior depend on the device's default Phone app and system notification settings; this change does not forcibly delete or modify the system call log.
- Read call-log permission is required to calculate the in-app count. If access is denied, no fake count is shown.
- APK version 1.9.28 (versionCode 128), package `com.scall.phone` unchanged. In-place upgrades from v1.9.21 still require the original matching signing certificate/key.
- A passing CI build is not a substitute for real incoming/missed-call device testing.
