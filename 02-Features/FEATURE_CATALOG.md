# Feature catalogue and acceptance criteria

1. **Dashboard:** Room-derived counts for captured/applied jobs, outreach, due/overdue follow-ups and status distribution. Reactive and honest empty states.
2. **Job capture:** User starts screen capture, grants Android consent, opens Naukri/LinkedIn native app, captures visible screen sections, scrolls manually and captures more. OCR extracts candidate title/company/location/description/skills/experience/salary/date/source. Review/edit before saving. Sharesheet fallback for shared text/URL. Deduplicate by source URL or normalized title+company+location.
3. **Job management:** Create/read/update/archive/delete/search; statuses SAVED, APPLIED, INTERVIEW, OFFER, REJECTED, WITHDRAWN, CLOSED; applied date, notes and follow-up date.
4. **Contacts:** CSV/XLSX import through document picker; column mapping, preview, email validation, duplicate detection and row-level errors.
5. **Matching:** Company/domain suggestions with reason and confidence; generic email domains are not company evidence; manual linking; never auto-send from a match.
6. **Templates:** Editable Referral Request and Open for Opportunities; placeholders `{{firstName}}`, `{{companyName}}`, `{{jobTitle}}`, `{{jobUrl}}`, `{{senderName}}`; unresolved placeholders block send.
7. **Resumes:** Import multiple PDFs, copy into app-private storage, preview, rename, select per outreach, checksum and delete. Never upload to Sheets.
8. **Outreach:** Select jobs, contacts, template and resume; separate preview and email per recipient; explicit confirmation; per-recipient attempt/result. Unknown SMTP result requires review before retry.
9. **Follow-ups:** Manual date/status; dashboard highlights due/overdue. Notifications configurable. No automatic follow-up email in v1.
10. **Google Sheets (optional):** UUID stable IDs, WorkManager queue, backoff, conflict handling, selected structured data only. Room remains source of truth. No secrets, resumes, PDFs or screenshots. Sheet config supplied later.
11. **Offline:** Jobs, manual entry, contacts, matching, resumes, templates, drafts and dashboard work offline. SMTP/Sheets require network.
12. **Settings/privacy:** Gmail connection, Sheets configuration, theme, notifications, export/delete and disconnect controls.
13. **CI:** GitHub Actions format/lint, unit tests, Android lint, debug APK on push/PR/manual dispatch. No signing secrets in repo.
