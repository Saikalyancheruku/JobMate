# PHASE-07-STEP-25 — Capture review and confirmed save

**Status:** IN PROGRESS (source implemented; verification deferred)  
**Completion date:** 2026-10-24

## Objective
Provide editable capture review, duplicate detection, and user-confirmed job save.

## Implementation summary
Added a ViewModel to collect OCR results, prefill editable fields, accept shared text, compute a stable capture fingerprint, check duplicates, and persist only after the user presses Confirm and save. The screen communicates OCR limitations and provides fields for title, company, location, URL, skills, experience, salary, and description. No auto-save occurs.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`

## Files modified
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Dependencies / schema
No schema changes. Uses existing JobRepository.

## Verification
No tests/build run per owner instruction. SDK-dependent verification remains pending.

## Known issues / next step
No OCR fixture or device validation yet. Continue with PHASE-08-STEP-26, follow-up management and reminders.
