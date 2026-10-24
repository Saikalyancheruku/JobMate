# PHASE-00-STEP-02

- **Phase and step number:** Phase 0, Step 02
- **Step title:** Create Gradle wrapper, root Gradle files, and version catalog
- **Status:** COMPLETE
- **Completion date:** 2026-10-24

## Objective

Create the Gradle 9.6.0 wrapper and root Gradle files (`settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`) with approved toolchain pins. Do not create the `:app` module.

## Implementation summary

Root Gradle scaffolding now exists at the workspace root. Spec folders remain documentation, not modules. `:app` is still not included.

Approved pins were kept. AGP 9 built-in Kotlin is used: `org.jetbrains.kotlin.android` and `kotlin-kapt` are not applied. Compose compiler plugin `org.jetbrains.kotlin.plugin.compose` is declared at Kotlin 2.4.21. Root `buildscript` classpaths upgrade KGP and KSP to the approved versions so AGP 9 does not stay on its bundled older KGP.

Email remains Gmail SMTP + App Password only. JavaMail Android artifacts are catalogued; no OAuth, Gmail API, or `security-crypto` entries were added.

## Files created

- `gradle.properties`
- `gradle/libs.versions.toml`
- `build.gradle.kts`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradlew`
- `gradlew.bat`
- This report: `docs/progress/PHASE-00-STEP-02.md`

## Files modified

- `JOBMATE_IMPLEMENTATION_PLAN.md` (living progress table only)
- `settings.gradle.kts` was already present from earlier scaffolding and was **not** changed: root name `JobMate`, `FAIL_ON_PROJECT_REPOS`, Google / Maven Central / Plugin Portal, no `include(":app")`

## Dependencies added or changed

Catalog pins (not applied to an Android module yet):

| Item | Version |
| ---- | ------- |
| Android Gradle Plugin | 9.4.0 |
| Gradle wrapper | 9.6.0 |
| Kotlin / Compose compiler plugin | 2.4.21 |
| KSP | 2.3.12 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| Hilt | 2.60.1 |
| AndroidX Hilt | 1.4.0 |
| WorkManager | 2.12.0 |
| Lifecycle | 2.11.0 |
| Navigation | 2.10.2 |
| Activity Compose | 1.13.0 |
| Core KTX | 1.19.1 |
| Coroutines | 1.11.0 |
| DataStore Preferences | 1.3.0 |
| ML Kit Text Recognition | 16.0.1 |
| `com.sun.mail:android-mail` / `android-activation` | 1.6.7 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 26 |

Test libraries (HTTP HEAD 200 before cataloguing):

| Item | Version |
| ---- | ------- |
| JUnit | 4.13.2 |
| AndroidX Test Ext JUnit | 1.3.0 |
| AndroidX Test Core | 1.7.0 |
| Espresso Core | 3.7.0 |
| MockK / MockK Android | 1.14.5 |
| Turbine | 1.2.1 |
| Hilt testing / Room testing / coroutines-test | same as library pins |
| Compose UI test | from Compose BOM |

**Not added:** `org.jetbrains.kotlin.android`, `kotlin-kapt`, Gmail API, AuthorizationClient, Play Services Auth, `androidx.security:security-crypto`.

## Database/schema changes

None.

## Important technical decisions

- Keep all owner-approved toolchain pins. No silent version change.
- JetBrains Kotlin 2.4.21 fully-supported AGP range is still **8.5.2–9.3.1**. Approved AGP is **9.4.0**. Same accepted matrix lag as STEP-01. Compile proof remains PHASE-00-STEP-08.
- AGP 9.4.0 official table: Gradle min/default 9.6.0, JDK 17, max API 37 (compile/target 36 is valid).
- Compose compiler is the Kotlin plugin `org.jetbrains.kotlin.plugin.compose` at 2.4.21, not a separate compiler artifact.
- KSP 2.3.12 uses independent KSP2 versioning (not `2.4.21-x`). Published and catalogued as approved.
- To honor Kotlin 2.4.21 with AGP built-in Kotlin, root `buildscript` adds `kotlin-gradle-plugin` 2.4.21 and `symbol-processing-gradle-plugin` 2.3.12. This is wiring, not a pin change.
- `settings.gradle.kts` does not `include(":app")` yet. That is STEP-03.
- `gradle.properties` does **not** set `android.builtInKotlin=false` or `android.newDsl=false`.
- JavaMail packaging excludes for `META-INF/NOTICE.md` / `LICENSE.md` remain a later APK packaging concern, not this step.

## Tests actually executed

No unit tests (`:app` still absent).

HTTP HEAD 200 for previously unpinned test artifacts listed above.

Gradle:

- Bootstrap: downloaded `gradle-9.6.0-bin.zip`, ran `gradle wrapper --gradle-version 9.6.0 --distribution-type bin --no-daemon` → `BUILD SUCCESSFUL`
- `.\gradlew.bat --no-daemon --version` → Gradle **9.6.0**
- `.\gradlew.bat --no-daemon help` → `BUILD SUCCESSFUL`

## Build results

Wrapper distribution URL: `https://services.gradle.org/distributions/gradle-9.6.0-bin.zip`.

`.\gradlew.bat --no-daemon --version`:

```
Gradle 9.6.0
Build time:    2026-06-18 23:19:03 UTC
Revision:      3f750f03d77e42327c5f9fcb9992110088330a32
Kotlin:        2.3.21
Groovy:        4.0.32
Launcher JVM:  19.0.1 (Oracle Corporation 19.0.1+10-21)
Daemon JVM:    C:\Program Files\Java\jdk-19
OS:            Windows 11 10.0 amd64
```

The Kotlin 2.3.21 line is **Gradle’s embedded Kotlin for the Gradle runtime**, not the project KGP pin (2.4.21 in the catalog / `buildscript`).

`help` succeeded with no `:app` module.

No Android compile or APK in this step.

## Errors encountered and how they were resolved

- First sandbox terminal spawn failed on Windows (no filesystem isolation). Re-ran Gradle and HTTP checks with unrestricted permissions; succeeded.

## Known issues or pending work

- Local launcher JDK is still **19**, not 17. CI will use JDK 17. Enforce Java 17 on `:app` in STEP-03 / STEP-08.
- Kotlin documented AGP max 9.3.1 vs approved AGP 9.4.0 remains a STEP-08 watch item. Pins were not changed.
- `:app` module, Manifest, Hilt Application, theme, and CI are not created yet.
- JavaMail 1.6.7 packaging excludes may be required when the library is applied.

## Exact next implementation step

**PHASE-00-STEP-03 — Create the `:app` module**

Create `app/build.gradle.kts` with namespace `com.saikalyan.jobmate`, minSdk 26, compile/target 36, Compose, Hilt, KSP, Java 17, AGP 9 built-in Kotlin rules. Include `:app` from `settings.gradle.kts`. Do not implement Manifest/Hilt Application/theme/CI in STEP-03 beyond what that step requires.
