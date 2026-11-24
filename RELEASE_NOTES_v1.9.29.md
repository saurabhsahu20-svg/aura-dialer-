# S Caller v1.9.29 — Show Saved Contact Name While Dialing

## Reported issue
A full saved phone number entered on the keypad showed only "1 contacts (tap to view)", instead of the contact's saved name.

## Fix
- When the complete dialed number matches a saved contact, display the **saved contact name** in the number card above the digits.
- Continue displaying the exact number itself in the same large number field. Keep the label **one line tall** so the fixed number-key positions never move while typing.
- Match correctly across Indian mobile number formats (10-digit national, leading zero, +91, 91) while preserving full international country codes. A partial number, T9 suggestion, extra digit, or similar suffix must **not** be treated as an exact caller identity.
- Keep the explicit tap-to-view suggestion dialog for partial matches and ambiguous searches. Put the exact result first when there are other partial suggestions.
- Run matching in a cancellable background coroutine with a short delay and attach the search query to results so a stale saved name never appears during a new keypress.
- Regression tests verify exact vs partial/international cases, stale name clearing and keypad geometry stability.

## Retained from previous releases
- The no-jump keypad layout, missed-call badge improvements, proximity touch protection, call history, WhatsApp and Telegram shortcuts, and existing Android Telecom functionality.
- Version 1.9.29 / versionCode 129; unchanged package `com.scall.phone`.

## Limitations
- Contact names depend on Android READ_CONTACTS permission and on the number being saved in the phone's contacts.
- Real-device contact provider, calling and update-install testing remain necessary.
- APKs signed with a different key **cannot install over an existing v1.9.21**. Do not remove your installed app without backing up data or finding its original signing key.
