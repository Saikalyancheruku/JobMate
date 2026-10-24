# File specification: `JobEntity.kt`

**Purpose:** Room job entity.

## Required implementation
UUID PK; fields from ROOM_SCHEMA; indexes company/status/fingerprint; no secrets or file bytes.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
