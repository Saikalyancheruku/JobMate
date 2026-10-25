# Phase 10 — Step 61: Fix AI Settings keyboard and duplicate heading

**Date:** 2026-10-25  
**Status:** Source updated; build and Android-device verification pending.

## Findings and changes

- AI Settings displayed an in-content “AI field correction” heading directly below the shared app top bar. Removed the duplicate heading; the shared app bar continues to identify the current screen.
- The settings content used a fixed `Column` without scrolling or IME insets. Made the form vertically scrollable and applied keyboard insets so the profile field and Save button can be reached while the keyboard is open.
- Hid the app's bottom navigation while the keyboard is visible on the AI Settings route, preventing it from being pushed into the reduced form viewport. It reappears when the keyboard closes.
- Set the activity soft-input behavior to `adjustResize` for reliable keyboard-aware layout with edge-to-edge content.
- Removed root-level `safeDrawingPadding()` that had separated the system status bar from the shared TopAppBar. The app bar can now draw behind the status bar, making the black status strip and logo header one continuous app header; Material 3 bars continue to apply their own system-bar insets.

## Files changed

- `app/src/main/java/com/saikalyan/jobmate/ui/settings/AiSettingsScreen.kt`
- `app/src/main/java/com/saikalyan/jobmate/ui/navigation/AppNavHost.kt`
- `app/src/main/java/com/saikalyan/jobmate/MainActivity.kt`
- `app/src/main/AndroidManifest.xml`
- `JOBMATE_IMPLEMENTATION_PLAN.md`
- `docs/PROJECT_IMPLEMENTATION_STATUS.md`

## Verification

- No Gradle build, lint, test, or device check was run. This layout change is not yet confirmed on the user's device.

## Next action

Build through GitHub Actions, install the new APK, open AI Settings, focus the profile field, scroll to Save profile while the keyboard is open, and confirm that the shared top bar is the only page heading and the bottom navigation returns after dismissing the keyboard.
