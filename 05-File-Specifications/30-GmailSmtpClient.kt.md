# File specification: `GmailSmtpClient.kt`

**Purpose:** Gmail SMTP transport.

## Required implementation
STARTTLS to smtp.gmail.com:587; certificate validation; Gmail App Password authentication only (no OAuth2/XOAUTH2/Gmail API); one To recipient; PDF MIME; typed SENT/FAILED/UNKNOWN; redact logs.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
