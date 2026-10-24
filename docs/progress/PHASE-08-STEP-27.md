# PHASE-08-STEP-27 — App settings, export, and data deletion

**Status:** IN PROGRESS (source implemented; verification deferred)  
**Completion date:** 2026-10-24

## Objective
Add theme preference, user-directed structured-data export, and local data deletion.

## Implementation summary
Added persistent dark theme selection, CSV export through Android's document picker for jobs, contacts, templates, and outreach records, and a confirmation dialog before deleting all local rows, resume files, WorkManager work, and encrypted Gmail App Password data. Credentials and PDF bytes are excluded from export. Gmail settings remain available as their own destination.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/data/storage/AppPreferences.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/storage/PrivacyDataManager.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AppSettingsViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AppSettingsScreen.kt`

## Files modified
- `app/src/main/java/com/saikalyan/jobmate/MainActivity.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Database changes
No schema changes; Room `clearAllTables()` is used after explicit confirmation.

## Verification
No tests or build run per owner instruction. Android SDK verification remains pending.

## Known issues / next step
Export/import round-trip is not implemented; export is a readable CSV backup. Continue with PHASE-09-STEP-28 only after the owner supplies the planned Sheets URL, tab layout, and identity choice.
