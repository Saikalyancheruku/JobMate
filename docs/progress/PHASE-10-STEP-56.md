# Phase 10 — Step 56: Export the APK signing certificate in Actions

**Date:** 2026-10-25  
**Status:** IN PROGRESS — workflow and documentation updated; Actions run pending.

## Objective

Let the owner download the SHA-1 certificate fingerprint for the exact debug APK produced by GitHub Actions without installing Android SDK tools locally.

## Workflow change

- After Gradle, run the installed Android Build Tools `apksigner` on `app-debug.apk` with `--print-certs --print-certs-pem`.
- Save SHA-1 and the public certificate details to `app-debug-certificate.txt` beside the APK.
- Print the SHA-1 in the GitHub Actions job summary.
- Upload the APK and certificate-details file together.
- Certificate extraction and upload use `always()` but are gated on the APK actually existing. Thus a later test/lint failure does not hide a produced APK; an early compile failure with no APK will not fabricate an artifact.
- Only public certificate material is exported. No private signing key or password is included.

## Files changed

- `.github/workflows/android-ci.yml`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`

## Verification status

- Inspected the existing workflow and confirmed it installs Android Build Tools 37.0.0, then builds `app-debug.apk`.
- Workflow YAML and GitHub Actions execution have not yet been validated by a run.
- Existing source/test/CI status is unchanged: Step 52's schema assertion fix and recent app changes still await CI confirmation.

## Important signing limitation

The workflow uses the runner's default debug signing key. This report identifies the SHA-1 for the exact APK downloaded from that run. A later fresh Actions run may use a different debug key, so repeatable Google OAuth testing across runs requires a stable dedicated test keystore stored as a GitHub secret and used by the workflow. The private key must never be uploaded as an artifact.

## Exact next action

Rerun GitHub Actions. Download the APK and `app-debug-certificate.txt` from the same artifact, then register the reported Android SHA-1 for package `com.saikalyan.jobmate` in the Google Cloud Android OAuth client before connecting Sheets.
