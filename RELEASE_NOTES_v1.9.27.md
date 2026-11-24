# S Caller v1.9.27 — Stable Keypad / No Accidental Contact Selection

## Screenshot-reported bug
Typing a number (for example, 88) automatically displayed up to three matching contacts *above* the keypad. Those cards pushed digit rows down and beyond the visible screen. The moving targets could select a contact or an unexpected digit.

## Fix
- Dialpad digit rows now use a **fixed available viewport**. Their height and position are completely independent of suggestion count and typing state.
- Matching contacts are accessed by an **explicit tap on the single-line “Phone number • N contacts (tap to view)” label**. A confirmation dialog lists matching people and only selects a number after an intentional tap.
- The previous automatic list of large contact cards between chat shortcuts and dialpad has been removed. No layout shift or unintended contact selection on each typed digit.
- Contact searching is cancellable, debounced by 60ms and performed on `Dispatchers.Default` to protect touch responsiveness on large contact books.
- Four rows of keypad digits retain stable layout on small and large displays; use scrolling only where even the minimum key targets do not fit.
- Unit tests verify stable dial row sizing; an Android Compose regression test types a matching number, verifies key bounds stay constant, and verifies contacts are only selectable after opening the dialog.

## Compatibility / limitations
- Version `1.9.27` (Android versionCode 127), original package `com.scall.phone`.
- Existing v1.9.26 Telecom proximity screen protection, Call History, direct WhatsApp/Telegram and other functions preserved.
- Real-device calling, digit touch accuracy, switch-between contacts and call mode, and different display/font settings need testing on real phones.
- **This preview still does not fix v1.9.21 signing compatibility.** A debug APK signed with a different key cannot update an installed v1.9.21 without uninstalling; do not delete the old app without a backup and a safe dialer fallback.
