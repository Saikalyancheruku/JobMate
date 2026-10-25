# Phase 7 — Step 44: Bind Naukri metadata and filter system chrome

**Date:** 2026-10-25  
**Status:** Source changes implemented; build and device verification pending.

## Changes

- Exclude the Android status-bar band, clock/network/battery indicators, and recognized notification banners from structured OCR lines and saved description text.
- Pass `ParsedJobFields.description` into automatic save. Previously the save path ignored the cleaned description and stored the original unfiltered frame text.
- Parse Naukri `Must have skills` and `Good to have skills` as skill groups.
- Recognize Naukri `Not disclosed` as salary text when no explicit salary amount was found.
- Extract experience ranges into both `experienceMin` and `experienceMax`, including the auto-save and shared-text save paths. Prefer a standalone experience value such as `1–6 Yrs` over a sentence mentioning experience.

## Limits and verification

This remains rule-based OCR parsing. Text recognition can still misread company names and numbers; source layout rules cannot recover characters ML Kit did not recognize. The device status band is excluded by normalized vertical position, and common notification labels are filtered, but unusual heads-up notification content may still require a clean capture (for example, enabling Do Not Disturb while capturing).

No tests were added or run, and no Gradle build was attempted for this step. Verify on a fresh APK with Naukri and LinkedIn captures before claiming field accuracy.
