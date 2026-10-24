# PHASE-05-STEP-18

- **Phase and step number:** Phase 5, Step 18
- **Step title:** Add built-in/custom email templates and safe placeholder renderer
- **Status:** BLOCKED — source written; tests deferred by owner and Android compilation needs SDK
- **Completion date:** 2026-10-24

## Implementation summary

Added the domain template model, local repository and two editable built-ins: Referral Request and Open for Opportunities. Templates seed into Room only when their stable IDs are absent; built-ins cannot be deleted. Added a deterministic allow-list renderer for `{{firstName}}`, `{{companyName}}`, `{{jobTitle}}`, `{{jobUrl}}`, and `{{senderName}}`. Missing values and unknown placeholder tokens return a blocked result; template content is never evaluated. Added a Compose template list/editor and Templates navigation destination.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/domain/model/EmailTemplate.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/model/TemplateRenderResult.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/TemplateRenderer.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/repository/EmailTemplateRepository.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/EmailTemplateRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/templates/TemplateListViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/templates/TemplateListScreen.kt`

## Verification

Tests are deferred per owner instruction. No schema or library changes. Compile remains blocked by missing Android SDK.

## Exact next step

PHASE-05-STEP-19 — add the individual recipient composer, preview, explicit draft confirmation and local draft persistence.
