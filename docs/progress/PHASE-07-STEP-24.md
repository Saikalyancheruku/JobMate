# PHASE-07-STEP-24 — OCR, parsing, and text sharing

**Status:** IN PROGRESS (source implemented; verification deferred)  
**Completion date:** 2026-10-24

## Objective
Recognize manually captured screen text on-device and parse candidate job fields; support shared text as a fallback.

## Implementation summary
Added ML Kit recognizer and a lightweight candidate parser with evidence/confidence metadata. OCR sections are delivered in memory to the capture ViewModel and raw frames are released after recognition. Added `ACTION_SEND` text/plain handling through MainActivity and the capture route.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/domain/usecase/JobFieldParser.kt`

## Files modified
- `app/src/main/java/com/saikalyan/jobmate/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Dependencies / schema
Uses pinned ML Kit Text Recognition 16.0.1. No database changes.

## Verification
No tests/build run per owner instruction. SDK-dependent verification remains pending.

## Known issues / next step
Parsing is heuristic and requires user review; no fixture testing yet, per owner deferral. Continue with PHASE-07-STEP-25, editable review and explicit persistence.
