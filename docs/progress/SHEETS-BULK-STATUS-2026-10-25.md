# Sheets Sync and Bulk Job Status Follow-up

**Date:** 2026-10-25  
**Status:** Source changes implemented; Gradle/CI and device verification pending.

## Request

Correct the apparent one-way Google Sheets sync so edits and rows in the Sheet can reach the app, preserve deleted app jobs in the Sheet as deleted records, and allow changing the status of multiple jobs in one action.

## Findings

- On successful authorization, `SheetsSyncViewModel` previously enqueued every Room job and contact for upload before the worker read the spreadsheet. A local row could therefore be written over a Sheet row before the pull/merge stage.
- The worker already had a remote merge stage and timestamp-based conflict handling, but that stage ran after processing queued uploads.
- App job deletion already queues a `DELETE` operation. The worker updates the Sheet row's `deleted` column and timestamp, preserving the row's data rather than physically deleting the Sheet row. This tombstone behavior was retained.
- The Jobs list had status filters and single-job status editing in Job Details, but no multi-select or bulk status action.

## Changes

- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncViewModel.kt`: authorization now schedules sync without pre-enqueueing every local row.
- `app/src/main/java/com/saikalyan/jobmate/worker/GoogleSheetsSyncWorker.kt`: the worker merges Sheet rows into Room before processing uploads, respects pending/conflict queue items, handles timestamped remote deletions, and queues local-only/newer rows for upload. Remote rows with equal or newer timestamps can update matching Room records. If an older pending local write conflicts with remote values at an equal/newer timestamp, the worker records a conflict instead of overwriting the Sheet silently.
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/JobDao.kt`: added one SQL update for status changes across a list of IDs.
- `app/src/main/java/com/saikalyan/jobmate/domain/repository/JobRepository.kt`: added a default bulk status operation to retain compatibility with existing repository fakes/implementations.
- `app/src/main/java/com/saikalyan/jobmate/data/repository/JobRepositoryImpl.kt`: bulk status writes use one DAO update timestamp and enqueue each changed job for Sheet sync.
- `app/src/main/java/com/saikalyan/jobmate/ui/jobs/JobListViewModel.kt`: added selection state and a bulk status action.
- `app/src/main/java/com/saikalyan/jobmate/ui/jobs/JobListScreen.kt`: added **Select jobs**, per-row checkboxes, and a **Change status** menu.
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`: wired list actions to the ViewModel.
- `JOBMATE_IMPLEMENTATION_PLAN.md`: living status and next verification steps updated.

## Sync behavior after change

1. Fetch the `Jobs` and `Contacts` rows from the connected Sheet.
2. Pull valid remote rows and tombstones into Room unless the matching local item is pending or conflicted.
3. Queue local rows that are absent from the Sheet or have a newer `updatedAt` timestamp.
4. Process queued writes and preserve app deletions by setting the Sheet row's `deleted` value to `true`.

The current data model uses timestamps rather than a persisted last-synced row snapshot. A manual sheet edit with an equal timestamp is pulled when there is no pending local write; if it overlaps a pending local write, it becomes a conflict for the user to resolve. If a manual sheet edit leaves an older timestamp than a newer local edit, the local version is uploaded. Resolve conflict cases in Sheets sync before relying on automatic merge for simultaneous/offline edits.

## Verification status

- No Gradle build, unit test, CI workflow, OAuth request, Sheets API call, or device flow was run as part of this change.
- Do not treat bidirectional synchronization or bulk status updates as verified until a successful build and the device scenarios below pass.

## Recommended verification scenarios

1. Put a valid job row in the connected `Jobs` tab that is not in the app; run sync and confirm it appears in the app.
2. Add a job in the app; run sync and confirm its row appears in Sheets.
3. Change a job field in Sheets, including `updatedAt` if needed; run sync and confirm the app reflects it without deleting unrelated Sheet rows.
4. Delete a job in the app; run sync and confirm the Sheet row remains with `deleted=true` and its other cells retained.
5. Select at least two jobs, set them to a new status, and confirm both app rows and both Sheet rows update.

