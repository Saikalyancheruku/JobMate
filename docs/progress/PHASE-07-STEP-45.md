# Phase 7 — Step 45: Correct captured job field bindings

**Date:** 2026-10-25  
**Status:** Source implementation completed; build, automated test, CI, and device verification pending.

## Objective

Trace the actual screen-capture OCR data flow and correct field bindings for Naukri/LinkedIn without hardcoding the supplied Altimetrik example.

## Implementation summary

- Kept portal identity (`Naukri` / `LinkedIn`) separate from capture method (`USER_CONSENTED_SCREEN_OCR`). The capture origin is inferred from layout cues; unknown layouts retain the generic screen-capture source.
- Separated location from work arrangement. A phrase such as `Bangalore (Weekly thrice in office)` is normalized into location `Bangalore` and arrangement `Weekly thrice in office`.
- Preserved the existing user status and application date independently of OCR. Screen capture continues to create new jobs as `SAVED`; OCR does not change it to `APPLIED` or fabricate an applied date. Editing no longer invents today's applied date from status.
- Added structured capture fields for company rating/reviews, applicant count, original posted wording, vacancy count, experience level, education, mandatory/preferred skills, raw extracted OCR, and capture timestamp. Education is extracted from an Education section; the parser avoids taking arbitrary promotional currency values as salary.
- Restricted description extraction to a recognized `Job description` / `About the job` section and stop at subsequent Naukri metadata sections. Common status-bar time, notification text, promotional text, app chrome, and repeated exact lines are removed from the cleaned description; original OCR is retained separately on-device.
- Preserved experience minimum/maximum separately, metadata through the editor/detail flow, and structured metadata through the Google Sheets job row. Raw OCR is deliberately excluded from Sheets and kept local.
- Added Room schema migration 1→2 for the new structured fields.

## Before and after for this listing

| Field | Before | New binding/result for the supplied sample |
|---|---|---|
| Title | `Bright Minds` (promotional candidate could win header ranking) | Primary listing heading; expected `.Net core + AWS Developer` |
| Company | `Similarroles` (navigation text could win company selection) | Employer under Naukri title; expected `Altimetrik` |
| Location | `Bangalore (Weekly thrice in office)` | Location-only `Bangalore` |
| Work arrangement | Not extracted | `Weekly thrice in office` |
| Source | `Screen capture` | Layout-derived portal `Naukri` |
| Capture method | Stored as capture mechanism but not exposed in the initial capture form | `USER_CONSENTED_SCREEN_OCR`, persisted separately |
| Status | New capture defaults to `Saved` | Preserved user-tracked status; OCR does not infer `Applied`. User must select Applied if that is their tracked status. |
| Experience | `3`/`6` from range, already parsed | Min `3`, max `6` (same expected values) |
| Applied date | Blank, but editor could auto-fill today when status was Applied | Remains blank unless user enters the actual application date |
| Follow-up date | Blank/manual | Still manual; no listing source exists |
| Salary | Often blank; broad currency detection risked unrelated price text | `Not disclosed` when recognized; unrelated currency snippets such as promotional pricing are not taken as salary |
| Employment type | Empty | `Full Time, Permanent` when present in listing metadata |
| Job URL | URL regex over captured text; no actual listing URL in sample | Actual URL only when a URL is present in extracted text; otherwise manual/unavailable |
| Company rating | No field | `3.4` when adjacent to company reviews in OCR |
| Review count | No field | `1.6K Reviews` |
| Applicant count | No field | `3985` |
| Posted date | No field populated | Original wording `Posted 17d ago`; no guessed absolute date |
| Vacancies | No field | `1` |
| Experience level | No field | `Associate level` |
| Education | No field | Value following the Education section: `B.Tech / B.E. in Any Specialization` |
| Mandatory skills | Previously merged in common skills | `C#, aws, .NET Core` from Must have skills |
| Preferred skills | Previously merged in common skills | `Azure, Web API, ASP.NET, Microservices, SQL` from Good to have skills |
| Legacy combined `skills` | Parser's general skills group; capture save merges recognized skills | Retained as a combined compatibility/search field; separate mandatory/preferred lists are now authoritative for those categories |
| Job description | Entire-screen/raw OCR was being saved on auto-capture | Cleaned text within the recognized description section, stopping at later metadata sections |
| Raw captured text | Transient/not separately saved | Original extracted OCR stored separately on device; not sent to Sheets |
| Capture timestamp | Save/edit timestamps only | Actual frame-capture timestamp stored separately |
| Absolute `postedAt` | No parsed value | Remains unset; the parser stores original relative text in `postedText` instead of inventing an absolute date |
| Notes | User-entered, not OCR-derived | Preserved as user-authored notes; empty for this sample unless entered |
| ID / fingerprint | Generated identity/deduplication values, not OCR fields | Unchanged; used for persistence and deduplication, not bound to listing text |
| `createdAt` / `updatedAt` | App save/edit timestamps | Remain app-maintained record timestamps; `capturedAt` now separately records frame time |

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/JobFieldParser.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/model/Job.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/entity/JobEntity.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/JobMateDatabase.kt`
- `app/src/main/java/com/saikalyan/jobmate/di/AppModule.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/JobRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureSessionController.kt`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/jobs/JobEditorViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/jobs/JobEditorScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/jobs/JobDetailScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/remote/GoogleSheetsClient.kt`
- `app/src/main/java/com/saikalyan/jobmate/worker/GoogleSheetsSyncWorker.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/SheetsSyncViewModel.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`

## Dependencies and schema

- No dependency versions changed.
- Room schema version increased from 1 to 2; migration 1→2 adds the metadata, skill-group, raw OCR, and capture-time columns.
- Google Sheets Jobs header/data range extends through column AG for structured metadata. Raw OCR remains local-only.

## Verification actually performed

- Static source/data-flow inspection of parser → automatic capture save → `Job` → `JobEntity` → repository → editor/detail and Sheets mappings.
- `git diff --check` ran and reported no whitespace errors; it emitted only Windows line-ending notices.
- No Gradle build, unit tests, CI run, or device capture was run in this step. No functional accuracy is claimed.

## Known issues and pending work

- Naukri/LinkedIn structure and OCR remain heuristic. The sample expected outputs are parser intent, not observed runtime output.
- If OCR misses a header/section label, these bindings can remain empty or wrong. Company name recognition is limited by source OCR quality.
- Without a recognized description heading, the fallback is less section-specific; validate on actual captures and add fixtures/tests when requested.
- The sample's `Applied` status is user-tracked and is not inferred or auto-applied by OCR. Follow-up date and unavailable job URL require user input.
- Full compile/test/lint/APK and device verification remain pending due to the previously recorded missing local Android SDK/JDK mismatch and failed latest CI test run.

## Exact next step

Run GitHub Actions (unit tests, lint, debug APK build), resolve any actual failures, then install the successful artifact and validate the sample on Naukri and LinkedIn. Review all parsed fields and description boundaries before making any accuracy claim.
