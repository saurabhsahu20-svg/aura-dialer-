# S call device validation checklist

Before setting this as an everyday dialer, use a secondary phone and test on the actual Android device:

- [ ] Android asks to make S call the default phone app and retains this role.
- [ ] Call from keypad starts a cellular call (with permission and an active SIM).
- [ ] Call to a saved contact shows correct name and photo.
- [ ] Right swipe on contact row calls the intended number and needs the threshold.
- [ ] Left swipe opens the intended SMS conversation.
- [ ] Incoming call notification arrives with screen off and screen on.
- [ ] Full-screen caller screen works on lock-screen when platform permits.
- [ ] Drag right answers; drag left rejects; accessible tap actions also work.
- [ ] End-call, mute, speaker, DTMF, and applicable hold/add/merge controls work.
- [ ] Contact photo chosen in editor persists after force-stop and restart.
- [ ] Contacts, favorites, and recent calls respond to permission denial.
- [ ] Dual-SIM selector routes a call through the selected account on this device.
- [ ] Call UI responds to a second incoming call, and returns to ongoing call correctly.
- [ ] In-call screen remains stable through rotation and app background/foreground.
- [ ] Settings for gesture and theme persist after restart.
- [ ] Emergency calling is **not** tested through this app. Android delegates to the preinstalled phone app.

Build verification in CI is necessary but **does not prove** real-world phone behavior.

## S call v1.1.0: Caller Photo Studio

- [ ] Open Contacts and tap the small photo icon beside a contact.
- [ ] Choose a gallery image and see it in the live incoming-call preview.
- [ ] Use pinch and drag to reframe the photo; adjust Zoom and Tone.
- [ ] Tap Reset and confirm zoom/position/contrast return to defaults.
- [ ] Tap Back without Save and confirm the previous stored image is unchanged.
- [ ] Tap Save look and reopen the editor to confirm framing persists.
- [ ] Place and receive a real call and confirm the custom framing and contrast appear.
- [ ] Tap Remove photo and confirm the custom photo is cleared.
- [ ] Test long names, low-light photos, Hindi names, phone with gesture navigation, and screen rotation.
