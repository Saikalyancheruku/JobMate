# File specification: `gradle/libs.versions.toml`

**Purpose:** Central version catalog.

## Required implementation
Pin compatible stable AGP/Gradle/Kotlin/Compose/Room/KSP/WorkManager/ML Kit/test libraries. Verify compatibility and licenses; remove unused dependencies.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
