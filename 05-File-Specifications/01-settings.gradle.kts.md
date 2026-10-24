# File specification: `settings.gradle.kts`

**Purpose:** Gradle repositories and :app module.

## Required implementation
Use Google, Maven Central and Gradle Plugin Portal. Use FAIL_ON_PROJECT_REPOS. Set root name JobMate and include :app. No web/backend modules.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
