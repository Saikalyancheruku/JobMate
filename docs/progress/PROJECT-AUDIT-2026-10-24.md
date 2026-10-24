# JobMate Project Audit — 2026-10-24

**Scope:** Static source, documentation, Gradle, workflow, and security review. No application code was changed. No Gradle build, tests, CI run, or device check was performed during this audit.

## 1. Actual source implementation status

“Source present” below means the corresponding implementation files and flows exist in the checked-out tree. It is not evidence that they compile, pass tests, or work on a device.

| Phase | Source found | Verification status |
|---|---|---|
| 0 — Foundation | Single `:app` module; package `com.saikalyan.jobmate`; Hilt Application; Compose theme/navigation; manifest; workflow | Gradle task configuration was reported successful in earlier step reports. Full compile, lint, tests, and APK assembly are not successful/verified. Step 08 remains blocked. |
| 1 — Room/domain | Nine Room entities, DAOs, converters, version-1 database, repository contracts/implementations, Hilt bindings | Seven JVM test source files exist, but no successful test or source compilation result is recorded. |
| 2 — Dashboard/jobs | Room-backed dashboard and job list/detail/editor ViewModels and Compose screens | Source exists; tests/build/device behavior unverified. |
| 3 — Contacts/import | Contact list/import/match screens, parsers, CSV/XLSX path, mapping and repository source | Source exists; feature tests and build/device verification unverified. |
| 4 — Resumes | Private resume storage/repository, PDF validation, list, preview, rename/delete source | Source exists; PDF/device and test verification unverified. |
| 5 — Templates/outreach | Template CRUD and rendering, draft composer, preview/confirmation/history source | Source exists; feature tests and send-flow verification unverified. |
| 6 — Gmail SMTP | Keystore-backed App Password storage, Gmail settings, SMTP client, per-recipient attempt history and explicit retry confirmation | Source exists; compile/tests, Gmail account eligibility, STARTTLS, send, and device checks unverified. |
| 7 — Capture/OCR | Consent flow, MediaProjection foreground service, on-device ML Kit OCR, parser, review/save and sharesheet source | Source exists; API-level behavior, tests, and device verification unverified. |
| 8 — Follow-ups/settings | WorkManager reminders, theme, CSV export, local data deletion and settings source | Source exists; notifications, export/deletion behavior, tests, and device verification unverified. |
| 9 — Google Sheets | `drive.file` authorization, REST client, new-sheet layout, durable sync queue/worker, tombstones and conflict-review source | Source exists; Google Cloud setup, compilation, tests, live account/API calls, and device sync unverified. |
| 10 — Delivery | Delivery workflow and checklist exist | Incomplete: no successful full build, completed CI run, or device verification. |

Seven JVM test files are present, covering a subset of Phase 1–2 behavior (database/converters, job status, dashboard, and job workflow). There are no `androidTest` source files in the checked tree. The presence of tests does not mean they pass. Later-phase test coverage was explicitly deferred.

## 2. Missing files or incomplete functionality

- The project has a Gradle wrapper, version catalog, root/app Gradle scripts, and `:app`; `settings.gradle.kts` includes `:app`.
- `app/schemas/` contains only `.gitkeep`; Room exports schemas to that directory and `exportSchema = true`, but no generated version-1 schema JSON is checked in yet.
- No `.gitignore` exists at the workspace root. Add appropriate ignores for local SDK configuration, build outputs, Gradle state, and signing materials before relying on the checkout for routine development or CI.
- The source implements the listed Phase 0–9 flows, but build/test/runtime verification is incomplete across all of them. Phase 10 is not complete.
- Google Sheets still needs Sheets API enablement and Android OAuth configuration for the package and the certificate SHA-1 that signs the APK used on the test device. A CI runner's default debug key may not be stable across runs.
- The current workflow produces a debug APK only. Release signing and release distribution remain undecided/out of scope.
- CSV export exists; round-trip import/export is not implemented according to the Phase 8 report.

## 3. Potential build/configuration blockers

