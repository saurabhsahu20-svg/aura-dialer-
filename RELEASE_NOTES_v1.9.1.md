# S Caller v1.9.1 — Original Bottle-Green Calling Symbol

This update replaces the S Caller launcher and in-app brand icon with the **exact bottle-green phone handset and three signal waves** selected by the user. No text, letter S, or extra ornament was added to the symbol itself.

## What changed
- A dark emerald launcher icon designed from the provided transparent PNG.
- Updated legacy Android icon resources for mdpi, hdpi, xhdpi, xxhdpi and xxxhdpi, including round variants.
- Updated Android 8+ adaptive launcher icon foreground and matching dark emerald background.
- Updated the logo displayed inside S Caller.
- All other app UI, dialer, call handling, SIM selection and audio routing are unchanged.
- Manual and automatic recording remain absent; no new permissions.

## Verification
- Source icon file derives from the user's supplied symbol; it is a 192 px optimized PNG used in each launcher resource.
- GitHub Actions requires successful unit tests, Android lint, debug APK build and validation of the actual packaged icon and version.
- Emulator smoke tests cover the UI; a real phone is still required to confirm device-specific call compatibility.

## Installation and release status
This is a **public debug-signed developer preview**, not a production-signed APK.
Android may reject in-place installation if your old APK was signed with a different key. Back up any app-private data before uninstalling. Keep a functioning default system Phone app available.
