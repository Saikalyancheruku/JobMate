# Phase 7 — Step 46: Anchor title and company to the rating row

**Date:** 2026-10-25  
**Status:** Parser source updated; build, tests, CI, and device verification pending.

## Objective

Use the Naukri raw OCR ordering visible in the supplied capture to distinguish the job title from the company name.

## Finding and rule

The raw capture shows the sequence:

```text
Bright Minds
Bold Ideas.
Join us and be part of the innovation!
Net core + AWS Developer
Altimetrik
*3.4 (1.6K Reviews)
```

The parser now treats the rating/reviews row as a header anchor when present. It selects the nearest valid company line immediately above the rating, then the nearest valid title line above that company. Known promotional text, UI labels, metadata, and rating/applicant lines are excluded from those candidate positions. This resolves the sample mapping as title `Net core + AWS Developer` and company `Altimetrik`, subject to OCR accurately providing the lines.

When the rating row is absent, the existing layout-aware selection remains as a fallback. A header line shaped like `Dot Net Developer @Qlnfosys` is also split into title `Dot Net Developer` and company `Qlnfosys` when found in the header.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/JobFieldParser.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`
- `docs/progress/PHASE-07-STEP-46.md`

## Verification actually performed

- Compared the rule against the raw OCR sequence supplied in the user message.
- `git diff --check` reported no whitespace errors; it emitted a Windows line-ending notice.
- No unit test, Gradle task, CI run, or device capture was run. The expected sample title/company values are not runtime-verified.

## Known limits

- If the rating/reviews line is not visible or is missed by OCR, the fallback heuristic is used.
- If OCR merges the role and company in a different form, the inline `role @company` split may not apply.
- Naukri layouts can vary. Verify on a freshly built APK before making an accuracy claim.

## Exact next step

Rerun Android CI after the pending Step 42 test assertion correction, then capture this listing on-device and check the saved title and company fields.
