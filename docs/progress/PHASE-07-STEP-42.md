# Phase 7 — Step 42: Layout-aware OCR field parsing

**Date:** 2026-10-25  
**Status:** Source implementation complete; build and device verification pending.

## Change

The capture parser previously received only ML Kit's flattened text, discarding the screen position of each OCR line. The recognizer now returns each line with normalized screen bounds, and the capture controller preserves that geometry through to `CaptureViewModel` and `JobFieldParser`.

The parser uses the upper-screen header region, relative line size/position, nearby company/location lines, labeled metadata, and textual layout signals associated with LinkedIn and Naukri. Experience, salary, skills, and source URL remain extracted from recognized text. Shared text continues to use the parser's text-only fallback with synthetic line positions.

## Scope and limitations

- This is still a heuristic parser. Line geometry and platform words provide layout clues, not guaranteed semantic labels.
- It does not inspect native LinkedIn/Naukri accessibility trees or network data.
- No real captured screens were available for calibration in this step.
- Existing CAP page merge and NEXT job separation behavior are unchanged.
- No tests were added or run, and no Gradle build was attempted in this step.

## Verification

Source wiring was reviewed. `git diff --check` is the available whitespace check; no successful compilation, CI run, APK build, or device verification is claimed. Verify field assignment on actual LinkedIn and Naukri screens in the next CI/device pass.
