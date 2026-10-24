# File specification: `JobMateApplication.kt`

**Purpose:** Application startup and DI.

## Required implementation
Initialize selected DI; no network or permission prompts at startup; lightweight redacted debug logging only.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
