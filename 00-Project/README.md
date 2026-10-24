# JobMate Development Specification

This ZIP is a detailed specification and commented source-file skeleton pack. It is **not** a completed or tested Android application.

## Confirmed decisions
- Native Android only; Kotlin + Jetpack Compose.
- MVVM + Repository architecture; Room/SQLite is the local source of truth.
- Package: `com.saikalyan.jobmate`; min API 26.
- No backend or hosting.
- Gmail/Google Workspace SMTP using a Gmail App Password only. Do not implement OAuth2, XOAUTH2, or the Gmail REST API for v1 sending. Never request/store a normal Gmail password.
- One separate email per recipient, with an individually rendered message and selected PDF attachment.
- Multiple local PDF resumes; no resume files or PDF contents in Google Sheets.
- Import employee contacts from CSV/XLSX; suggest matches by company/domain, with manual review.
- Templates: Referral Request and Open for Opportunities.
- Native Naukri/LinkedIn Android-app capture: user-initiated MediaProjection + on-device ML Kit OCR; Sharesheet fallback if source app shares text/URL.
- Optional Google Sheets sync; user will provide Sheet URL/config later.
- GitHub Actions builds APK on push, PR and manual workflow dispatch.

## Use
1. Read architecture, feature catalogue, data model and capture/security limitations.
2. Follow `04-Implementation-Order/BUILD_SEQUENCE.md` in order.
3. Implement each source file from its matching file specification and commented skeleton.
4. Complete tests and acceptance criteria before moving to the next phase.
5. Ask the project owner when a requirement is unclear; do not invent product behavior.

## Important limitations
MediaProjection only captures user-visible content after system consent. Secure screens may block capture. OCR is best-effort and must always be reviewed. No hidden scraping, automatic scrolling/clicking, or baseline AccessibilityService. Email sending always requires explicit user confirmation. Room data remains available offline; SMTP and Sheets sync need internet.

Use mutually compatible stable versions of Android Gradle Plugin, Gradle, Kotlin, Compose, Room, WorkManager and ML Kit at implementation time. Keep minSdk 26; choose compile/target SDK supported by the current toolchain and comply with current Android service/permission rules.
