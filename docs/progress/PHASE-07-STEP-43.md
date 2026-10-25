# Phase 7 — Step 43: Sample-based capture parsing adjustments

**Date:** 2026-10-25  
**Status:** Source adjustments implemented from the supplied LinkedIn and Naukri screenshots; build and device verification pending.

## Findings from the saved-job screenshots

- LinkedIn title was captured as `Senior Software Engineer`, but the company appears to have been assigned from the status line (`Actively reviewing applicants`) instead of the company line above the title. The source header shows company above title, then location and posting metadata below.
- Naukri title was captured as `.NET Developer`; the saved company text is `MBS GnAL` while the source screenshot shows `MBS GLOBAL`. This is an OCR recognition error, not only a parser field-order error.
- The saved descriptions contain capture controls and app chrome, including `NEXT`, JobMate navigation labels, LinkedIn actions, and repeated captured text.
- The Naukri source has explicit `Must have skills` and `Good to have skills` sections. The parser did not previously target those headings.

## Changes

- Parse the LinkedIn company from the line immediately before its title; keep the Naukri company-after-title layout.
- Extract a location from a LinkedIn metadata line even when posting age/applicant counts share that line, then remove those metadata suffixes.
- Recognize Naukri must-have/good-to-have skill headings and omit common app chrome/control labels from the saved description.
- Hide the whole capture overlay while waiting for and acquiring a frame. Hiding only CAP left NEXT visible to OCR.
- Preserve existing multi-page CAP and NEXT behavior.

## Verification and limitations

The screenshot comparison is a manual source-level review. No Gradle build, tests, CI run, or device capture was performed. The known `MBS GnAL` OCR error cannot be reliably corrected from parser layout alone; validate ML Kit output on-device and use confidence/evidence or user correction for ambiguous company text. JobMate dashboard text in a saved description suggests a frame may have been captured while JobMate itself was foreground; do not tap CAP while reviewing saved jobs.
