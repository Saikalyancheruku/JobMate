# Outreach core — profile-based referral drafts and follow-ups

**Date:** 2026-10-25  
**Status:** Source changes implemented; build, CI, and device verification pending.

## Product decisions captured

- Suggest contacts by normalized company-name match, while leaving recipient selection manual.
- Generate a reusable referral-email draft from the selected job and the user's saved profile. Do not research the company or send contact data to the AI provider.
- Render recipient-specific placeholders locally for each selected contact.
- Send messages individually through the existing SMTP flow, after explicit confirmation.
- Also allow one batch confirmation for ready drafts belonging to the selected job; each recipient still receives a distinct SMTP message.
- After a successful send, remind the user after two calendar days. The user reviews and sends each follow-up manually.

## Source changes

- Added a professional-profile editor to AI settings. The profile is encrypted with Android Keystore AES-GCM and persisted separately from the API-key value.
- Added a Gemini referral-draft client that sends selected job fields and the saved profile only when the user requests generation. The request excludes contact records, raw screen images, and unrelated job captures.
- Updated the Outreach composer with company-match filtering, contact search, manual contact selection, AI draft generation, per-contact previews, and follow-up preparation.
- Added a batch-send option for all ready drafts for the selected job. It summarizes recipients before confirmation, processes messages sequentially, continues past per-recipient FAILED/UNKNOWN results, and never retries an existing UNKNOWN attempt.
- Added local placeholders `{{contactName}}`, `{{contactEmail}}`, and `{{contactCompany}}`; existing placeholders remain available.
- Added outreach metadata for initial/follow-up messages, parent linkage, follow-up count, and next due time. A successful SMTP send schedules the next due time two local calendar days later. Preparing a follow-up atomically clears the due marker and creates a separate draft; it does not send.
- Added the dashboard count for due reminders and unsent follow-up drafts awaiting review.
- Added Room migration 3→4 for the follow-up fields and indexes, preserving existing outreach rows.
- Updated the existing database-version assertion to expect schema v4; this test was not run.
- Updated the AI privacy description and template placeholder help text.

## Safety and data handling

- Emails remain one recipient per SMTP send. The user can retain individual confirmation or confirm a batch of ready drafts for the selected job once.
- `FAILED` and `UNKNOWN` attempts do not create follow-up schedules. Existing explicit verification is still required before retrying an `UNKNOWN` attempt.
- Contact names/emails are inserted on-device and are not included in the Gemini request.
- Follow-up drafts do not reattach a resume automatically.
- No OAuth/Gmail API, backend, live company research, automatic send, automatic retry, or feature unrelated to this outreach flow was added.

## Files changed

- `app/src/main/java/com/saikalyan/jobmate/data/local/JobMateDatabase.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/dao/OutreachDao.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/local/entity/OutreachEntity.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/remote/GeminiReferralDraftClient.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/OutreachDraftRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/OutreachSendRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/secure/AiApiKeyStore.kt`
- `app/src/main/java/com/saikalyan/jobmate/di/AppModule.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/model/JobDashboardSummary.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/model/OutreachDraft.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/repository/OutreachDraftRepository.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/TemplateRenderer.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/dashboard/DashboardScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/dashboard/DashboardViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/outreach/OutreachComposerScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/outreach/OutreachViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AiSettingsScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AiSettingsViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/templates/TemplateListScreen.kt`
- `app/src/test/java/com/saikalyan/jobmate/data/local/dao/DatabaseRelationsTest.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`

## Verification and next action

- No Gradle build, unit tests, lint, Actions run, AI API request, SMTP send, Room migration run, or Android-device test was performed for this change.
- Run the GitHub Actions workflow on this source revision. Resolve any compiler, Room schema, test, or lint errors before installing the APK.
- On device, verify AI settings save/load across app restart; generate one draft and inspect that it uses the selected job/profile; confirm contact fields are not sent to Gemini; manually select company matches; save and send separate drafts; verify the dashboard count appears two calendar days after a successful send; prepare, edit/review, and manually send a follow-up; verify that `FAILED`/`UNKNOWN` do not schedule one.
- The source changes are not confirmed to work until those checks pass.
