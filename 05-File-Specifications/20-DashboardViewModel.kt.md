# File specification: `DashboardViewModel.kt`

**Purpose:** Dashboard state and aggregation.

## Required implementation
Immutable StateFlow; Room-derived counts/status; loading/empty/error; no hardcoded production metrics.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
