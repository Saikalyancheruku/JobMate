# Google Sheets Reconnect Destination Choice — 2026-10-25

**Status:** Source changes implemented; Gradle/CI and live Google/device verification pending.

## Problem

The selected spreadsheet ID is stored in app-local DataStore. Uninstalling JobMate removes that state. On a new install, the prior UI offered only “Connect Google and create spreadsheet,” and the worker created a new spreadsheet whenever no ID was saved. The old spreadsheet remained in Drive, but JobMate had no way to reconnect to it.

## Changes

- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncScreen.kt`: when disconnected, show explicit **Connect Google and create a new sheet** and **Connect existing sheet** choices. The existing-sheet path accepts a Google Sheets URL or spreadsheet ID. A connected user can open **Change spreadsheet** to select another destination.
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncViewModel.kt`: persist an explicitly selected existing sheet ID before scheduling sync; clear the old reference when the user explicitly chooses to create a new sheet; preserve the selected ID during ordinary reauthorization.
- `app/src/main/java/com/saikalyan/jobmate/data/secure/SheetsSyncPreferences.kt`: added a method to clear only the sheet reference for explicit new-sheet selection.
- `app/src/main/java/com/saikalyan/jobmate/data/remote/GoogleSheetsClient.kt`: added spreadsheet metadata lookup to check for required `Jobs` and `Contacts` tabs.
- `app/src/main/java/com/saikalyan/jobmate/worker/GoogleSheetsSyncWorker.kt`: validate those tabs before reading or writing rows. A missing tab records an actionable error and stops sync; it does not silently create another spreadsheet.
- `JOBMATE_IMPLEMENTATION_PLAN.md`: living implementation status updated.

## Access and data behavior

The existing-sheet path uses the already approved `drive.file` OAuth scope. It is intended for a JobMate-created sheet that remains accessible to the same Google account and OAuth app. Arbitrary Google Sheets not created/opened/shared with JobMate may be inaccessible under that narrow scope. The old sheet is not deleted or modified just by selecting it. On successful sync, the existing pull-before-push logic reconciles its rows with local data.

## Verification status

- No Gradle build, Actions run, OAuth flow, Sheets API request, or device test was run for this change.
- The local workspace currently has no configured Android SDK, so Gradle compilation cannot proceed here.
- Rebuild and install an APK, then select the old sheet URL and verify existing rows appear in Jobs/Contacts without a second sheet being created.

