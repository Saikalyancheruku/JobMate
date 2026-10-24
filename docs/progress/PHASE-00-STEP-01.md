# PHASE-00-STEP-01

- **Phase and step number:** Phase 0, Step 01
- **Step title:** Verify toolchain and dependencies
- **Status:** COMPLETE
- **Completion date:** 2026-10-24

## Objective

Confirm that the approved pinned toolchain and libraries are published and reachable, confirm Android-compatible JavaMail coordinates for SMTP App Password sending, and record AGP 9 Kotlin rules before any Gradle module is created.

## Implementation summary

No application source or Gradle module was created in this step.

HTTP HEAD requests (status 200) confirmed these published artifacts:

- Android Gradle Plugin 9.4.0 (`com.android.tools.build:gradle:9.4.0` on Google Maven)
- Kotlin stdlib 2.4.21 (Maven Central)
- Gradle 9.6.0 distribution (`gradle-9.6.0-bin.zip` on services.gradle.org)
- KSP Gradle plugin 2.3.12 (Maven Central)
- Compose BOM 2026.09.00
- Room runtime 2.8.5
- Hilt Android 2.60.1
- AndroidX Hilt Navigation Compose 1.4.0
- WorkManager KTX 2.12.0
- Lifecycle runtime KTX 2.11.0
- Navigation Compose 2.10.2
- Activity Compose 1.13.0
- Core KTX 1.19.1
- Kotlin Coroutines 1.11.0
- DataStore Preferences 1.3.0
- ML Kit Text Recognition 16.0.1 (`com.google.mlkit:text-recognition:16.0.1`)
- `com.sun.mail:android-mail:1.6.7`
- `com.sun.mail:android-activation:1.6.7`

Email remains Gmail SMTP + App Password only. No OAuth/Gmail API artifacts were selected.

## Files created

- None for application code.
- This report: `docs/progress/PHASE-00-STEP-01.md`

## Files modified

- None for this verification step itself. Earlier in the same session (documentation/setup, not this Gradle step): spec email-alignment files and `JOBMATE_IMPLEMENTATION_PLAN.md`.

## Dependencies added or changed

None added to a Gradle project yet. Confirmed proposed coordinates for later catalog use; **do not add** Gmail API, AuthorizationClient, Play Services Auth for sending, or `androidx.security:security-crypto`.

## Database/schema changes

None.

## Important technical decisions

- Keep all owner-approved version pins. Artifacts exist.
- JavaMail remains `com.sun.mail:android-mail:1.6.7` and `com.sun.mail:android-activation:1.6.7`. When those libraries are added (Phase 6 catalog wiring in Phase 0 catalog is allowed), exclude `META-INF/NOTICE.md` and `META-INF/LICENSE.md` from APK packaging if the merge fails.
- AGP 9 built-in Kotlin: do not apply `org.jetbrains.kotlin.android` or `kotlin-kapt`; apply `org.jetbrains.kotlin.plugin.compose`; use KSP; Java 17 for the Android toolchain.
- JetBrains Kotlin 2.4.21 compatibility table lists AGP **8.5.2–9.3.1**. Approved pin is AGP **9.4.0** (published; Gradle min 9.6.0; JDK 17). This matrix lag is accepted for now because AGP 9 uses built-in Kotlin and the owner approved 9.4.0. Compile proof is PHASE-00-STEP-08, not this step.
- v1 email authentication stays SMTP App Password. JavaMail confirmation does not authorize OAuth.

## Tests actually executed

No unit tests (no `:app` module yet). Verification was HTTP HEAD of Maven/Google/Gradle URLs, all HTTP 200.

## Build results

No Gradle build in this step.

Local machine:

- `java -version` → Java 19.0.1 (`JAVA_HOME=C:\Program Files\Java\jdk-19`)
- No JDK 17 install found under Program Files Java / Adoptium.

CI will still use JDK 17 per the plan. Local STEP 08 should use JDK 17 (install or Gradle toolchain). This does not block STEP 01.

## Errors encountered and how they were resolved

- Initial sandboxed HEAD check could not spawn a terminal on Windows. Re-ran with unrestricted permissions; all requested URLs returned 200.

## Known issues or pending work

- Local default JDK is 19, not 17. Resolve before or during PHASE-00-STEP-08 (GitHub Actions uses JDK 17).
- Kotlin documented AGP max 9.3.1 vs approved AGP 9.4.0: watch STEP 08 for plugin incompatibility; do not change pins unless the build fails.
- JavaMail 1.6.7 is from 2021; packaging excludes may be required later.
- Android project files still do not exist.

## Exact next implementation step

**PHASE-00-STEP-02 — Create Gradle wrapper, root Gradle files, and version catalog**

Create at workspace root (do not add spec folders as modules):

- `settings.gradle.kts` (root name JobMate, include `:app` or prepare for STEP 03 if `:app` is created empty, FAIL_ON_PROJECT_REPOS, Google / Maven Central / Plugin Portal)
- root `build.gradle.kts` (plugin aliases, no duplicate versions)
- `gradle/libs.versions.toml` with approved pins
- Gradle wrapper 9.6.0 committed

Do not implement Manifest, Hilt, theme, or CI in STEP 02.
