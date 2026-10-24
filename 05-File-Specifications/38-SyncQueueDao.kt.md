# File specification: `SyncQueueDao.kt`

**Purpose:** Durable sync queue DAO.

## Required implementation
Transactional claim; mark synced/retry/conflict; observe pending; prevent concurrent duplicate processing.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
