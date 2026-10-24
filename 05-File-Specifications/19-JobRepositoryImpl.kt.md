# File specification: `JobRepositoryImpl.kt`

**Purpose:** Job persistence implementation.

## Required implementation
Map entity/domain; local-first writes; enqueue eligible Sheets sync only when enabled; remote failure never rolls back local data.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
