# S Caller v1.9.36 — Predictable Back Navigation

## Reported bug
Switching Contacts → Favorites → Keypad (or other bottom tabs) caused Android Back and the UI Back arrow to step through every tab visited. The app required repeated Back presses, which felt like a slow/hanging navigation loop.

## Fix
- Treat **Recents, Contacts, Keypad and Favorites** as sibling primary tabs, not a chronological back-stack.
- Switching any bottom tab clears the old tab history. Android system Back and gesture Back from Contacts, Keypad or Favorites now return directly to **Recents** in one press.
- Back on **Recents** exits the app naturally instead of trapping the user in previously visited tabs.
- Secondary pages (**Settings**, **SIM Manager**) retain the correct direct parent page. Back from SIM Manager opened in Settings returns to Settings, then the originating main tab; subsequent Back from any main tab returns to Recents.
- Contact details, photo editor, selection mode, and search clearing continue to take precedence over page navigation, so no data is silently lost.
- Existing saved back-stack data from older releases cannot force multiple old-tab Back steps.
- Caps genuine nested history at 8 entries and uses zero stack entries for primary tab switches, reducing unnecessary saved state and navigation work.
- Updates Settings version label to 1.9.36; Android versionCode 136. Package remains `com.scall.phone`.

## QA
- Automated unit regression tests cover multi-tab switching, direct root return, secondary page parent navigation, stale saved tab history, same-page navigation and bounded nested stacks.
- Require passing build, Android Lint and emulator compatibility tests before publishing the updated APK.
- Real-device Android Back gesture, different keyboard/search states, and other OEM behavior still require field verification.
- The existing v1.9.21-to-new-debug-APK signing certificate mismatch is **unchanged**: in-place update cannot succeed without the original matching signing key. Do not uninstall or delete data without backups.
