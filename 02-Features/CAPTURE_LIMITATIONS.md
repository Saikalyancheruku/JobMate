# Native capture: permissions and limitations

1. User taps Capture current job.
2. Explain screen capture purpose.
3. Launch Android MediaProjection consent prompt.
4. Start a foreground service with an ongoing notification and stop action.
5. User switches to Naukri or LinkedIn native Android app.
6. OCR visible content on-device; user manually scrolls and captures additional sections.
7. Parse candidate fields; show editable review screen; save only after confirmation.

MediaProjection is not a permanent grant. Follow target-SDK foreground-service/notification rules. Secure-window screens may be blank; never bypass this. OCR only sees visible pixels and can misread content. Job-site UI changes. The app cannot promise fields not displayed or shared by the source app. Sharesheet fallback works only when source app exposes text/URL. Discard frames after OCR; do not upload screenshots. AccessibilityService is excluded from baseline; any future use needs a separate policy/privacy review and explicit product approval.
