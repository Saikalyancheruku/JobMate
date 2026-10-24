# Test and release plan

**Unit:** job validation/status; contact normalization/duplicates; matcher false positives; CSV/XLSX mapping/errors; placeholder renderer; OCR parser fixtures; sync conflict/retry; SMTP one-recipient MIME and redacted errors.

**Room:** CRUD/search/filter/counts/fingerprint; transactions; foreign keys; migrations.

**Integration:** Gmail SMTP App Password with dedicated test account (valid/invalid password, ineligible-account messaging, STARTTLS, PDF attachment, disconnect/wipe, restart recovery); no OAuth client ID; Sheets test spreadsheet (Phase 9); MediaProjection denial/stop/secure-window/process lifecycle.

**UI:** dashboard states; job CRUD; import preview; resume lifecycle; per-recipient preview/confirmation; capture review; settings disconnect/delete.

**Device matrix:** API 26 and current Android, plus user's device/OS when provided; Naukri/LinkedIn app versions; dark mode, font scale, rotation, low storage, offline, revoked permissions.

**Privacy gate:** no normal Gmail password; no secrets in logs/Room/backup/Sheets/GitHub; no resume upload; visible user-stoppable capture; no stealth capture; TLS; no automatic email send/retry after uncertain outcome; dependency/license review.
