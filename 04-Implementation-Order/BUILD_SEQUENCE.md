# Build sequence — whole application file by file

The selected approach is to implement the entire application in dependency order.

## Phase 0: baseline
1. settings.gradle.kts
2. root build.gradle.kts
3. gradle/libs.versions.toml
4. app/build.gradle.kts
5. AndroidManifest.xml
6. JobMateApplication.kt, MainActivity.kt
7. JobMateTheme.kt, AppNavHost.kt
Exit: Gradle sync, app launch, CI debug build.

## Phase 1: domain and Room
8. Domain enums/models and repository interfaces
9. Room entities, DAOs, converters, database
10. Repository implementations and DI
Exit: CRUD, migration and DAO tests.

## Phase 2: jobs/dashboard
11. Dashboard use case/ViewModel/screen
12. Job list/detail/editor/search/status
Exit: offline job CRUD and reactive dashboard.

## Phase 3: contacts
13. CSV/XLSX parsers and document picker
14. Import preview, column mapping, validation
15. Contact repository and matcher
Exit: import errors visible and matching reviewable.

## Phase 4: resumes
16. PDF private storage, metadata, checksum
17. Resume list, preview, rename, delete
Exit: persists across restart; safe deletion.

## Phase 5: templates/outreach
18. Seed/edit templates and placeholder renderer
19. Composer, per-recipient previews, draft persistence
Exit: individual preview per contact.

## Phase 6: Gmail SMTP (App Password only)
20. Gmail settings UI and Keystore-backed App Password storage
21. SMTP STARTTLS client (smtp.gmail.com:587); no OAuth2/XOAUTH2/Gmail API
22. Per-recipient send attempts and error handling
Exit: test account with App Password; explicit confirmation; one recipient per message; no OAuth client ID required.

## Phase 7: capture
23. MediaProjection consent and foreground service
24. ML Kit OCR adapter and field parser
25. Review/edit/deduplicate/save; Sharesheet fallback
Exit: device validation; blocked screens fail gracefully.

## Phase 8: follow-ups/settings
26. Follow-up management and notifications
27. Settings, privacy, export/delete, theme
Exit: no automatic follow-up sending.

## Phase 9: optional Sheets
28. Google Identity authorization and Sheets API client; Gmail SMTP remains App Password-only.
29. Sync queue, WorkManager and conflict UI.
30. Owner-approved generated spreadsheet: `JobMate Sync` with `Jobs` and `Contacts` tabs; configure Sheets API and an Android OAuth client for live account verification.
Exit: offline-first sync; no secret or resume upload.

## Phase 10: quality/delivery
31. Unit/DAO/integration/UI tests
32. Lint, format, dependency and privacy review
33. GitHub Actions APK artifact
34. Release checklist and device validation
Exit: CI green and documented limitations.

For each file, give the AI tool architecture + feature spec + exact file spec + skeleton + current project tree. Ask it to implement only that file and necessary narrow changes. Review, compile and test after each coherent phase.
