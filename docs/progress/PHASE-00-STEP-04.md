# PHASE-00-STEP-04

- **Phase and step number:** Phase 0, Step 04
- **Step title:** Configure Android Manifest
- **Status:** COMPLETE
- **Completion date:** 2026-10-24

## Objective

Declare the network and runtime permissions, launcher activity, and future MediaProjection capture service. Keep this step limited to manifest configuration; activity/service implementations belong to later steps.

## Implementation summary

Updated the app manifest with `INTERNET`, `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, and `FOREGROUND_SERVICE_MEDIA_PROJECTION`. Declared `.MainActivity` as the exported `MAIN`/`LAUNCHER` entry point and `.service.CaptureForegroundService` as a non-exported service with `mediaProjection` foreground-service type. The app continues to set `android:allowBackup="false"`, preventing local credential data from being included in app backups. No broad storage permission or AccessibilityService was added.

The activity and service classes are intentionally declared ahead of implementation in their planned steps. No OAuth, Gmail API, backend, SMTP, or business behavior was added.

## Files created or modified

- `app/src/main/AndroidManifest.xml` — permissions and component declarations.
- `JOBMATE_IMPLEMENTATION_PLAN.md` — living progress and next-step pointer.
- `docs/progress/PHASE-00-STEP-04.md` — this report.

## Verification actually executed

- XML parsed with PowerShell's XML parser — **SUCCESS**.
- `gradlew.bat --offline --no-daemon :app:tasks` — **BUILD SUCCESSFUL** after the manifest update. This validates Gradle module configuration; it does not compile/package the app.
- APK assembly was not retried: the previous `:app:assembleDebug` attempt failed before compilation because Android SDK location is not configured; see PHASE-00-STEP-03.

## Exact next implementation step

**PHASE-00-STEP-05 — Set up Hilt Application and MainActivity**

Implement only the `@HiltAndroidApp` application class and Compose `setContent` entry activity. Do not start Step 06.