# Phase 10 — Step 58: Make certificate export explicit

**Date:** 2026-10-25  
**Status:** IN PROGRESS — workflow updated; next Actions run pending.

## Observation

The owner downloaded an Actions artifact ZIP that contained only `app-debug.apk`, with no `app-debug-certificate.txt`. The exact reason is not known from the artifact alone: the run may have used an older workflow revision, or the certificate-export step may have failed/skipped.

## Change made

- Changed certificate extraction to the standalone `apksigner verify --print-certs-pem` invocation rather than combining both certificate-printing flags.
- Added an explicit executable check for the expected Build Tools 37.0.0 `apksigner` path, which will report a clear error in the workflow log if missing.
- The output remains written beside the APK and is listed in the artifact upload paths.

## Files changed

- `.github/workflows/android-ci.yml`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`

## Verification status

- The workflow source now contains the standalone certificate command and artifact path.
- No new GitHub Actions run has verified that the file is generated and included.
- `git diff --check` remains pending.

## Exact next action

Run Actions using the updated workflow. Check that **Export APK signing certificate details** succeeds and that the artifact ZIP contains `app-debug-certificate.txt`. If it fails, use its log message to diagnose the remaining issue.
