# PHASE-04-STEP-17

- **Phase and step number:** Phase 4, Step 17
- **Step title:** Add resume list, preview, rename and delete UI
- **Status:** BLOCKED — source written; tests deferred by owner and Android compilation requires SDK setup
- **Completion date:** 2026-10-24

## Objective

Expose local resume management through a Compose screen and allow preview using a narrowly scoped content URI.

## Implementation summary

Added a Hilt ViewModel and Compose resume list with PDF SAF import, metadata display, rename and confirmed delete. Preview resolves the stored file and shares only a `FileProvider` URI with temporary read permission. The provider exposes only the private `resumes/` directory. Import validation and metadata persistence remain in the repository/storage layers.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/ui/resumes/ResumeListViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/resumes/ResumeListScreen.kt`
- `app/src/main/res/xml/file_paths.xml`

## Files modified

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/saikalyan/jobmate/di/AppModule.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/ResumeRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`

## Verification

Tests are deferred per owner instruction. Gradle compile/lint/APK verification remains blocked by missing Android SDK. No PDF was imported or previewed on a device in this environment.

## Known issues

External preview requires a PDF viewer app. Release/device verification must confirm SAF import permissions, provider URI grants and deletion/reference behavior.

## Exact next step

PHASE-05-STEP-18 — implement seeded/editable outreach templates and placeholder rendering.
