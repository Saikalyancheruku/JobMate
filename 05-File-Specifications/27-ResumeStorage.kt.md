# File specification: `ResumeStorage.kt`

**Purpose:** Private PDF file management.

## Required implementation
SAF import, validate PDF, stream copy with size cap, SHA-256, generated private filename; never sync bytes.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
