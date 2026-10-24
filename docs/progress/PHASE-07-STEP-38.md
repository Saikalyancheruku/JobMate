# Phase 7 Step 38 — Visible floating capture bubble

**Status:** IN PROGRESS — source updated; Gradle/device verification pending  
**Date:** 2026-10-24

## Objective

Provide a visible, draggable capture control over LinkedIn, Naukri, and other apps, with immediate feedback when OCR and automatic saving are in progress or complete.

## Implementation summary

- Added the Android `SYSTEM_ALERT_WINDOW` permission declaration and a setup flow that sends the user to Android's Display over other apps settings before starting MediaProjection.
- The foreground capture service creates a draggable circular `CAP` overlay. Tapping it captures the visible screen; it is temporarily hidden before the projected frame is read so it does not cover listing text.
- The bubble displays OCR/save progress, a success checkmark, or an error marker. A capture-frame timeout restores the helper and reports failure if Android does not deliver a frame.
- Added capture-session states for saving and successful saves, shared between the ViewModel, screen, and foreground service.
- Kept Android's required foreground-service notification for transparency and Stop control; removed its Capture action so the floating bubble is the external-app capture control. No AccessibilityService or automatic scrolling/clicking was added.
- Notification permission denial no longer blocks using the overlay after the user grants the overlay special access.

## Files modified

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureForegroundService.kt`
- `app/src/main/java/com/saikalyan/jobmate/service/CaptureSessionController.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureViewModel.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/capture/CaptureScreen.kt`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/progress/PHASE-07-STEP-38.md`

## Dependencies and schema

No dependency or database/schema changes.

## Verification

No Gradle build, automated test, or device test has been run for Step 38. The environment previously failed the debug assembly before compilation because no Android SDK location is configured; it also reports Java 19 rather than the approved JDK 17. The bubble must be validated on a device after CI builds the updated APK.

## User permissions and limitations

- Android's **Display over other apps** special access must be granted. Android opens its system settings for the user to enable it.
- Android's foreground-service notification remains present as required for the MediaProjection service and includes Stop. The floating bubble is the capture control.
- Some apps may block screen capture, and OCR may miss fields. Captured text is retained and saved job details can be corrected later.

## Exact next step

Run the configured GitHub Actions verification, install the resulting APK, grant Display over other apps access, and verify bubble visibility, dragging, capture feedback, automatic saving, and repeated captures on a device.
