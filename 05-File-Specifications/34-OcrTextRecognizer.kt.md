# File specification: `OcrTextRecognizer.kt`

**Purpose:** On-device ML Kit adapter.

## Required implementation
OCR off main thread; text blocks/bounds; handle empty output; release frames; no cloud OCR or screenshot retention.

## Acceptance criteria
- Matches the architecture, data contracts and feature catalogue.
- Handles null/empty/error states explicitly.
- Has no unrelated responsibilities.
- Includes or is covered by relevant tests.
- Compiles with the pinned compatible dependency versions.
