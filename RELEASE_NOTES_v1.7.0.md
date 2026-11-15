# S call v1.7.0 Preview — Clear Incoming / Outgoing / Missed Call Symbols

This preview refreshes the Call History interface so the three common calling directions can be recognized at a glance, without relying only on color.

## New directional call icons

- 🟢 **Incoming:** green **down-left arrow** (Call Received), plus a clearly written **Incoming** label.
- 🔵 **Outgoing:** blue **up-right arrow** (Call Made), plus a clearly written **Outgoing** label.
- 🔴 **Missed:** red **missed-call arrow** and **Missed** label; missed caller names are accented for visibility.
- A small circular direction badge appears on each caller photo/initials; the same direction symbol and label also appear below the name.
- Icons use dark-theme-aware foreground colors for legibility and accessibility.

## Faster call history

- New horizontally scrollable filters: **All**, **Incoming**, **Outgoing**, **Missed**, showing a call count for each.
- A header summarizes recent call totals and missed calls.
- Display the actual call date/time and **connected call duration** where available.
- A polite empty state appears when the chosen filter has no matching entries.
- Multi-select **Select visible** works within the current filter, with confirmation before permanent history deletion.
- Saved personal history labels and notes continue to work.
- Rejected/blocked/unknown system call types are left as **Other**, never falsely marked Incoming, Outgoing, or Missed.

## All previous features retained

Interactive dialpad, corrected Android Back navigation, preferred voice SIM / Android default / Ask Every Time, full-screen caller photos, mint-green icon, longer smooth call sliders, light/dark themes, and call history editing.

## APK verification and downloads

**Direct APK:** [S-call.apk](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.0-preview/S-call.apk)

**ZIP fallback:** [S-call-v1.7.0-install-package.zip](https://github.com/saurabhsahu20-svg/aura-dialer-/releases/download/v1.7.0-preview/S-call-v1.7.0-install-package.zip)

The Android workflow runs unit tests, verifies APK compilation, package name, version code/name, APK and ZIP integrity, and SHA-256 checksum.

**Debug preview only:** Actual Android device calling, SIM behavior, incoming-call UI, and phone gesture navigation remain unverified. Call logs represent the real OS record and this update does not modify timestamps/call types. Keep the preinstalled phone app available, particularly for emergency calling.

**App package:** `com.scall.phone` | **Version:** 1.7.0 (version code 10) | **Minimum Android:** 8.0.
