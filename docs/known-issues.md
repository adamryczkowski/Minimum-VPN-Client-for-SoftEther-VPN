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
- **Status:** Fixed
- **Discovered:** 2026-01-03
- **Fixed:** 2026-01-03
- **Description:** Setting namespace via package attribute in AndroidManifest.xml is no longer supported
- **Location:** `app/src/main/AndroidManifest.xml`
- **Resolution:** Removed `package="kittoku.mvc"` from AndroidManifest.xml (namespace is now set in build.gradle)

---

## Technical Debt

### Detekt Static Analysis Baseline

- **Severity:** Medium
- **Status:** Open (Baselined)
- **Discovered:** 2026-01-03
- **Description:** Detekt static analysis found 166 weighted issues in the codebase. These have been baselined to allow the build to pass while tracking technical debt for incremental improvement.
- **Location:** `app/detekt-baseline.xml`
- **Technical Debt Summary:**
  - **Total Issues:** 166 weighted issues
  - **Estimated Effort:** 1 day 1 hour 25 minutes
  - **Breakdown by Category:**
    - **Style (12h 40min):** Formatting, naming conventions, code style issues
    - **Exceptions (5h):** Exception handling patterns (SwallowedException, TooGenericExceptionCaught)
    - **Complexity (4h 20min):** Complex methods, high cyclomatic complexity
    - **Naming (2h 25min):** Variable/function naming conventions
    - **Performance (35min):** Performance-related issues (SpreadOperator)
    - **Potential Bugs (25min):** Code patterns that could lead to bugs
- **Files with Most Issues:**
  - `ControlClient.kt` - Complex VPN control logic
  - `IPTerminal.kt` - IP packet processing
  - Various preference classes - Exception handling
- **Resolution Strategy:**
  1. Address issues incrementally during regular development
  2. Prioritize potential-bugs and exceptions categories first
  3. Use `just detekt-report` to generate detailed HTML report
  4. New code must not introduce new detekt issues (baseline only covers existing code)
- **Commands:**
  - `just lint-detekt` - Run detekt with baseline
  - `just detekt-report` - Generate HTML report at `app/build/reports/detekt/`
  - `just detekt-baseline` - Regenerate baseline (only if intentionally adding issues)

---

## Runtime Issues

### Missing POST_NOTIFICATIONS Permission (Android 13+)

- **Severity:** Medium
- **Status:** Open
- **Discovered:** 2026-01-05
- **Description:** The app does not request POST_NOTIFICATIONS permission on Android 13+, which means error notifications from failed VPN connections are not displayed to the user.
- **Location:** `app/src/main/AndroidManifest.xml`
- **Steps to Reproduce:**
  1. Install app on Android 13+ device/emulator
  2. Attempt to connect to VPN
  3. If connection fails, no error notification is shown
- **Expected Behavior:** Error notification should be displayed when VPN connection fails
- **Actual Behavior:** No notification appears; user only sees the switch bounce back
- **Workaround:** Check logcat for error messages
- **Resolution:** Add `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>` to AndroidManifest.xml and request permission at runtime

### Hostname Field Accepts Port in Value

- **Severity:** Low
- **Status:** Open
- **Discovered:** 2026-01-05
- **Description:** The hostname preference field accepts values like "hostname:port" but the code expects hostname and port to be separate settings. Entering "172.104.148.166:992" in the hostname field causes DNS resolution to fail.
- **Location:** `app/src/main/java/kittoku/mvc/preference/custom/NonEmptyStringPreference.kt`
- **Steps to Reproduce:**
  1. Enter "172.104.148.166:992" in the hostname field
  2. Attempt to connect
  3. Connection fails immediately
- **Expected Behavior:** Either parse the port from the hostname, or validate that hostname doesn't contain ":"
- **Actual Behavior:** The full string including ":992" is passed to DNS resolution, which fails
- **Workaround:** Enter only the hostname/IP without port, and set port separately in SSL settings

### VPN Connection Fails Silently

- **Severity:** High
- **Status:** Open (Under Investigation)
- **Discovered:** 2026-01-05
- **Description:** VPN connection attempts fail without clear error feedback to the user. The connect switch bounces back immediately after being turned on.
- **Location:** `app/src/main/java/kittoku/mvc/service/client/ControlClient.kt`
- **Steps to Reproduce:**
  1. Configure VPN settings (hostname, hub, username, password)
  2. Click the Connect switch
  3. Switch bounces back to OFF position
- **Expected Behavior:** Either successful connection or clear error message
- **Actual Behavior:** Switch bounces back with no visible error (due to missing notification permission)
- **Investigation Notes:**
  - Network connectivity to VPN server is confirmed (ping and port 992 accessible)
  - VPN service starts successfully (foreground service allowed)
  - Exception is caught by CoroutineExceptionHandler which calls kill()
  - kill() sets HOME_CONNECTOR to false, causing switch to bounce back
  - Error notification is not shown due to missing POST_NOTIFICATIONS permission
- **Workaround:** Enable logging in settings and check log files, or use logcat to see error messages
- **Next Steps:** Enable POST_NOTIFICATIONS permission to see error messages, then debug the specific protocol failure
