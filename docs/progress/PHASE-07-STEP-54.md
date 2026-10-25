# Phase 7 — Step 54: Make finish-job boundary stateful

**Date:** 2026-10-25  
**Status:** IN PROGRESS — source fix applied; CI and device verification pending.

## Reported behavior

After capturing a page and tapping the checkmark, the next camera capture still updated the previously saved job. The intended behavior is that the checkmark closes the active job boundary; the next capture must start another job record.

## Cause addressed

The finish action had been represented as a one-shot `MutableSharedFlow` event. Such an event can be lost when no collector is subscribed. The ViewModel could then retain the previous `activeCaptureJob` and merge the next capture into it.

## Change made

- Replaced the finish event stream with a monotonic `StateFlow` job-generation counter in `CaptureSessionController`.
- The ViewModel observes generation changes and clears `activeCaptureJob`.
- Before processing every new captured section, the ViewModel synchronizes against the latest generation. This prevents a missed collector notification from merging a page into the prior job.
- Existing behavior remains: camera captures are saved/merged as they arrive; checkmark finishes the active job; the next camera capture starts a new one.

## Files changed

- `app/src/main/java/com/saikalyan/jobmate/service/CaptureSessionController.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- Living implementation status documents.

## Verification status

- Source flow was inspected and the stateful boundary fix applied.
- No Gradle compile, unit test, CI rerun, or Android device verification has been performed.
- The latest supplied CI test failure (schema assertion version 2 vs current version 3) is separately fixed in Step 52 and also awaits CI rerun.

## Exact next action

Rerun CI, install the resulting APK, capture a listing, tap checkmark, and capture a different listing. Confirm the second capture creates a distinct job rather than updating the first.
