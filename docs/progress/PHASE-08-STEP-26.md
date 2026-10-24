# PHASE-08-STEP-26 — Follow-up list and reminders

**Status:** IN PROGRESS (source implemented; verification deferred)  
**Completion date:** 2026-10-24

## Objective
List scheduled job follow-ups and post local reminders at their requested dates.

## Implementation summary
Added a follow-up screen backed by `JobRepository`, a unique WorkManager request per job, and a notification worker. Saving or editing a job replaces or cancels its reminder according to the follow-up date and closed status. Notification permission is respected on Android 13+; no email is sent by reminders.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/worker/FollowUpNotificationWorker.kt`
- `app/src/main/java/com/saikalyan/jobmate/worker/FollowUpScheduler.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/followups/FollowUpViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/followups/FollowUpScreen.kt`

## Files modified
- `app/build.gradle.kts` (WorkManager dependency, pinned version)
- `app/src/main/java/com/saikalyan/jobmate/ui/jobs/JobEditorViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Database changes
None; existing `followUpAt` field is used.

## Verification
No tests or build run per owner instruction. Android SDK verification remains pending.

## Known issues / next step
Notification delivery and date behavior require device verification. Continue with PHASE-08-STEP-27, privacy/data/settings features.
