# PHASE-09-STEP-30 — New-sheet layout and user setup

**Status:** IN PROGRESS (source and documentation implemented; cloud setup/runtime verification pending)  
**Completion date:** 2026-10-24

## Objective
Implement the owner-approved new-sheet layout and provide an explicit in-app connection/disconnection flow.

## Owner decision
Create a new spreadsheet and use Google account authorization for Sheets. No existing Sheet URL was supplied because the app creates the target sheet.

## Layout
- `Jobs`: `id`, `deleted`, `title`, `companyName`, `location`, `description`, `skillsJson`, `experienceMin`, `experienceMax`, `salaryText`, `employmentType`, `postedAt`, `source`, `sourceUrl`, `status`, `appliedAt`, `followUpAt`, `notes`, `captureMethod`, `fingerprint`, `createdAt`, `updatedAt`.
- `Contacts`: `id`, `deleted`, `fullName`, `email`, `companyName`, `domain`, `designation`, `notes`, `createdAt`, `updatedAt`.

## Implementation summary
The Sheets settings route starts Google authorization and creates the spreadsheet on first sync. It displays the sheet link, reconnect/sync/disconnect controls, privacy boundaries, and unresolved conflict actions. Disconnect revokes the `drive.file` grant and stops periodic work. Deleting local app data also clears the Sheet link and revokes the grant; the spreadsheet itself remains in Drive.

## Verification
No tests, builds, or live Google account/API calls were performed. The current workspace has no configured Android SDK and cloud OAuth setup is not available here.

## Next step
Configure Google Cloud API/OAuth settings and run authorized device verification after test coverage is approved. Phase 10 delivery verification remains pending.
