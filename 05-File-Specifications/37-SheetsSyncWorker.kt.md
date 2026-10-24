# File specification: `SheetsSyncWorker.kt`

**Purpose:** Optional Sheets sync.

## Required implementation
WorkManager network constraint/backoff; idempotent UUID upsert; auth refresh; conflict handling; no secret/resume/screenshot data.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
