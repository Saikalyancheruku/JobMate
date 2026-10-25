# Phase 7 — Step 53: Explicit capture job boundaries

**Date:** 2026-10-25  
**Status:** IN PROGRESS — source changes applied; build, CI, and device verification pending.

## Problem

The existing overlay offered `CAP` and `NEXT`. `NEXT` emitted a new-job request that cleared the active capture job in `CaptureViewModel`. The next capture therefore created a separate record, even when the user intended to continue capturing another page of the same listing.

## New interaction

- Camera bubble: capture the visible page. Repeated camera captures merge into the same active saved job.
- Checkmark bubble: finish the current listing by clearing the active-job pointer. It does not stop MediaProjection or delete the saved job.
- The next camera capture after finishing begins a new job.
- OCR/save progress and error indicators remain on the camera control. The overlay is still hidden during screenshot capture.
- Capture screen instructions, consent text, notification wording, accessibility descriptions, and parser overlay-noise filtering were aligned with the new controls.

## Files changed

- `app/src/main/java/com/saikalyan/jobmate/service/CaptureSessionController.kt`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/JobFieldParser.kt`
- Living implementation status documents.

## Verification status

- Source flow traced: `NEXT` previously emitted a request that reset `activeCaptureJob`; the next OCR section was then treated as a new job.
- No Gradle compile, tests, CI run, or device interaction test has been performed for this change.
- `git diff --check` passed with Git line-ending notices; it does not verify Android compilation or behavior.

## Exact next action

Run CI after the outstanding Room assertion change and this overlay change. On device, capture at least two pages of one listing, tap checkmark, then capture a second listing. Confirm the first two pages update one job and the later capture creates a new job.
