# JobMate project implementation status

**Last updated:** 2026-10-25
**Overall status:** Source implementation through Phase 9 is recorded. The outreach core was extended with encrypted user-profile input, on-demand AI email drafting, company contact matching/manual selection, manual follow-up drafts, and resume attachment statements. AI Settings keyboard/header layout was adjusted. The latest supplied Actions run failed at Kotlin compilation because the Compose `WindowInsets.ime` extension import was missing; source now imports it, but CI has not rerun. Phase 10 delivery remains incomplete and no device verification is recorded for these changes.

**Build compatibility update:** application `compileSdk` is 37 to address dependency AAR metadata requirements; `targetSdk` remains 36 and `minSdk` remains 26. CI requests SDK package `platforms;android-37.0` and Build Tools 37.0.0. The change is pending a successful CI run.

The audit on 2026-10-24 found that this workspace currently has a Git checkout on `main` tracking `origin/main` and a configured GitHub remote. Earlier statements below claiming no `.git` directory or remote are stale. The audit did not push, commit, or query GitHub Actions.

## Progress by phase

| Phase | Work recorded so far | What is still unverified or incomplete |
|---|---|---|
| 0 — Foundation | Gradle wrapper, Android app module, manifest, Hilt, Compose navigation/theme, and GitHub Actions workflow are present. | Step 08 is blocked: no successful compile, lint, test run, or APK build. The recorded attempt stopped before Gradle tasks because Android SDK configuration was missing. |
| 1 — Domain and database | Models, Room entities/DAOs/database, repositories, and JVM test source are present. | Compilation and the tests have not been run. |
| 2 — Dashboard and jobs | Dashboard, job list/detail/editor, CRUD, filtering/search, and JVM test source are present. | Compilation and the tests have not been run. |
| 3 — Contacts | Contact screens, CSV/XLSX import, validation, matching, and review source are present. | Feature tests and Android build/device verification are deferred. |
| 4 — Resumes | Private PDF storage, validation, list, preview, and deletion source are present. | Feature tests and Android build/device verification are deferred. |
| 5 — Templates and outreach | Template management, rendering, separate per-contact drafts, confirmation flow, on-demand Gemini referral draft generation using job plus encrypted user profile, company-matched contact suggestions with manual selection, locally rendered contact placeholders, batch confirmation for selected-job drafts, and a resume-attached sentence when a PDF is selected are present in source. | Newly changed source is not compiled or device-verified. AI request, returned wording, attachment behavior, placeholder rendering, and 3,000-contact usability need manual validation. |
| 6 — Gmail SMTP | App Password storage, SMTP STARTTLS, per-recipient send/history, and settings source are present. Users can send individually or confirm a batch that sends separate per-contact emails. Successful sends schedule a two-calendar-day follow-up; preparing follow-ups creates separate drafts. | Compilation, migration, Gmail account authorization, actual sends, and reminder behavior have not been verified. No automatic retry was added. |
| 7 — Job capture | Consent-based MediaProjection, foreground service, on-device OCR with normalized line bounds, layout-aware LinkedIn/Naukri heuristics, automatic save, multi-page merge, explicit finish-current-job boundary, and shared-text route source are present. Steps 48–50 add manual Gemini correction and secure key storage, revert the rating-row title/company anchor, and fix nullable mapping. Steps 53–54 use camera/checkmark controls and a stateful job-generation boundary; Step 55 adds copy/open actions for the job URL in details. | Steps 43–50 and 53–55 await CI/build/device verification. OCR/layout heuristics and Gemini output are not verified; the earlier captured result shows company OCR confusion (`MBS GLOBAL` vs `MBS GnAL`). |
| 8 — Follow-ups and settings | WorkManager reminders, theme, CSV export, and local data deletion source are present. | Compilation, tests, notification behavior, export, and deletion have not been verified. |
| 9 — Google Sheets | Google authorization, generated `JobMate Sync` sheet (`Jobs` and `Contacts` tabs), sync queue/worker, and conflict review source are present. | Google Cloud setup, compilation, tests, and real account/sync/device behavior remain pending. |
| 10 — Delivery | Step 56 adds certificate output to the APK artifact; Step 57 adds the JobMate launcher/app-bar mark. The owner downloaded an artifact containing only the APK. Step 58 changes certificate extraction to the standalone `apksigner --print-certs-pem` option and adds a clear missing-tool error. | Not complete: Step 58 is not yet on a new Actions run; exact reason the previous artifact omitted the certificate file is unverified. The latest supplied test run had 13 of 14 tests pass; Step 52 fixed the stale schema assertion, but CI has not confirmed it. A stable test signing key is needed for repeatable Sheets OAuth testing across fresh Actions runners. |

Tests were explicitly deferred for the newer feature phases. Seven JVM test files currently exist for earlier jobs/database behavior; their presence does not mean they pass. No test run is recorded as successful.

