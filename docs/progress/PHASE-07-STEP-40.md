# Phase 7 Step 40 — Capture multiple pages into one job

**Status:** IN PROGRESS — source updated; Gradle/device verification pending  
**Date:** 2026-10-25

## Objective

Support job listings that span multiple screens while retaining automatic saving and support for multiple jobs in one capture session.

## Implementation summary

- CAP now captures a page and saves it to the active job. Further CAP taps append OCR text to that same job and upsert it, preserving the existing job ID and fingerprint.
- Parsed fields are filled when missing; existing non-fallback title/company values are kept to avoid replacing them with unrelated headings from later pages.
- Added a NEXT control beside CAP. It clears the active job association so the next CAP starts a different job. The user does not need to open JobMate or review/confirm the job.
- Session saved-job count increments only when a new job is created, not for each added page.
- Updated in-app instructions and ongoing notification text to explain CAP versus NEXT.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureSessionController.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/progress/PHASE-07-STEP-40.md`

## Dependencies and schema

No dependency or database/schema changes. Page updates use the existing `JobRepository.save` upsert behavior.

## Verification

No Gradle build, automated test, or device test was run. The local environment has no configured Android SDK and the prior assemble attempt stopped before compilation. Steps 37–39 also remain unbuilt.

## User workflow and limitation

1. Open one job listing and tap CAP on each page that contains more details.
2. When opening a different job, tap NEXT once, then use CAP on its pages.

NEXT is the explicit boundary because OCR cannot reliably infer when a listing changes, especially when a later page has no title or company text. If NEXT is omitted, the next page is appended to the currently active job.

## Exact next step

Run GitHub Actions, install the resulting APK, and verify that multiple CAP captures update one job while NEXT causes the following capture to create another job.
