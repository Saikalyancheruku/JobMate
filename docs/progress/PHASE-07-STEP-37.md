# Phase 7 Step 37 — Automatically save captured jobs

**Status:** IN PROGRESS — source updated; Gradle/device verification pending  
**Date:** 2026-10-24

## Objective

Remove the per-job review and confirmation step from the screen-capture flow. Each user-triggered screen capture should create a saved local job automatically.

## Implementation summary

- Each successful OCR capture now immediately checks for an existing fingerprint and saves a new `SAVED` job through `JobRepository` without requiring a review or confirmation tap.
- The full OCR text is retained in the job description so details can be corrected later from the Jobs feature.
- When OCR cannot identify a title or company, the saved job uses explicit fallback values (`Captured job <timestamp>` and `Company not detected`) rather than dropping the capture.
- Existing duplicate fingerprints are skipped and shown as already saved. Captures without an OCR URL use a normalized OCR-text hash for repeat detection.
- The capture screen now describes one notification capture per listing, shows automatic-save progress/result, and no longer presents editable fields or a confirm/save button.
- Session counters report successfully saved jobs. The next capture starts with a fresh draft state.
- The separate Sharesheet text-import path retains its editable review and confirmation flow.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/progress/PHASE-07-STEP-37.md`

## Dependencies and schema

No dependency or schema changes.

## Verification

No Gradle build or automated/device test has been run for Step 37. The prior local build attempt for Step 36 failed before compilation because the Android SDK location is not configured, and this environment reports Java 19 rather than the required JDK 17. That result does not validate Step 37.

## Known behavior and limitations

- Each tap captures and saves the currently visible screen as one job. To capture another listing, scroll to it and tap again.
- OCR can miss or misidentify fields. The raw recognized text is retained; edit saved jobs later as needed.
- A detected duplicate is not inserted again.
- The notification action requires notification permission on Android 13+.

## Exact next step

Run the configured GitHub Actions workflow and record actual test, lint, and APK assembly results. Then install that APK and verify automatic save, duplicate handling, and repeated captures on a real device.
