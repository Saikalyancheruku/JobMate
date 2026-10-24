# Phase 7 Step 39 — Projection frame format and visible errors

**Status:** IN PROGRESS — source updated; build/device verification pending  
**Date:** 2026-10-24

## Objective

Investigate the user's report that tapping the floating capture bubble results in an error marker and no saved job. Correct the likely MediaProjection/ImageReader format mismatch and make the failure reason visible.

## Implementation summary

- Changed the virtual-display `ImageReader` output from `YUV_420_888` to `RGBA_8888`, a suitable pixel format for screen mirroring.
- Added RGBA plane-to-bitmap conversion for ML Kit. The OCR recognizer still supports YUV images if provided by another source.
- Added the specific exception class to OCR error state and display capture error messages in a Toast when the bubble shows `!`.
- Added an OCR-start label after the frame is actually acquired; the bubble now distinguishes ready (`CAP`), OCR, saving (`SAVE`), success (`✓`), and failure (`!`).
- Kept the capture timeout and frame retry feedback. No credentials, network flow, AccessibilityService, or automatic interaction with other apps was introduced.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/remote/OcrTextRecognizer.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/progress/PHASE-07-STEP-39.md`

## Dependencies and schema

No dependency or database/schema changes.

## Verification

No Gradle build or device test was run. The environment lacks a configured Android SDK; the earlier local Gradle attempt failed before compilation with `SDK location not found`. The source-format change is a hypothesis based on the capture/OCR path and must be confirmed by CI plus a device run.

## Exact next step

Build a new debug APK in GitHub Actions, install it, grant overlay and screen-capture permissions, and tap the floating bubble. Confirm the bubble advances through OCR and SAVE to ✓. If it shows `!`, record the Toast message and device Android version so the remaining failure can be diagnosed.
