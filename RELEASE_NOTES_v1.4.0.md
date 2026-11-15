# S call v1.4.0 Preview — Call History Manager

**New:** Edit and remove individual entries from the Recent Calls screen.

## Features

- **Edit an entry:** Choose **Edit** on a recent call and save a personal display name or private note.
- **Edit only that call:** Other calls from the same number are not renamed.
- **Delete one call:** Tap **Delete** below the selected history entry, then confirm the permanent action.
- **Delete multiple:** Tap **Select**, check the calls you want to remove (or **Select all** among the currently displayed results), and tap **Delete**.
- **Confirmation before deletion:** No deletion happens until the user confirms.
- **Search custom notes:** Recents search can match the local display name or note.
- **Permissions:** Requires S call to be set as your default phone app and Android to grant `WRITE_CALL_LOG` for deleting system call history. If Android denies access, S call reports the failure rather than falsely claiming to have deleted data.
- **Privacy:** Custom names and notes remain inside S call on this device. They do not modify the system's actual call number, timestamp, call type or duration.

Existing functionality retained: full-screen caller photos, 68% contact swipe-to-call, 82% incoming-call slider, mint/light-green app icon, phone line selection, SMS SIM suggestions, and Light/Dark/AMOLED themes.

## How to use

Open **Recents → Edit** to change a call's display label or add a personal note.
Open **Recents → Delete** to remove one entry, or **Recents → Select** to remove several.
Grant the default-phone role and call log permissions as prompted.

## Important notes

- This is a debug-signed preview, not a fully device-validated production release.
- Removing a system call-log entry is permanent and can affect other dialer apps displaying the same history.
- Android controls restricted call-log permissions; not all devices or installers can grant them.
- Custom labels and notes are local presentation changes, not fabricated or altered carrier call records.
- Real phone/carrier behavior and the deletion feature still require testing on an Android device.
- Keep the system dialer available while evaluating a new alternative dialer, particularly for emergency calls.

Build: GitHub Actions, JDK 17 / Android SDK 35 / Gradle 8.10.2, with gesture unit tests.
Source: https://github.com/saurabhsahu20-svg/aura-dialer-
