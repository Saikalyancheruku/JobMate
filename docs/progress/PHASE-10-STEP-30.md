# PHASE-10-STEP-30 — Raise compile SDK for dependency metadata compatibility

- **Phase and step number:** Phase 10, Step 30
- **Step title:** Fix CI Android SDK compatibility
- **Status:** CONFIGURATION UPDATED — CI verification pending; local SDK task blocked
- **Completion date:** 2026-10-24

## Objective

Update the Android compile SDK and CI-installed Android SDK packages to meet dependency AAR metadata requirements for API 37, without changing dependency or toolchain versions.

## Implementation summary

- Updated the version catalog's `compileSdk` value from 36 to 37; `app/build.gradle.kts` already reads this catalog value.
- Updated GitHub Actions to install Android Platform 37, Build Tools 37.0.0, platform-tools, and latest command-line tools.
- Kept `minSdk = 26`, `targetSdk = 36`, JDK 17 in CI, every dependency/toolchain version, the application package, and architecture unchanged.
- Updated the implementation plan's living status and SDK matrix to reflect the configuration change. No application source or feature behavior changed.

## Files changed

- `gradle/libs.versions.toml` — set `compileSdk = "37"`.
- `.github/workflows/android-ci.yml` — install Android SDK Platform 37, Build Tools 37.0.0, platform-tools, and command-line tools.
- `JOBMATE_IMPLEMENTATION_PLAN.md` — record the SDK matrix and pending verification in the living progress section; update the stack/CI/Phase 0 references that described compile SDK 36.
- `docs/progress/PHASE-10-STEP-30.md` — this report.

## Verification actually executed

- Static configuration inspection confirms `app/build.gradle.kts` obtains `compileSdk` from `libs.versions.compileSdk`; `targetSdk` and `minSdk` still come from their unchanged catalog entries.
- `gradlew.bat --offline --no-daemon :app:tasks` — **BUILD SUCCESSFUL**; Gradle configured the module and listed Android tasks.
- `gradlew.bat --offline --no-daemon --stacktrace :app:checkDebugAarMetadata` — **BUILD FAILED before AAR metadata evaluation** because no Android SDK location is configured (`ANDROID_HOME`/`sdk.dir` missing). This does not confirm or refute whether the API 37 requirement is satisfied.
- No Android SDK environment variables are configured in this environment; `java -version` reports Java 19.0.1 and `JAVA_HOME` points to JDK 19.
- No full Android compile/build, unit tests, lint, APK assembly, or GitHub Actions run is claimed. CI has not yet confirmed the fix.

## Remaining compatibility issues

- GitHub Actions must run successfully with the new SDK packages before the pipeline can be called fixed. The dependency metadata failure should be addressed by compile SDK 37, but additional configuration/source failures may appear afterward.
- This local environment cannot run the full Android verification tasks without Android SDK Platform 37 / Build Tools 37.x and the required JDK 17 setup.
- Approved dependency and Gradle plugin pins were not changed.

## Exact next step

Run `.github/workflows/android-ci.yml` and record actual unit-test, lint, and debug APK assembly results. Do not mark this compatibility issue resolved until a successful Actions run confirms it.
