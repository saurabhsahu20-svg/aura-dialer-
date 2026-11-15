# S call v1.5.0 Preview — SIM Visibility & Reliable Calling

This version fixes the issue where **SIM 1 / SIM 2 options could be missing or hard to find**.

## What changed

- **Prominent SIM & Network shortcut** on the main screen (including Recents, Contacts, Favorites and Keypad), with a **Choose** action.
- **Detected SIM cards** section shows the visible active subscriptions, their physical SIM slots and carrier labels when Android exposes them.
- **Clear setup buttons** for **Allow Phone permission** and **Make S call default Phone app**, directly on the SIM screen.
- **Refresh SIM details** when you return from Android settings or grant permissions, plus a manual Refresh button.
- **Voice calling preferences:** **Ask before every call**, **Use Android default SIM**, or choose a real Android Telecom calling line.
- **Safer call routing:** if a saved calling line goes missing, S call prompts rather than silently using the other SIM. If Android shows two SIMs but exposes no selectable Telecom accounts, S call directs you to SIM settings instead of guessing.
- **System SMS and Mobile Data information** remains available. The actual SMS-sending SIM is decided by the messaging app; Android settings manage the mobile-data SIM.
- **Automated SIM routing tests** added alongside existing swipe gesture tests.
- Original **light-green S call icon**, long smooth call sliders, contact caller-photo editor and call-history edit/delete features remain.

## Where to find SIM controls

1. Open S call and find the **SIM & Network** card near the top.
2. Tap **Choose**. If prompted, grant **Phone permission** and make **S call your default Phone app**.
3. The **Detected SIM cards** area shows SIM 1 / SIM 2 when available.
4. Under **Voice calls**, choose **Ask each time**, **Android default SIM**, or a specific *selectable phone line*.
5. If the voice line list stays empty, open **Android SIM settings** from the same screen.

## Important Android limitations

- Active SIM subscriptions and Telecom calling accounts are different Android APIs. Not every SIM visible in Android can be exposed as a selectable calling account by an ordinary third-party dialer.
- Only available Android Telecom phone accounts can be used to route a call explicitly. S call never guesses which account corresponds to a physical slot just from list order.
- Mobile Data default SIM cannot be changed directly by an ordinary dialer. SMS defaults are controlled by Android and the messaging app.
- Screens, carrier support and available options vary by phone and Android version.

## Verification

- GitHub Actions Android APK build: **passed**.
- Automated SIM route and swipe gesture unit tests: **passed**.
- **Physical phone tests:** not yet performed. Incoming call handling, the user's SIM hardware, dual-SIM routing and lock-screen UI require device-specific testing.
- This is a **debug-signed test APK**, not a store production release.

**App:** S call  
**Version:** 1.5.0 (versionCode 7)  
**Android:** 8.0+  
**Source:** https://github.com/saurabhsahu20-svg/aura-dialer-

Please keep the stock phone app available when evaluating a new dialer, particularly for emergency calls.
