# File specification: `build.gradle.kts`

**Purpose:** Root plugin declarations.

## Required implementation
Declare plugin aliases centrally through version catalog; avoid hardcoded dependency versions and duplicate plugin declarations.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