## Remaining work

### Google Cloud setup for Sheets

1. Select or create a Google Cloud project.
2. Enable the Google Sheets API.
3. Configure Google Auth Platform consent for testing, select the applicable audience, and add the Google account(s) used for testing.
4. Create an Android OAuth client with package `com.saikalyan.jobmate` and the SHA-1 fingerprint of the certificate that signs the APK to be installed.

### GitHub Actions setup and verification

1. Use the already configured GitHub repository/remote for `.github/workflows/android-ci.yml`; confirm the intended branch and repository state before triggering CI.
2. Choose a stable test signing key for downloadable CI APKs. The current workflow relies on the runner's default debug key, which can differ on a fresh runner. To test Sheets from an APK downloaded from Actions, store a dedicated test keystore as GitHub Secrets (never commit it) and update the workflow to sign with it. Register that key's SHA-1 in the Android OAuth client.
3. Trigger the workflow. It is configured to run `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`, then upload the debug APK. Fix any failures and record the actual results.
4. Install the produced APK on an Android device and manually verify Google authorization, sheet creation/sync/conflicts, and the app features. CI does not launch the app or exercise interactive Google authorization on a device.

### After CI

- Add/execute the deferred feature test coverage when requested.
- Verify Gmail SMTP with a test account, and verify capture, notifications, import/export, and deletion on supported Android versions.
- Decide release signing if distributing a release APK. The current workflow produces a debug APK only.

## Actual verification record

