# Known Issues

This document tracks known issues discovered during testing.

## Format

### Issue Title
- **Severity:** Critical/High/Medium/Low
- **Status:** Open/In Progress/Fixed
- **Discovered:** Date
- **Description:** Brief description
- **Steps to Reproduce:**
  1. Step 1
  2. Step 2
- **Expected Behavior:** What should happen
- **Actual Behavior:** What actually happens
- **Workaround:** If any

---

## Issues

### Deprecated startActivityForResult API
- **Severity:** Low
- **Status:** Open
- **Discovered:** 2026-01-03
- **Description:** HomeFragment.kt uses deprecated `startActivityForResult` API
- **Location:** `app/src/main/java/kittoku/mvc/fragment/HomeFragment.kt:44`
- **Workaround:** None needed currently, but should migrate to Activity Result API

### Deprecated stopForeground(Boolean) API
- **Severity:** Low
- **Status:** Open
- **Discovered:** 2026-01-03
- **Description:** ControlClient.kt uses deprecated `stopForeground(Boolean)` method
- **Location:** `app/src/main/java/kittoku/mvc/service/client/ControlClient.kt:313`
- **Workaround:** None needed currently, but should migrate to `stopForeground(int)` with STOP_FOREGROUND_REMOVE

### Java Type Mismatch in NetworkObserver
- **Severity:** Low
- **Status:** Open
- **Discovered:** 2026-01-03
- **Description:** Inferred type is 'String?' but 'String' was expected
- **Location:** `app/src/main/java/kittoku/mvc/service/client/stateless/NetworkObserver.kt:39`
- **Workaround:** None needed currently

### Instance Check Always False (Kotlin 2.4 Breaking Change)
- **Severity:** Medium
- **Status:** Open
- **Discovered:** 2026-01-03
- **Description:** Check for instance is always 'false'. This will become an error in Kotlin 2.4
- **Location:** `app/src/main/java/kittoku/mvc/service/teminal/tcp/TCPTerminal.kt:179`
- **Workaround:** Must be fixed before upgrading to Kotlin 2.4
- **Reference:** https://youtrack.jetbrains.com/issue/KTLC-365

### Package Attribute in AndroidManifest.xml (Deprecated)
- **Severity:** Low
- **Status:** Open
- **Discovered:** 2026-01-03
- **Description:** Setting namespace via package attribute in AndroidManifest.xml is no longer supported
- **Location:** `app/src/main/AndroidManifest.xml`
- **Workaround:** Remove `package="kittoku.mvc"` from AndroidManifest.xml (namespace is now set in build.gradle)
