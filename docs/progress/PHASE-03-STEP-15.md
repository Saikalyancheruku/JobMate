# PHASE-03-STEP-15

- **Phase and step number:** Phase 3, Step 15
- **Step title:** Add conservative contact matcher and user-confirmed job links
- **Status:** BLOCKED — source implementation written; phase tests deferred by owner and Android compilation requires SDK setup
- **Completion date:** 2026-10-24

## Objective

Provide explainable, conservative job/company suggestions and persist a contact/job association only after explicit user confirmation.

## Implementation summary

Added `ContactMatcher` in the domain layer. It suggests only normalized exact company-name matches or a company-name match to a non-generic email domain. Consumer email providers are excluded as evidence. Suggestions contain confidence and an explanation and always require user confirmation; the matcher itself does not write data.

Added a local job/contact link repository bound through Hilt. Confirmed links persist `USER_CONFIRMED`, confidence, and `isConfirmed=true`; links can be removed. Added a review screen reachable from each contact with Confirm link/Linked · remove actions. No automatic linking or sending occurs. No tests were added or run per owner instruction.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/domain/matching/ContactMatcher.kt`
- `app/src/main/java/com/saikalyan/jobmate/domain/repository/JobContactLinkRepository.kt`
- `app/src/main/java/com/saikalyan/jobmate/data/repository/JobContactLinkRepositoryImpl.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactMatchViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactMatchScreen.kt`

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/di/AppModule.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactListScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`

## Dependencies and schema

No new library or schema changes; links use the existing `job_contacts` table and DAO.

## Verification and pending work

Tests are deferred per owner request. Android compile/lint/build remains blocked by missing SDK. Phase 3 source scope is implemented; it must not be marked verified until SDK-backed compile and later test coverage succeed.

## Exact next step

PHASE-04-STEP-16 — implement app-private PDF storage, validation, metadata, and checksums.
