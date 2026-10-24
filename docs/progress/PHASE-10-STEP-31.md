# PHASE-10-STEP-31 — Correct Android 37 SDK package identifier

- **Phase and step number:** Phase 10, Step 31
- **Step title:** Fix Android 37 SDK package resolution in CI
- **Status:** CONFIGURATION UPDATED — CI verification pending
- **Completion date:** 2026-10-24

## Objective

Correct the Android SDK package identifier after GitHub Actions reported that `platforms;android-37` is not published/found by `sdkmanager`.

## Implementation summary

Changed only the CI platform package ID from `platforms;android-37` to `platforms;android-37.0`. Kept `compileSdk = 37`, Build Tools 37.0.0, platform-tools, command-line tools, targetSdk 36, minSdk 26, JDK 17, dependency/toolchain pins, architecture, and package name unchanged. Android 17 documentation directs projects to compile with API 37 and install the Android 17 platform and Build Tools 37.x; SDK tooling for this pre-release platform publishes the platform using the `.0` package suffix.

## Files changed

- `.github/workflows/android-ci.yml` — request `platforms;android-37.0` instead of the nonexistent `platforms;android-37` package.
- `JOBMATE_IMPLEMENTATION_PLAN.md` — update the living current step, CI verification action, and report index.
- `docs/PROJECT_IMPLEMENTATION_STATUS.md` — record the corrected package ID and still-pending CI verification.
- `docs/progress/PHASE-10-STEP-31.md` — this report.

## Verification actually executed

- Confirmed the reported failure is from `sdkmanager` package lookup, before Gradle tasks begin.
- No GitHub Actions run was performed after the edit. The local environment has no Android SDK configured, so it cannot validate installation or perform a full Android build.

## Remaining compatibility issues

- CI must confirm that `sdkmanager` installs `platforms;android-37.0` and that the configured AGP resolves the installed platform for `compileSdk = 37`.
- No successful CI, full build, lint, tests, APK, or device result is claimed.

## Exact next step

Re-run GitHub Actions and inspect the SDK installation and Gradle task results. Do not report the pipeline fixed until CI passes.
