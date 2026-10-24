# PHASE-03-STEP-14

- **Phase and step number:** Phase 3, Step 14
- **Step title:** Add contact import preview, mapping, validation, and review UI
- **Status:** BLOCKED — source implementation written; phase tests deferred by owner and Android compilation requires SDK setup
- **Completion date:** 2026-10-24

## Objective

Let users map source columns, inspect row-level validation and duplicates, and explicitly import only eligible contacts.

## Implementation summary

Added import mapping/preview models and an injected mapper. It normalizes email addresses, requires name and email mappings, validates basic email structure, reports missing/invalid values by parsed row number, and marks duplicates found in the local database or within the file. Added a Hilt import ViewModel that reads SAF documents, remaps candidates when the user changes a column, checks current contacts, and calls the existing atomic repository import only after an explicit button action. No emails are sent.

Added an OpenDocument picker with CSV/XLSX MIME filters and an import screen for mapping, eligible/duplicate/invalid counts, row review and import. No tests were added or run per owner instruction.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/data/importing/ContactImportModels.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactImportViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactImportScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactListViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/contacts/ContactListScreen.kt`

## Files modified

- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Dependencies and schema

No new libraries or schema changes. Uses existing SAF, Compose, Hilt, repository, and Room import transaction.

## Verification and pending work

Phase tests are intentionally deferred at the owner's request. Android compilation remains unverified because no Android SDK is configured. Contact CRUD screens use the existing repository; company matching/review and confirmed link persistence are implemented in Step 15.

## Exact next step

PHASE-03-STEP-15 — add conservative company/domain suggestions and manually confirmed job/contact linking.
