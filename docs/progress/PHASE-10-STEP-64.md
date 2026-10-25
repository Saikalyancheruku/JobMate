# Phase 10 Step 64 — Capture and outreach E2E corrections

**Date:** 2026-10-25  
**Status:** Source changes implemented; build, CI, and device verification pending.

## Reported issues addressed

1. Capturing several pages of one listing could start a second database job if the capture screen's ViewModel was recreated while the foreground capture session remained alive.
2. A missing listing URL could be left as an unresolved template placeholder or block draft rendering.
3. Generated email bodies needed explicit paragraph structure and SMTP should preserve line endings.
4. The sender requested a LinkedIn profile URL below the sender signature.

## Changes

- `CaptureSessionController` now retains the active `Job` in its singleton session state. `CaptureViewModel` restores that active job when created, updates the shared reference during saves, restores the previous reference on a failed save, and honors the finish-job generation boundary so an in-flight save cannot reopen a job the user already finished.
- `TemplateRenderer` treats missing `jobUrl` and `linkedInUrl` placeholders as optional, leaves no unresolved placeholder, and removes a line consisting only of a job URL/link label.
- Gemini now returns a structured greeting, 1–3 body paragraphs, and closing; JobMate assembles these sections with actual blank lines before the draft is reviewed. SMTP normalizes CRLF and CR to LF and preserves existing line breaks for both plain and resume-attached messages.
- App Settings now saves a LinkedIn profile URL through `AppPreferences`. Initial email previews/drafts and follow-up drafts append `LinkedIn: <URL>` below the sender signature; the renderer also supports an explicit `{{linkedInUrl}}` placeholder. Clearing all local data removes the saved URL. Settings content is vertically scrollable.

## Verification and limits

- `git diff --check` passed (Git emitted only line-ending notices). `:app:compileDebugKotlin` was attempted and failed before Kotlin compilation because this environment has no Android SDK location configured (`ANDROID_HOME` or `sdk.dir`). No tests were run. The current CI failure noted in the master plan is still awaiting a rerun after Step 63.
- No Actions run or Android device verification confirms that a duplicate capture is resolved. Source inspection found that the active job was previously held only in `CaptureViewModel`, while the foreground service/session controller outlives that ViewModel; this is a lifecycle path capable of losing the current job association.
- SMTP's existing `MimeMessage.setText(..., "UTF-8")` sends plain text. The new normalization preserves line endings; the paragraph content remains model-generated and requires an actual sent-mail check.
- A LinkedIn URL should be entered in **App settings → LinkedIn profile URL**, then saved. Leave it blank to omit the link.

## Recommended verification

1. Run the GitHub Actions build/unit-test/lint workflow and inspect the result before claiming compile success.
2. On the installed build, capture multiple pages from one listing, navigate away and back during the capture session, finish with ✓, then capture a second listing. Confirm exactly two saved jobs.
3. Create a draft for a job with no URL and confirm it renders/saves without a `{{jobUrl}}` token or empty `Job URL:` line.
4. Send a controlled test message and confirm paragraph breaks in Gmail, with the configured LinkedIn URL below the sender's name. Repeat with a resume selected.
