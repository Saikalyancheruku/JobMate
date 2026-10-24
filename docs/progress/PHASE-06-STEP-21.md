# PHASE-06-STEP-21

- **Phase and step number:** Phase 6, Step 21
- **Step title:** Add Gmail SMTP STARTTLS connection and one-recipient client
- **Status:** BLOCKED — source written; tests/account/device verification deferred; Android compilation needs SDK
- **Completion date:** 2026-10-24

## Implementation summary

Added `GmailSmtpClient` using the pinned Android Mail/Activation libraries. It uses `smtp.gmail.com:587`, required STARTTLS, server identity checks, certificate validation through platform trust, and connection/read/write timeouts. Connection testing authenticates and closes without sending. Send API accepts exactly one recipient, creates a separate MIME message with one To address and optional private PDF, and uses no CC/BCC. SMTP debug is disabled. No OAuth, XOAUTH2, Gmail API, or unattended worker was added.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/data/remote/GmailSmtpClient.kt`

## Files modified

- `app/build.gradle.kts` — pinned mail/activation dependencies and META-INF exclusions.

## Verification

No SMTP tests or real-account sending performed, per owner instruction. Gradle configuration task succeeded. Compilation and Android SDK/device verification remain pending.

## Exact next step

PHASE-06-STEP-22 — persist per-recipient send attempts/outcomes, show explicit send confirmation, and require review before retrying UNKNOWN.
