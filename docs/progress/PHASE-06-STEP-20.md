# PHASE-06-STEP-20

- **Phase and step number:** Phase 6, Step 20
- **Step title:** Add Gmail SMTP settings and Keystore-backed App Password storage
- **Status:** BLOCKED — source written; tests/device validation deferred; Android compilation needs SDK
- **Completion date:** 2026-10-24

## Implementation summary

Added an AES-256-GCM credential store. An Android Keystore key encrypts the Gmail App Password; Preferences DataStore stores only ciphertext and the email address. Disconnect clears both stored values and deletes the key alias. The password is masked, is not read back into the screen, and temporary cleartext bytes are zeroed. Added Gmail settings UI for App Password save, SMTP connection test, status and disconnect, with account-eligibility guidance. App backup is already disabled. Added the pinned Preferences DataStore and Android Mail dependencies.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/data/secure/SecureCredentialStore.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/secure/EncryptedAppPasswordStore.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/GmailSettingsViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/GmailSettingsScreen.kt`

## Files modified

- `app/build.gradle.kts`
- `app/src/main/java/com/saikalyan/jobmate/di/AppModule.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Verification

No tests run per owner instruction. Gradle task configuration succeeded. Keystore behavior is not device-verified; source compile remains SDK-blocked.

## Exact next step

PHASE-06-STEP-21 — implement Gmail SMTP STARTTLS connection testing and one-recipient MIME send.