- The Phase 0 build attempt failed before the requested Gradle tasks because `ANDROID_HOME` / `sdk.dir` was not configured.
- On 2026-10-24, `:app:tasks` configured successfully after the SDK change; `:app:checkDebugAarMetadata` could not evaluate metadata because the local Android SDK location is still missing.
- An earlier Actions run passed main and unit-test Kotlin compilation, then failed during `:app:testDebugUnitTest`: 3 Robolectric DAO test classes requested API 36, which requires Java 21; CI is configured for JDK 17. They now request API 35 per [PHASE-10-STEP-35](progress/PHASE-10-STEP-35.md). The run shows lint analysis started but no successful `lintDebug` or `assembleDebug` result. Earlier compile fixes are in [Steps 32–34](progress/PHASE-10-STEP-32.md). No CI rerun had confirmed that adjustment before the newer run below.
- A newer Actions log compiled main and test Kotlin and ran 14 unit tests; one failed because `DatabaseRelationsTest` still expected schema version 1. The current database is version 2 after Phase 7 Step 45. The assertion/test name has been updated in [PHASE-10-STEP-42](progress/PHASE-10-STEP-42.md). Lint analysis began, but the log does not show successful `lintDebug` or `assembleDebug`; CI rerun is pending.
- No successful full compilation, lint, test run, device check, or APK output is recorded.
- The 2026-10-25 Phase 7 Step 42 change preserves ML Kit text line geometry through the capture controller and uses it during title/company/location parsing. See [PHASE-07-STEP-42](progress/PHASE-07-STEP-42.md); it has not yet been built or exercised on-device.
- The 2026-10-25 Phase 7 Step 43 changes use the supplied LinkedIn/Naukri screenshots to adjust header order, location metadata cleanup, skills headings, description chrome filtering, and capture overlay visibility. See [PHASE-07-STEP-43](progress/PHASE-07-STEP-43.md); no build or device verification has been run.
- The 2026-10-25 Phase 7 Step 44 changes filter status-bar/notification noise, save the cleaned description, parse skill and salary labels, and store experience ranges in both min/max fields. See [PHASE-07-STEP-44](progress/PHASE-07-STEP-44.md); no build or device verification has been run.
- The 2026-10-25 Phase 7 Step 45 changes field bindings to separate portal source from capture method, location from work arrangement, user-tracked status from OCR, and description from local-only raw OCR. It adds structured Naukri metadata and a Room 1→2 migration. See [PHASE-07-STEP-45](progress/PHASE-07-STEP-45.md). `git diff --check` passed with line-ending notices; no Gradle build, test, CI, or device verification has been run for this step.
- The 2026-10-25 Phase 7 Step 46 uses the raw OCR ordering to select the company immediately above a rating/reviews row and the title immediately above the company, with the prior layout heuristic as fallback. It also supports a `role @company` header. See [PHASE-07-STEP-46](progress/PHASE-07-STEP-46.md); no test, build, CI, or device verification has been run.
- The 2026-10-25 Phase 7 Step 47 limits the rating-row rule to Naukri screens and filters common promotional banner text so LinkedIn/generic layouts keep their own ordering. Naukri's official recruiter guide describes semantic posting sections but not a fixed mobile layout; see [PHASE-07-STEP-47](progress/PHASE-07-STEP-47.md). No test, build, CI, or device verification has been run.
- The 2026-10-25 Phase 7 Step 48 adds manual per-job Gemini Flash-Lite correction, encrypted API-key storage, a Room correction marker, and Scraped/Corrected filters; OCR capture remains unchanged. Step 49 reverts the rating/reviews-row anchor from title/company selection while keeping rating extraction independent. Step 50 fixes the nullable title/company mapping reported by the latest supplied CI log. Reports: [Step 48](progress/PHASE-07-STEP-48.md), [Step 49](progress/PHASE-07-STEP-49.md), [Step 50](progress/PHASE-07-STEP-50.md).
- Latest verification state: the supplied Actions log failed at `:app:compileDebugKotlin` on nullable title/company values. Step 50 applies the code fix, but no CI rerun has confirmed it. A local compile attempt stopped before Kotlin compilation because Android SDK configuration is missing. `git diff --check` passed with line-ending notices; build, tests, lint, APK assembly, and device verification remain unconfirmed.
- An intermediate Actions excerpt confirmed `:app:compileDebugKotlin`, Java compilation, and Hilt tasks completed, but omitted the cause after `generateDebugLintReportModel`; see [PHASE-10-STEP-51](progress/PHASE-10-STEP-51.md). The next supplied log identifies the failure: `DatabaseRelationsTest.kt:54` asserted schema version 2 although the database has advanced to version 3 (`aiCorrected` migration). Step 52 changes only that stale assertion to 3; CI rerun is pending. This is not a successful test/build result.
- Phase 7 Step 53 changes capture overlay semantics: camera captures a page into the active job; checkmark finishes that job without stopping screen capture; the next camera capture starts another job. This addresses the reported behavior where tapping NEXT cleared the active job and caused the following screen to create another database record. Source change and UI wording are present, but CI and device behavior are unverified; see [PHASE-07-STEP-53](progress/PHASE-07-STEP-53.md).
- After device feedback showed the next capture could still append to the prior job, Step 54 replaces the one-shot finish event with a `StateFlow` generation counter. The ViewModel clears its active job when the counter changes and synchronizes against it again before processing every captured page. This targeted fix awaits CI and device confirmation; see [PHASE-07-STEP-54](progress/PHASE-07-STEP-54.md).
- Phase 7 Step 55 replaces the inert URL field in job details with inline copy and open-link actions. The open action launches only HTTP/HTTPS URLs and adds `https://` when a scheme is absent; it reports when no app can open the URL. UI/build behavior is not yet verified; see [PHASE-07-STEP-55](progress/PHASE-07-STEP-55.md).
- Phase 10 Step 56 adds an Actions step to run `apksigner verify --print-certs --print-certs-pem` against the built APK, print SHA-1 in the workflow summary, and attach the APK plus certificate-details text file in one artifact. The artifact steps run after a failed build step only when the APK exists. No Actions run has confirmed the change. The runner-generated debug signing certificate may change on later runs; this provides the fingerprint for that exact artifact, not a stable signing identity. See [PHASE-10-STEP-56](progress/PHASE-10-STEP-56.md).
- Phase 10 Step 57 creates a single J/check JobMate mark in forest green and amber, uses it as the Android adaptive launcher icon, and places it beside app-bar titles. Source/resource wiring is present; no Gradle build or device visual check has been run. See [PHASE-10-STEP-57](progress/PHASE-10-STEP-57.md).
- The owner reported that an Actions ZIP contained only the APK despite the certificate-export change. The cause (old workflow revision versus export-step failure) is unknown from the available details. Step 58 switches to the standalone `apksigner verify --print-certs-pem` invocation, checks that the tool exists, and writes certificate details beside the APK. A new Actions run is required; see [PHASE-10-STEP-58](progress/PHASE-10-STEP-58.md).
- The 2026-10-25 cross-cutting UI refresh updates navigation, theme, dashboard, job list/detail, and capture guidance. See [UI-REDESIGN-2026-10-25](progress/UI-REDESIGN-2026-10-25.md); no build or device visual verification has been run.
- GitHub Actions reported a `NavigationBarItem` scope error in `AppNavHost.kt`; the More destinations now use a bottom sheet. Local Kotlin compilation could not start because no Android SDK path is configured. See [UI-REDESIGN-CI-FIX-2026-10-25](progress/UI-REDESIGN-CI-FIX-2026-10-25.md); CI confirmation remains pending.
- The audit made no Git mutations; the checkout already had `.git` and an `origin` remote.
- The root `.gitignore` is absent, and `app/schemas/` has no exported Room schema JSON yet (only `.gitkeep`).

See [PROJECT-AUDIT-2026-10-24.md](progress/PROJECT-AUDIT-2026-10-24.md) for source-level phase findings, build readiness risks, and security review.

See the [implementation plan](../JOBMATE_IMPLEMENTATION_PLAN.md) and individual reports in [`progress/`](progress/) for step-by-step details.
