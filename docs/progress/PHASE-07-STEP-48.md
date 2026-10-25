# Phase 7 — Step 48: Add manual Gemini job-field correction

**Date:** 2026-10-25  
**Status:** IN PROGRESS — source implemented; compile, CI, tests, and device verification pending.

## Objective

Allow the user to request online AI correction for an individual captured job without changing the existing OCR capture flow or sending screen images.

## Implementation summary

- Added a Gemini settings screen with a masked API key field. The key is encrypted at rest using an Android Keystore AES-GCM key and can be removed from settings or by Delete all local data.
- Added a manual Gemini `generateContent` request using model ID `gemini-3.5-flash-lite`, JSON response mode, and a schema for JobMate fields.
- Added a per-job correction action. It sends that job's saved raw OCR text only, then applies supported extracted fields while preserving status, applied/follow-up dates, notes, source, capture method, capture timestamp, and raw OCR.
- Added a Room `aiCorrected` marker and migration 2→3. Added Scraped and Corrected job filters; Sheets merges preserve the local correction marker.
- The Gemini Free Tier may use submitted content to improve Google products; this is disclosed in the settings UI. API quota and model access remain account-dependent.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/data/remote/GeminiJobCorrectionClient.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/secure/AiApiKeyStore.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AiSettingsScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AiSettingsViewModel.kt`

## Files modified

- Job model/entity/repository and Room schema/migration wiring
- Job list/detail UI, ViewModels, and navigation
- Settings privacy deletion and Sheets merge preservation

## Dependencies and schema

- No dependency versions changed and no new library added; Gemini REST uses HTTPS, `HttpURLConnection`, and Android `org.json`.
- Room database version raised from 2 to 3 with `MIGRATION_2_3`, adding `jobs.aiCorrected` with default `false`.

## Verification actually performed

- Source inspection and `git diff --check`.
- No successful Kotlin compile, tests, CI run, or device test is recorded.
- A supplied GitHub Actions run failed at `:app:compileDebugKotlin` on nullable `String?` values passed to required Job title/company fields. The targeted fix is recorded in [Step 50](PHASE-07-STEP-50.md).
- Local Gradle compilation cannot reach Kotlin compilation because Android SDK location is not configured in this environment.

## Known limitations / pending work

- Do not treat AI output or the Corrected filter as verified until the app builds and is exercised with captures.
- A fresh CI run is needed to confirm the model request, structured response parsing, Room migration, and UI compile.
- Google account model access, free-tier quota, and actual field accuracy need device testing.

## Exact next step

Run GitHub Actions against the updated source; resolve any new compile/test/lint/build errors and record the actual results. Then install the produced APK and test one captured job with a Gemini key.
