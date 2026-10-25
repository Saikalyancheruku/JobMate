# Outreach Startup Crash Fix — 2026-10-25

**Status:** Source fix applied; build and device verification pending.

## Device evidence

The supplied Realme RMX3660 Android bug report contains repeated `FATAL EXCEPTION: main` entries for `com.saikalyan.jobmate`. Each relevant crash reports:

- `java.lang.ExceptionInInitializerError` while creating the Hilt ViewModel for Outreach.
- Root cause: `java.util.regex.PatternSyntaxException` in `TemplateRenderer` static initialization.
- Faulty pattern: `\\{\\{([^{}]*)}}`; Android ICU rejects the unescaped closing braces (error near index 13).
- Source location reported by the APK: `TemplateRenderer.kt:34`.

This explains why the app closes when Outreach is opened: Hilt constructs `OutreachViewModel`, which injects `TemplateRenderer`, causing the invalid regex to compile during class initialization.

## Change

Updated `app/src/main/java/com/saikalyan/jobmate/domain/usecase/TemplateRenderer.kt` to escape both closing braces:

```kotlin
Regex("\\{\\{([^{}]*)\\}\\}")
```

The expression continues to match documented `{{placeholder}}` forms while avoiding the Android `PatternSyntaxException`.

## Verification status

- The bug report confirms the original crash and root cause.
- No new APK has been built or installed with this correction yet.
- Local Gradle compilation is blocked because this workspace has no configured Android SDK (`ANDROID_HOME` or `sdk.dir`).
- A GitHub Actions build and opening Outreach on the rebuilt APK are still required to confirm the fix.
