# S Caller 1.9.3 — Contact Management Developer Preview

## Implemented in the existing S Caller package
- Premium full-screen **New Contact** editor with first name, last name, initial mobile number, Save, Cancel and expandable optional details.
- Real Android **ContactsContract** insert/read/update/delete using background dispatcher and atomic contact-provider operations.
- Supports multiple phone numbers and emails, nickname, middle name, birthday and anniversary, work/company/title/department, addresses, website, relationship and notes where supported by the selected Android contact account.
- Securely choose a contact photo from Android's document picker; downsample and write to the contact provider only when Save is pressed. Existing contact photos can be replaced or removed.
- Choose a local device account or a contact account exposed by the system provider. Account support varies with the actual account/phone; no claim of cloud sync.
- Contact profile with accessible **CALL | MESSAGE | EDIT**, number selection for multi-number contacts, favourite toggle, vCard share through supported Android provider, and delete confirmation.
- Contacts/Favourites/Groups selector; Groups and membership reflect the device's Android contact provider.
- Contact search by name, nickname, company or any stored phone number; one row per contact in the main list.
- Recent call summary on the contact profile uses only call records that the app has actually loaded with Android permission.
- Duplicate warning for an existing phone number; it does **not** automatically merge or overwrite.
- User-directed **Import VCF** opens a compatible system Contacts importer; not all devices have one. **Export VCF** creates a basic text vCard file at a user-selected SAF location.
- The original green S Caller branding, Android Telecom calling, keypad, SIM switching, call history, other UI and privacy protections remain. Automatic and manual call recording remain absent.

## Limitations — not yet provided or certified
- **This is not a complete backup/restore implementation**: basic VCF export includes names, numbers, emails, notes, company, title and birthday, but not contact photos, group metadata or all synced-account fields.
- VCF import delegates to an Android contact importer; no custom full-format import parser is claimed.
- Only one writable raw contact per aggregated person is edited. Information in other linked accounts remains untouched; some account providers may reject editing.
- Full duplicate merge, creation of groups, custom encrypted data fields, automatic reminders, gender/pronunciation fields and advanced A–Z fast scrolling are not implemented.
- Account syncing, availability of VCF import, device contacts write capabilities and physical SIM calling must be verified on the user's specific Android phone.
- Real device call crashes cannot be declared fixed by UI tests.

## Privacy and release policy
- READ_CONTACTS and WRITE_CONTACTS are requested through Android; write permission is requested only on user-initiated contact editor actions.
- No app-managed contact upload or external contact server.
- No automatic public GitHub release. The work remains in a **draft PR** until the project owner authorizes publication.
- Built APK, if made available, is a debug-signed testing preview, not production signed. Preserve your personal data and maintain a working system Phone app.
