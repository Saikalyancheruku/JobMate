# File specification: `ContactEntity.kt`

**Purpose:** Room contact entity.

## Required implementation
UUID PK; normalized email; index email/domain; unique email policy; timestamps.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
