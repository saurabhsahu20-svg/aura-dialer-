# S Caller v1.9.21 — Full call history and tidy action menus

- Removed hard-coded cap of 200 call-history rows. The All and missed/incoming/outgoing totals now reflect the actual entries returned by the Android CallLog provider.
- Data load stays on Dispatchers.IO and LazyColumn renders only rows in view. Call type counts are memoized.
- Recents entries initially show only the call item (plus its small expand arrow). Tap the item to expand WhatsApp, Telegram, Details, Edit and Delete. Tap it again to collapse. Only one entry is expanded at a time.
- Select mode stays separate; deleting call history still requires explicit confirmation.
- Large multi-select deletions now use batches of 100 instead of silently truncating at 200.
- Existing direct chats in Contacts, Favorites, Contact details and Keypad, and guarded end-call fixes remain.

CI tests and build verify source; live device tests remain necessary. The signing certificate of the CI debug APK may differ from a prior installed version.
