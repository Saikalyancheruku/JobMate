# PHASE-09-STEP-29 — Durable queue, WorkManager, and conflicts

**Status:** IN PROGRESS (source implemented; tests/device verification deferred)  
**Completion date:** 2026-10-24

## Objective
Keep Room as the local source of truth, sync structured rows when connected, retry transient failures, and expose concurrent edits for review.

## Implementation summary
Job and contact writes/deletes enqueue stable per-record sync entries only while Sheets sync is enabled. A network-constrained unique WorkManager task processes pending records with exponential backoff; a periodic task checks for remote edits. UUID rows are updated in place. Deletes become remote tombstones. Rows from Sheets can update local data when no local change is pending. Concurrent edits create local/remote snapshots in Room and show Keep JobMate / Use Sheet version choices. API bearer tokens and snapshots contain only the whitelisted job/contact columns.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/data/repository/SyncQueueManager.kt`

## Files modified
- `app/src/main/java/com/saikalyan/jobmate/worker/GoogleSheetsSyncWorker.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/SyncQueueDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/SyncConflictDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/JobDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/ContactDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/JobRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/ContactRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncScreen.kt`

## Database changes
No schema changes; the existing sync queue and conflict tables are now used.

## Verification
No tests/build/device sync were run per owner instruction. Compile and remote behavior remain unverified.

## Known limitation / next step
Manual Sheet edits must update the row's `updatedAt` field using ISO-8601 for conflict detection. Continue with PHASE-09-STEP-30 schema/configuration documentation and enablement flow.
