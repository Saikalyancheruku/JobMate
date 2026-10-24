# File specification: `TemplateRenderer.kt`

**Purpose:** Safe template rendering.

## Required implementation
Only documented placeholders; unknown/missing values block send; no eval; deterministic output.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
