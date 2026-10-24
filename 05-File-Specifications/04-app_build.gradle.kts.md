# File specification: `app/build.gradle.kts`

**Purpose:** Android app configuration.

## Required implementation
applicationId/namespace com.saikalyan.jobmate; minSdk 26; select supported stable compile/target SDK; Compose enabled; configure tests and KSP; no secrets in build config.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
