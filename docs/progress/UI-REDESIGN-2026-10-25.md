# JobMate UI refresh

**Date:** 2026-10-25  
**Status:** UI source updated; build and Android visual verification pending.

## Updated

- Replaced the crowded horizontal feature-chip strip with a branded app bar and a four-item bottom navigation for Home, Jobs, Capture, and More. Secondary features remain available from More.
- Added a consistent evergreen and warm-neutral palette, dark palette, and rounded component shapes.
- Reworked the dashboard with a welcome header, prominent next-action card, job metrics, grouped status counts, and more legible recent-job cards.
- Restyled job list cards and job details with clearer hierarchy, status/location badges, separate metadata cards, and horizontally scrollable status choices.
- Reorganized the capture screen into concise steps, a session status card, and full-width actions.
- Corrected singular experience display (`1 year`).

## Verification

No Gradle build, tests, APK install, or device screenshot review was run. Verify the bottom navigation and layouts on a phone-sized device, including long descriptions and small screens. Existing feature routes remain reachable through More.
