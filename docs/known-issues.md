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
