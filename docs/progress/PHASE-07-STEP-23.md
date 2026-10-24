# PHASE-07-STEP-23 — User-consented capture lifecycle

**Status:** IN PROGRESS (source implemented; verification deferred)  
**Completion date:** 2026-10-24

## Objective
Implement consent-gated, visible MediaProjection capture with explicit manual frame requests.

## Implementation summary
Added a foreground service with API 29+ mediaProjection type, Android system consent data, ongoing notification and Stop action. The service discards non-requested frames and does not persist images. Added a Compose capture screen that explains capture, requests notification permission where applicable, and launches Android's system consent prompt. Fixed callback registration/unregistration to use the same callback instance.

## Files created
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`

## Files modified
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureSessionController.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`

## Dependencies / schema
No database changes. ML Kit dependency was already declared at its approved version pin.

## Verification
No tests or Android build were run per owner instruction. Android SDK is unavailable in this environment; build verification remains pending.

## Known issues / next step
Device/API-specific MediaProjection behavior remains unverified. Continue with PHASE-07-STEP-24, OCR parsing and review/save flow.
