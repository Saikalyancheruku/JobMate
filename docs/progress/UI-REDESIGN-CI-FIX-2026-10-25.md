# UI redesign CI compile fix

**Date:** 2026-10-25  
**Status:** Source fix applied; CI confirmation pending.

## Failure

The supplied GitHub Actions output failed `:app:compileDebugKotlin` in `AppNavHost.kt`: `NavigationBarItem` is a `RowScope` API and could not be called from inside the nested `Box` used to anchor the More dropdown.

## Fix

All four `NavigationBarItem` calls are now direct children of `NavigationBar`. Tapping More opens a `ModalBottomSheet` with the existing secondary destinations.

## Verification

`git diff --check` passed. A local `:app:compileDebugKotlin` attempt stopped before task dependency resolution because this environment has no configured Android SDK (`ANDROID_HOME` or `local.properties` `sdk.dir`). The fix has not yet been confirmed by GitHub Actions; no APK build is claimed.
