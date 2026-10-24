# PHASE-10-STEP-35 — Align Robolectric DAO tests with CI JDK

- **Phase and step number:** Phase 10, Step 35
- **Step title:** Fix Robolectric API 36 runtime failures on JDK 17
- **Status:** TEST CONFIGURATION UPDATED — CI rerun pending
- **Completion date:** 2026-10-24

## Objective

Address three DAO test class failures reported as `UnsupportedOperationException` from Robolectric `DefaultSdkProvider` while CI uses the approved JDK 17.

## Implementation summary

The failed Robolectric classes all requested API 36. Robolectric's compatibility table states Android API 36 needs Java 21, while API 35 works with Java 17. Changed only those three JVM-only Room DAO test annotations to API 35. The CI JDK 17, app `compileSdk = 37`, `targetSdk = 36`, and `minSdk = 26` remain unchanged.

The latest CI output reached `:app:testDebugUnitTest`: main and unit-test Kotlin compilation completed, then 10 tests ran and 3 DAO test class setups failed. Lint analysis tasks began, but no successful `lintDebug` or `assembleDebug` result appears in the log. The SDK-strip and deprecation messages are warnings, not the failure.

## Files changed

- `app/src/test/java/com/saikalyan/jobmate/data/local/dao/ContactDaoTest.kt` — set Robolectric SDK to 35.
- `app/src/test/java/com/saikalyan/jobmate/data/local/dao/DatabaseRelationsTest.kt` — set Robolectric SDK to 35.
- `app/src/test/java/com/saikalyan/jobmate/data/local/dao/JobDaoTest.kt` — set Robolectric SDK to 35.
- `JOBMATE_IMPLEMENTATION_PLAN.md` — update living status, report index, and next step.
- `docs/progress/PHASE-10-STEP-35.md` — this report.

## Verification actually executed

- Inspected the supplied CI output: main compilation succeeded; unit-test compilation succeeded; `:app:testDebugUnitTest` failed with 3 DAO test class setup failures; the log does not show successful lint or APK assembly.
- Confirmed the three failing classes used `@Config(sdk = [36])` and the workflow uses JDK 17.
- No local Robolectric tests or CI rerun were performed.

## Remaining issues

- The API 35 test runtime adjustment needs CI confirmation. Other tests or lint findings may surface after the DAO classes execute.
- Full unit tests, lint, APK assembly, and device verification remain incomplete.

## Exact next step

Rerun GitHub Actions and inspect the test results. Then address any remaining test failures before claiming the workflow passes.
