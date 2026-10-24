# File specification: `JobStatus.kt`

**Purpose:** Job lifecycle enum.

## Required implementation
SAVED, APPLIED, INTERVIEW, OFFER, REJECTED, WITHDRAWN, CLOSED; stable persisted string mapping and safe unknown-value handling.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
