# S Caller: install updates without uninstalling

Android permits an APK to update an installed app **only if** the application ID is unchanged, the new APK's versionCode is higher, and the signing identity matches the installed app (or an Android-supported signing lineage is used). An APK cannot recover its signing private key.

## Diagnosis from supplied S Caller APKs

Both packages use `com.scall.phone`, but the supplied APKs have different SHA-256 signing-certificate fingerprints:

- S Caller v1.9.3 Contacts Preview: `0C:99:E5:84:76:C0:66:3E:2E:33:20:BE:06:16:D9:F3:78:A5:A0:ED:03:A7:EF:80:C1:92:2E:38:B6:10:DF:91`
- S Caller v1.9.7 Call Response Preview: `75:2C:4E:6C:80:C7:7B:9C:2B:AF:34:DB:A2:6D:5D:27:3B:86:43:FB:93:E7:88:DF:32:6D:1B:C9:D2:35:AC:B4`

These certificates **cannot** be used interchangeably for regular in-place upgrades. A random GitHub Actions debug APK gets a new runner-generated debug key, causing installation/update failure. Increasing versionCode alone does not solve this.

## One-time setup for a verified v1.9.8 update

1. Identify the APK signing certificate of the app actually installed on the phone. If unsure, preserve the existing app and its data, and obtain a copy of the same APK for inspection. A fingerprint from a **different** version does not prove the phone's installed identity.
2. Locate the **original private keystore** used to sign that installed APK (often a saved project's `debug.keystore`, or a release `.jks`), along with its store password, key alias and key password. A certificate/public APK alone is not enough. Never commit a keystore or passwords into a public repository.
3. In GitHub repository Settings → Secrets and variables → Actions, add repository secrets:
   - `SCALL_SIGNING_STORE_B64`: base64 encoding of the ORIGINAL keystore file (single line).
   - `SCALL_SIGNING_STORE_PASSWORD`: existing keystore password.
   - `SCALL_SIGNING_KEY_ALIAS`: existing key alias.
   - `SCALL_SIGNING_KEY_PASSWORD`: existing key password.
   - `SCALL_EXISTING_CERT_SHA256`: SHA-256 certificate fingerprint of the app **currently installed**.
4. Run `S Caller v1.9.8 Verified In-Place Upgrade APK` from GitHub Actions after merging the signing workflow, using the source branch that contains the desired updates.
5. The workflow builds the signed release APK, verifies its signature against the supplied fingerprint, checks application ID `com.scall.phone` and versionCode `36`, and creates a downloadable CI artifact **only** when all checks pass.
6. Download the validated APK from that workflow's Artifacts. Install over the current app without uninstalling; retain contacts/profile/preferences stored on the app. It remains important to back up device data beforehand.

If the original private key is unavailable, **an in-place update to that installed certificate is not possible**. The safe alternative is to back up data, migrate once to a stable, securely stored signing key, and reuse that same key for all subsequent versions. Do not promise users that a new key can update previously installed apps.

### Separate issues
The GitHub Actions release API has previously returned HTTP 403. This is a repository token-authorization issue; it does **not** mean an APK is upgrade-compatible. Publishing a release must remain separate from successful signature verification. Developer debug builds should be labelled as not suitable for in-place updates.

### Verification checklist
- Exact application ID, increasing versionCode, same certificate SHA-256
- Signed release APK can install on a phone with the matching previous build while preserving app data
- Android Telecom calling (incoming/answer/decline/outgoing), popup hide/reopen and slide controls
- Android 6/8/15 UI checks and real-device tests before production rollout
