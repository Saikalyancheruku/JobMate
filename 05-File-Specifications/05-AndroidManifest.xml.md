# File specification: `AndroidManifest.xml`

**Purpose:** Permissions, activity and service declarations.

## Required implementation
Internet; required notification/foreground-service declarations per target API; non-exported capture service; exported launcher activity; SAF instead of broad storage; no AccessibilityService.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
