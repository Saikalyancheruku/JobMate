# Phase 10 Step 41 — SemVer identity for every APK build

**Status:** IN PROGRESS — version scheme and CI wiring added; workflow verification pending  
**Date:** 2026-10-25

## Objective

Give every local and CI APK a valid, identifiable SemVer version while keeping CI Android `versionCode` values increasing.

## Implementation summary

- Added root `VERSION` with the current release line `0.1.0`.
- Replaced hard-coded `versionName` and `versionCode` with Gradle logic that validates the base version and emits SemVer build metadata.
- Local builds use `0.1.0+local.<epoch_milliseconds>`.
- CI builds use `0.1.0+ci.<run_number>.<run_attempt>` and `versionCode = 1,000,000 + (run_number × 1,000) + run_attempt`.
- CI validates `VERSION`, attempt range, and Android's maximum `versionCode`; the uploaded artifact name includes the full SemVer build identifier.
- Documented MAJOR/MINOR/PATCH bump rules and metadata semantics in the implementation plan.

## Files created

- `VERSION`
- `docs/progress/PHASE-10-STEP-41.md`

## Files modified

- `app/build.gradle.kts`
- `.github/workflows/android-ci.yml`
- `JOBMATE_IMPLEMENTATION_PLAN.md`

## Dependencies and schema

No dependency, database, or schema changes.

## Verification

No Gradle build or GitHub Actions run was performed for Step 41. This environment has no configured Android SDK, so an APK build cannot be confirmed here. The workflow must run to verify the resolved `versionName`, `versionCode`, and artifact name.

## Versioning rules

- Increase PATCH for backward-compatible fixes.
- Increase MINOR for backward-compatible features and reset PATCH to zero.
- Increase MAJOR for incompatible changes and reset MINOR and PATCH to zero.
- CI run and attempt values are SemVer build metadata. They uniquely identify build artifacts without changing release precedence.

## Exact next step

Run GitHub Actions and confirm that the APK contains the expected versionName/versionCode and uploads under the versioned artifact name. Then install it and confirm it upgrades the previous debug APK.
