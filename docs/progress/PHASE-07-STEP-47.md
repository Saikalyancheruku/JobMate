# Phase 7 — Step 47: Make header parsing template-aware

**Date:** 2026-10-25  
**Status:** Parser source adjusted; tests, build, CI, and device verification pending.

## Objective

Investigate the reported OCR accuracy regression and account for multiple Naukri header/content layouts without applying a single positional rule to other portals.

## Findings

- JobMate's supplied Naukri raw capture has a detail-header sequence of promotional banner text, job title, company, and rating/reviews. The rating/reviews row is a useful anchor for this captured template, but it is not a universal cross-portal rule.
- The previous Step 46 implementation applied the rating anchor to all layouts. That could override LinkedIn's different company/title order if a review row appeared there.
- Naukri's public recruiter guide documents semantic groups: Job Details (title, location, experience, salary), preferred-candidate details (skills, education, certification), and Job Description (responsibilities, preferred profile, company information). It also describes Standard, Hot Vacancy, and Classified posting types. These are field/content categories, not an official fixed mobile pixel-layout contract. See [Naukri Recruiter Job Posting](https://www.naukri.com/blog/recruiter-job-posting/).

## Change

- Applied the rating/reviews header anchor only after the page is classified as Naukri. LinkedIn and generic layouts retain their own existing header rules.
- Expanded banner-noise filtering for common promotional phrases from the supplied raw sample and treated the standalone `EMPLOYER` label as UI chrome.
- Retained the inline `role @company` pattern and the previous layout-based fallback where no Naukri rating anchor is available.
- No hardcoded sample company, title, or listing values were added.

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/JobFieldParser.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`
- `docs/progress/PHASE-07-STEP-47.md`

## Verification actually performed

- Compared the parser strategy with supplied raw capture sequences and Naukri's public description of its semantic posting sections and post types.
- No unit tests, Gradle build, CI run, or device capture were performed. No accuracy or regression fix is claimed as verified.

## Remaining limitations

- Naukri does not publish a stable pixel-level mobile listing template contract in the public guide. Job cards, detail pages, promotional headers, and incomplete/scroll captures can differ.
- OCR can omit or merge labels and lines; where header evidence is absent, JobMate still has a heuristic fallback.
- The latest CI log had one unrelated stale Room-version test assertion, corrected in Phase 10 Step 42; that correction also needs a CI rerun.

## Exact next step

Run the updated CI, install the resulting APK, and compare captures from at least three Naukri cases: a detail page with rating/reviews, a listing without rating/reviews, and a banner/inline-employer variant. Keep representative raw OCR fixtures for these templates when test coverage is resumed.