1. **Android SDK absent here:** `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset. The Phase 0 Step 08 record says Gradle failed before the requested tasks because no SDK location/`sdk.dir` was configured. Platform 36 and Build Tools 36.0.0 are required by the workflow.
2. **Local JDK mismatch:** this environment reports Java 19.0.1; the approved toolchain and CI use JDK 17. A local full build needs JDK 17 selected/configured.
3. **AGP/Kotlin compatibility risk:** approved pins are AGP 9.4.0 and Kotlin 2.4.21. Phase 0 reports note Kotlin's documented AGP support range at the time ended at 9.3.1. This is a compatibility risk, not a confirmed compile failure; preserve pins unless an actual build proves they must change and obtain owner approval before changing them.
4. **Room schema generation pending:** KSP is configured with `room.schemaLocation` and Room has `exportSchema = true`; the schema output has not been generated/checked in. Verify this during the first successful KSP/build run and preserve the resulting schema artifact.
5. **No root ignore rules:** the missing `.gitignore` increases the chance of accidentally tracking `local.properties`, build output, or signing files. No such credential/config files were found among the currently tracked filenames in the inspected checkout.
6. **CI is configured but not verified:** `.github/workflows/android-ci.yml` installs SDK 36 and runs `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`, then uploads a debug APK. The progress records show no dispatched/successful workflow. This audit did not query GitHub Actions.
7. **Test breadth is limited:** seven JVM test files exist; no Android instrumentation tests or later-phase tests exist in the tree. The workflow's unit-test task can still fail due to compile/configuration issues.

Gradle scripts use catalog pins, AGP's built-in Kotlin approach (no `org.jetbrains.kotlin.android` or `kotlin-kapt` plugin), Compose compiler plugin, KSP for Room/Hilt, Hilt Android, Java 17 compile options, and JavaMail packaging excludes. Room KSP wiring and schema export are present. The `hilt-work` aliases exist in the catalog but are not applied; current workers use ordinary `CoroutineWorker` construction and a Hilt entry point where needed, so this is not by itself a demonstrated blocker. Earlier `:app:tasks`/configuration successes are not Android compilation or APK successes.

## 4. Security findings

- **Gmail credentials:** no hardcoded credential literal was found in the inspected source/workflow, and no tracked file with a credential/config filename pattern (`local.properties`, `google-services.json`, keystore, `.env`, or secrets file) was found. This is a current-tree check, not a historical Git-commit or remote-repository scan. The SMTP App Password implementation uses Android Keystore AES-GCM and stores ciphertext in Preferences; logs/debug SMTP are disabled in the inspected client.
- **SMTP:** source configures `smtp.gmail.com:587`, required STARTTLS, server identity checking, timeouts, and a one-recipient send API. It has not been exercised against Gmail.
- **Sheets authorization:** source requests the separate `drive.file` scope; the bearer token is passed transiently to the Sheets HTTP client/worker and is not designed as an SMTP credential. The Sheets field layout covers structured job/contact data, not passwords, outreach drafts, resumes, or screenshots. Live authorization and API behavior remain unverified.
- **Resume PDFs:** stored below app-private `filesDir/resumes`; the FileProvider exposes only that subdirectory and is non-exported with temporary URI grants. Room holds metadata, not PDF bytes. Manifest sets `allowBackup=false`.
- **Screenshots:** capture source sends selected frames to on-device OCR and does not persist or upload screenshots in the inspected flows. No screenshot-upload path was found.
- **Accessibility:** no `AccessibilityService` class or manifest declaration was found.
- **Email automation:** sending is invoked from the user-facing outreach flow; no SMTP WorkManager job was found. UNKNOWN sends require an explicit user confirmation after checking Gmail Sent; no automatic UNKNOWN retry path was found.
- These are source-level findings only. Privacy behavior still needs tests and device review.

## 5. Documentation inconsistencies

- The living plan now separates Phase 0–9 source implementation from Phase 10 verification, and states that no successful full build/device verification exists. Older per-step reports retain their historical wording and were not rewritten.
- `docs/PROJECT_IMPLEMENTATION_STATUS.md` said there was no `.git` directory or GitHub remote, but this audit's read-only inspection found a Git checkout on `main` tracking `origin/main` with the GitHub remote configured. No Git mutation was made. The status document should be corrected so it does not report this as absent.
- Some Phase 0–6 reports describe implementation steps as `COMPLETE` while also saying compilation/feature verification is pending. Read those as completion of the small source/configuration step only; they do not establish phase functionality or Phase 10 delivery.
- Phase 0 Step 07 says no remote/repository was configured at that time. That is historical and conflicts with the current checkout state; do not retroactively rewrite that report.
- The checked-out remote presence alone does not establish that GitHub Actions has run. The workflow has no successful run result in the reviewed project records; the audit did not query GitHub.

## 6. Exact recommended next actions

1. Configure JDK 17 and Android SDK Platform 36 / Build Tools 36.0.0 for the selected local or CI verification environment.
2. Add root ignore rules for `local.properties`, Gradle/build outputs, and signing material; confirm no secrets are tracked before running or publishing builds.
3. Run the configured workflow (or its exact Gradle command) and capture separate actual results for unit tests, lint, and `assembleDebug`. Fix compile/configuration failures without changing approved pins unless evidence requires it and the owner approves.
4. Confirm KSP generates the Room schema under `app/schemas/`; review and retain the version-1 export needed for future migrations.
5. After build output exists, use a stable test signing certificate for any CI APK used with Sheets; configure Sheets API and the Android OAuth client for package `com.saikalyan.jobmate` and that certificate SHA-1.
6. Install the verified APK on a device and check the interactive flows: Sheets authorization/sync/conflicts; Gmail App Password connection and controlled one-recipient send; capture consent/OCR; resume privacy; import/export; reminders; local data deletion.
7. Add and run the deferred phase-specific tests when the owner is ready, then update Phase 10 only from actual results. Decide release signing separately before any release distribution.

**Audit outcome:** implementation source is recorded through Phase 9; project verification and Phase 10 delivery remain incomplete. No successful full build, CI run, or device verification is claimed.
