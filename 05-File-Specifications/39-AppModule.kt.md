# File specification: `AppModule.kt`

**Purpose:** Dependency injection providers.

## Required implementation
Singleton Room, DAOs, repos, clients, dispatchers; TLS/timeouts; test overrides; no credentials.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
