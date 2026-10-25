# Phase 10 — Step 60: Mention selected resume attachment in email

**Date:** 2026-10-25  
**Status:** Source updated; build, CI, and device verification pending.

## Change

When the user selects a stored PDF resume in the Outreach composer:

- The recipient-specific preview and saved email body include “I've attached my resume for your reference.”
- The sentence is inserted before common email sign-offs and is not duplicated when the message already mentions an attached resume/CV.
- The selected resume remains attached through the existing per-recipient SMTP attachment flow.
- The AI prompt is instructed not to claim that a resume is attached. JobMate adds the statement only when a resume is actually selected.
- If the selected resume has disappeared from the available resume list, JobMate blocks saving rather than creating a false attachment statement.
- The composer explains that selecting a resume adds both the PDF and the statement. Leaving resume selection empty does not add attachment wording.

Individual sending and the selected-job batch-send action both use the same saved recipient-specific drafts, so this behavior applies to either send path, including first-contact and follow-up drafts where a resume was explicitly selected. Follow-up drafts continue to omit the resume by default.

## Files changed

- `app/src/main/java/com/saikalyan/jobmate/ui/outreach/OutreachViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/outreach/OutreachComposerScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/remote/GeminiReferralDraftClient.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`

## Verification

- `git diff --check` should be run after this change.
- No Gradle build, unit test, Actions run, SMTP send, or device test has confirmed this source.

## Next action

Run the configured GitHub Actions workflow, then verify on-device that a selected resume is attached and its statement appears in the per-contact preview and sent draft; also verify no attachment wording appears when no resume is selected.
