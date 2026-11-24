# S Caller v1.9.24 — Keypad root layout fix

- Root-cause correction for failing Android emulator Keypad smoke test: the parent Compose Column measured an unweighted KeypadScreen with unbounded height, so nested weighted digit layout had no usable viewport. Wrap only the KeypadScreen in a weighted Box to guarantee a bounded full-height slot.
- Carry forward v1.9.23 explicit digit-row spacing, small-screen scrolling, smooth T9 suggestions.
- No change to colors, branding, tabs, contact photos or feature locations.
- Keep improved India/international contact matching, complete call-log history, expandable actions, native WhatsApp/Telegram shortcuts, call hangup guarded retries and partial-deletion safety.
- Validate with unit tests, lint, API 23/26/35 and API29 UI smoke tests. Live cellular calls remain hardware/network dependent.
- In-place upgrade still requires matching APK signing certificate.
