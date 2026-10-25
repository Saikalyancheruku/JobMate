# Phase 7 — Step 49: Revert rating-row title/company anchor

**Date:** 2026-10-25  
**Status:** IN PROGRESS — parser source reverted; compilation and OCR/device verification pending.

## Objective

Remove the rule that uses a company rating/reviews row to select a Naukri job title and company, returning title/company selection to the prior header logic.

## Implementation summary

- Removed `chooseHeaderFromRating` and its candidate helpers from the title/company selection path.
- Restored the existing fallback order: inline `role @company` when present; otherwise the existing title candidate and layout-specific company-position rules (LinkedIn above title, Naukri/generic below title), with explicit labeled company values taking precedence.
- Kept rating and review-count extraction as separate fields; those values no longer influence title or company.
- Preserved other existing noise-filtering changes.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/JobFieldParser.kt`

## Verification actually performed

- Confirmed the rating-row helper is no longer referenced and ran `git diff --check`.
- No parser tests, Gradle build, CI, or device OCR comparison has completed.

## Known limitations / pending work

- The prior title/company logic remains heuristic and may select incorrect candidates when OCR misses or reorders header lines.
- The changed parser must be built and compared against Naukri and LinkedIn captures before claiming accuracy improvement.

## Exact next step

Verify the complete current source through CI, then run the existing OCR examples on device and check title/company output for both portals.
