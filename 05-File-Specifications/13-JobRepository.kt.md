# File specification: `JobRepository.kt`

**Purpose:** Job repository contract.

## Required implementation
Flow observe list/detail/dashboard; suspend save/status/archive/delete/search/fingerprint methods; typed errors; no DAO leakage.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
