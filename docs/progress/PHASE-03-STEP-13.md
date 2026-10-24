# PHASE-03-STEP-13

- **Phase and step number:** Phase 3, Step 13
- **Step title:** Add CSV/XLSX contact parser and SAF document input
- **Status:** IN PROGRESS — parser source added; contact preview/mapping UI is Step 14; verification deferred by owner
- **Completion date:** 2026-10-24

## Objective

Read a user-selected CSV or XLSX document through Android's Storage Access Framework, enforce bounds, and expose its header/row data for a reviewable import preview.

## Implementation summary

Added a parser that opens only the SAF URI selected by the user, detects CSV/XLSX, copies the stream to a bounded temporary cache file, and deletes the temporary file in `finally`. CSV parsing supports quoted commas, escaped double quotes, CRLF and embedded line breaks. XLSX parsing reads the first worksheet and shared strings using `ZipFile` and Android `XmlPullParser`; no Apache POI is used. Imports are limited to 10 MB, 2,000 data rows and 100 columns. Legacy XLS and unknown formats return clear errors. This parser only previews source values; it does not insert or upload contacts.

## Files created

- `app/src/main/java/com/saikalyan/jobmate/data/importing/ContactImportParser.kt`

## Dependencies and schema

No new dependencies and no database changes. Uses Android SAF, ZIP and XML APIs already available on the platform.

## Verification

Per the owner's instruction, phase tests are deferred. Gradle compilation is also pending Android SDK setup (see Phase 2 Step 12 report); this parser has not yet been compiled.

## Known issues and pending work

The parser currently supports the first XLSX worksheet. The next step adds column mapping, preview validation, row-numbered issues, duplicate review and the document-picker UI before repository import.

## Exact next step

PHASE-03-STEP-14 — implement the contact import preview, column mapping, validation and user-reviewed import screen.
