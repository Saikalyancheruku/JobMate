# PHASE-09-STEP-28 — Google Sheets authorization and API client

**Status:** IN PROGRESS (source implemented; runtime verification deferred)  
**Completion date:** 2026-10-24

## Objective
Implement user-authorized Google Sheets access and create a JobMate spreadsheet without introducing a backend.

## Implementation summary
Added Google Play services authorization using the `drive.file` scope. A bearer token is held only in the active worker call and is never written to Room, DataStore, logs, or WorkManager input. Added an HTTPS REST client for Google Sheets API v4 spreadsheet creation, values reads, idempotent row updates, and append. New files are named `JobMate Sync`; the app creates `Jobs` and `Contacts` tabs with a documented stable-ID schema. The scope is limited to files created by JobMate.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/data/remote/GoogleSheetsClient.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/secure/SheetsSyncPreferences.kt`
- `app/src/main/java/com/saikalyan/jobmate/worker/GoogleSheetsSyncWorker.kt` (authorization entry point and API use)
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncViewModel.kt`

## Files modified
- `gradle/libs.versions.toml` — add `play-services-auth` 21.6.0
- `app/build.gradle.kts` — add the pinned authorization dependency
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/storage/PrivacyDataManager.kt` — disconnect/revoke Sheets access during data deletion
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/SyncQueueDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/SyncConflictDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AppSettingsScreen.kt`

## Data and privacy decisions
Jobs and contacts only. Outreach, Gmail credentials, resume PDFs, screenshots, and tokens are excluded. The app uses the authorized Google APIs only for its created sheet. The sheet remains in Drive after disconnect; the user can delete it in Drive.

## Verification
No tests or build run, per owner instruction. Live account/API calls were not attempted.

## External setup / next step
The owner must enable Google Sheets API and configure an Android OAuth client for package `com.saikalyan.jobmate` with the signing certificate SHA-1 used by the installed build. Then continue with PHASE-09-STEP-29 queue and conflict processing.
