# Phase 7 Step 36 — Notification capture and multi-job session

**Status:** IN PROGRESS — source changes prepared; Gradle/device verification pending  
**Completion date:** 2026-10-24

## Objective

Allow a user to start a consented screen-capture session in JobMate, switch to a job listing app, trigger capture from Android's ongoing notification, and capture/save multiple jobs in that session.

## Implementation summary

- Added a `Capture screen` action alongside `Stop` in the ongoing MediaProjection notification. The action waits briefly for the notification shade to close before arming the next frame capture.
- Added a single-capture-in-progress guard and cleanup for pending capture actions.
- Updated the capture screen to explain the external-app workflow and show per-session captured-section and saved-job counts. It explains the Android 13+ notification permission need.
- Kept capture user-triggered, on-device OCR, and user-reviewed before saving. No overlay permission, AccessibilityService, auto-scroll, or screenshot upload was added.
- Added next-job draft reset after a saved job and an explicit “Prepare next job” action. Job-session counters persist across draft resets; a new draft does not carry the previous job URL.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/progress/PHASE-07-STEP-36.md`

## Dependencies and schema

No dependencies or database/schema changes.

## Verification

Attempted: `.\\gradlew.bat --no-daemon --stacktrace :app:assembleDebug`. Result: **BUILD FAILED before compilation** because Android SDK location is not configured (`SDK location not found`). This environment reports Java 19.0.1, while the approved build toolchain is JDK 17. No automated tests or Android device test were completed. A successful CI build is not implied.

## Known limitations / pending work

- The notification action depends on the user allowing JobMate notifications on Android 13+.
- Some apps may block screenshots; OCR results can be incomplete and must be reviewed.
- Device testing must verify action behavior with the notification shade, app switching, repeated captures, and saving multiple jobs.

## Exact next step

Run the configured GitHub Actions verification. Install its debug APK and manually verify capture in LinkedIn/Naukri and a multiple-job session before treating this workflow as verified.

