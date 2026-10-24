# File specification: `JobDao.kt`

**Purpose:** Job SQL access.

## Required implementation
Parameterized Flow list/detail/search/status/count queries; CRUD; fingerprint lookup; transactions when multi-row.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
