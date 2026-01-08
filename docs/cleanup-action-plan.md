# Cleanup Action Plan

**Date:** 8th January 2025
**Based on:** [cleanup-findings.md](./cleanup-findings.md)

---

## Overview

This action plan addresses the issues identified in the code audit, organized into milestones that can be implemented, tested, and validated independently. Each milestone follows test-driven development (TDD) principles with corresponding unit tests for every change.

---

## Milestone 1: Consolidate Duplicate Types (CRITICAL)

**Goal:** Eliminate duplicate `ConnectionState` and `ConnectionStats` classes to establish a single source of truth.

**Estimated Effort:** 2-3 days

### 1.1 Consolidate `ConnectionState` Classes

**Current State:**

- [`kittoku.mvc.connection.ConnectionState`](../app/src/main/java/kittoku/mvc/connection/ConnectionState.kt:9) - More feature-rich with `displayName`, `isConnected`, `isConnecting` properties
- [`kittoku.mvc.service.contract.ConnectionState`](../app/src/main/java/kittoku/mvc/service/contract/IVpnConnection.kt:6) - Simpler version

**Decision:** Keep `kittoku.mvc.connection.ConnectionState` as the canonical version because it has more features and better documentation.

**Changes Required:**

| File | Change |
|------|--------|
| [`IVpnConnection.kt`](../app/src/main/java/kittoku/mvc/service/contract/IVpnConnection.kt) | Remove `ConnectionState` sealed class (lines 6-45), add import for `kittoku.mvc.connection.ConnectionState` |
| [`HomeScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt) | Update import from `kittoku.mvc.service.contract.ConnectionState` to `kittoku.mvc.connection.ConnectionState` |
| [`HomeViewModel.kt`](../app/src/main/java/kittoku/mvc/viewmodel/HomeViewModel.kt) | Update import |
| [`VpnConnectionRepository.kt`](../app/src/main/java/kittoku/mvc/repository/VpnConnectionRepository.kt) | Update import |
| [`VpnConnectionManager.kt`](../app/src/main/java/kittoku/mvc/service/VpnConnectionManager.kt) | Update import |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| [`ConnectionStateTest.kt`](../app/src/test/java/kittoku/mvc/connection/ConnectionStateTest.kt) | Verify all state properties, transitions, and display names |
| New: `ConnectionStateCompatibilityTest.kt` | Verify backward compatibility with all consumers |

**Separation of Concerns:**

- `kittoku.mvc.connection` package owns all connection state types
- `kittoku.mvc.service.contract` package defines interfaces only, imports types from `connection`

### 1.2 Consolidate `ConnectionStats` Classes

**Current State:**

- [`kittoku.mvc.connection.ConnectionStats`](../app/src/main/java/kittoku/mvc/connection/ConnectionStats.kt:12) - Feature-rich with formatting methods, duration calculation
- [`kittoku.mvc.service.contract.ConnectionStats`](../app/src/main/java/kittoku/mvc/service/contract/IVpnConnection.kt:50) - Simple data holder

**Decision:** Keep `kittoku.mvc.connection.ConnectionStats` as the canonical version.

**Changes Required:**

| File | Change |
|------|--------|
| [`IVpnConnection.kt`](../app/src/main/java/kittoku/mvc/service/contract/IVpnConnection.kt) | Remove `ConnectionStats` data class (lines 50-79), add import for `kittoku.mvc.connection.ConnectionStats` |
| All files using `kittoku.mvc.service.contract.ConnectionStats` | Update imports |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| [`ConnectionStatsTest.kt`](../app/src/test/java/kittoku/mvc/connection/ConnectionStatsTest.kt) | Verify all properties, formatting methods, duration calculation |

**Validation Criteria:**

- [ ] All tests pass after consolidation
- [ ] No duplicate type definitions remain
- [ ] All imports updated correctly
- [ ] `just validate` passes

---

## Milestone 2: Extract Shared Utility Functions (HIGH)

**Goal:** Eliminate duplicate `formatBytes()` and `formatDuration()` functions by extracting them to a shared utility.

**Estimated Effort:** 1-2 days

### 2.1 Create Formatting Utility Object

**New File:** `app/src/main/java/kittoku/mvc/extension/Formatting.kt`

```kotlin
package kittoku.mvc.extension

import java.util.Locale

/**
 * Utility object for formatting values for display.
 */
object Formatting {
    /**
     * Format bytes into a human-readable string.
     *
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "1.5 MB", "2.3 GB")
     */
    fun formatBytes(bytes: Long): String { ... }

    /**
     * Format duration in milliseconds into HH:MM:SS format.
     *
     * @param durationMs Duration in milliseconds
     * @return Formatted string (e.g., "01:23:45")
     */
    fun formatDuration(durationMs: Long): String { ... }
}
```

### 2.2 Remove Duplicate Functions

**Files to Update:**

| File | Line | Change |
|------|------|--------|
| [`PerformanceMonitor.kt`](../app/src/main/java/kittoku/mvc/performance/PerformanceMonitor.kt) | 246 | Remove `formatBytes()`, use `Formatting.formatBytes()` |
| [`AggregateStatistics.kt`](../app/src/main/java/kittoku/mvc/statistics/AggregateStatistics.kt) | 84, 69 | Remove both functions, use `Formatting` |
| [`ConnectionSession.kt`](../app/src/main/java/kittoku/mvc/statistics/ConnectionSession.kt) | 130, 68 | Remove both functions, use `Formatting` |
| [`HomeScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt) | 372, 380 | Remove both functions, use `Formatting` |
| [`ConnectionStats.kt`](../app/src/main/java/kittoku/mvc/connection/ConnectionStats.kt) | 78, 49 | Remove private `formatBytes()`, use `Formatting` |
| [`StatisticsViewModel.kt`](../app/src/main/java/kittoku/mvc/viewmodel/StatisticsViewModel.kt) | 167, 159 | Remove both functions, use `Formatting` |
| [`ConnectionTimer.kt`](../app/src/main/java/kittoku/mvc/statistics/ConnectionTimer.kt) | 119 | Remove `formatDuration()`, use `Formatting` |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| New: `FormattingTest.kt` | Test `formatBytes()` with edge cases: 0, 1, 1023, 1024, 1MB, 1GB, negative values |
| New: `FormattingTest.kt` | Test `formatDuration()` with edge cases: 0, 59s, 1h, 24h, negative values |

**Separation of Concerns:**

- `kittoku.mvc.extension` package owns all utility/extension functions
- Domain classes delegate formatting to the utility

**Validation Criteria:**

- [ ] All 12 duplicate function locations updated
- [ ] New `FormattingTest.kt` passes with 100% coverage of utility
- [ ] Existing tests continue to pass
- [ ] `just validate` passes

---

## Milestone 3: Consolidate Notification Constants (LOW)

**Goal:** Eliminate duplicate notification channel constants.

**Estimated Effort:** 0.5 days

### 3.1 Consolidate Constants

**Current State:**

- [`service/constant.kt`](../app/src/main/java/kittoku/mvc/service/constant.kt:6-7) - `CHANNEL_ID`, `CHANNEL_NAME`
- [`VpnNotificationManager.kt`](../app/src/main/java/kittoku/mvc/notification/VpnNotificationManager.kt:26-28) - `CHANNEL_ID`, `CHANNEL_NAME`, `CHANNEL_DESCRIPTION`

**Decision:** Keep constants in `VpnNotificationManager` companion object as the canonical source since it's the notification-focused class.

**Changes Required:**

| File | Change |
|------|--------|
| [`constant.kt`](../app/src/main/java/kittoku/mvc/service/constant.kt) | Remove `CHANNEL_ID` and `CHANNEL_NAME` |
| [`SoftEtherVpnService.kt`](../app/src/main/java/kittoku/mvc/service/SoftEtherVpnService.kt) | Import `CHANNEL_ID` from `VpnNotificationManager` |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| [`VpnNotificationContentTest.kt`](../app/src/test/java/kittoku/mvc/notification/VpnNotificationContentTest.kt) | Verify constants are accessible |

**Separation of Concerns:**

- `kittoku.mvc.notification` package owns all notification-related constants
- `kittoku.mvc.service` package imports from notification when needed

---

## Milestone 4: Refactor SoftEtherVpnService (MEDIUM)

**Goal:** Extract notification handling from `SoftEtherVpnService` to use `VpnNotificationManager`.

**Estimated Effort:** 1-2 days

### 4.1 Integrate VpnNotificationManager

**Current State:** [`SoftEtherVpnService.beForegrounded()`](../app/src/main/java/kittoku/mvc/service/SoftEtherVpnService.kt:78) creates notifications directly.

**Changes Required:**

| File | Change |
|------|--------|
| [`SoftEtherVpnService.kt`](../app/src/main/java/kittoku/mvc/service/SoftEtherVpnService.kt) | Inject or create `VpnNotificationManager`, use it in `beForegrounded()` |
| [`VpnNotificationManager.kt`](../app/src/main/java/kittoku/mvc/notification/VpnNotificationManager.kt) | Add method `buildForegroundNotification()` for service use |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| New: `SoftEtherVpnServiceTest.kt` | Verify notification manager is used correctly |
| Update: `VpnNotificationContentTest.kt` | Test foreground notification content |

**Separation of Concerns:**

- `VpnNotificationManager` owns all notification creation logic
- `SoftEtherVpnService` delegates notification work to the manager

---

## Milestone 5: Remove Unused Koin Modules (MEDIUM)

**Goal:** Clean up empty Koin modules that add no value.

**Estimated Effort:** 0.5 days

### 5.1 Remove Empty Modules

**Changes Required:**

| File | Change |
|------|--------|
| [`AppModule.kt`](../app/src/main/java/kittoku/mvc/di/AppModule.kt) | Remove `networkModule` (lines 157-163) |
| [`AppModule.kt`](../app/src/main/java/kittoku/mvc/di/AppModule.kt) | Remove `protocolModule` (lines 170-176) |
| [`AppModule.kt`](../app/src/main/java/kittoku/mvc/di/AppModule.kt) | Update `allModules` to only contain `appModule` or remove entirely |

**Alternative:** If these modules are planned for future use, add TODO comments with issue references.

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| New: `AppModuleTest.kt` | Verify Koin module loads correctly |

---

## Milestone 6: Address Unused Utility Classes (MEDIUM)

**Goal:** Either integrate or remove unused utility classes.

**Estimated Effort:** 2-3 days

### 6.1 Evaluate Each Unused Class

| Class | Recommendation | Rationale |
|-------|----------------|-----------|
| [`BatteryOptimizer`](../app/src/main/java/kittoku/mvc/performance/BatteryOptimizer.kt) | **Remove** or **Integrate** | Evaluate if battery optimization is needed |
| [`SecureCredentialStorage`](../app/src/main/java/kittoku/mvc/security/SecureCredentialStorage.kt) | **Integrate** | Security feature should be used for credential storage |
| [`DiagnosticExporter`](../app/src/main/java/kittoku/mvc/logging/DiagnosticExporter.kt) | **Integrate** | Useful for debugging, integrate with VpnLogger |
| [`CrashHandler`](../app/src/main/java/kittoku/mvc/logging/CrashHandler.kt) | **Integrate** | Important for production stability |
| [`PerformanceMonitor`](../app/src/main/java/kittoku/mvc/performance/PerformanceMonitor.kt) | **Remove** or **Integrate** | Evaluate if performance monitoring is needed |

### 6.2 Integration Plan for SecureCredentialStorage

**Changes Required:**

| File | Change |
|------|--------|
| [`AppModule.kt`](../app/src/main/java/kittoku/mvc/di/AppModule.kt) | Add `SecureCredentialStorage` to DI |
| Preference accessors | Use `SecureCredentialStorage` for password storage |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| [`SecureCredentialStorageTest.kt`](../app/src/test/java/kittoku/mvc/security/SecureCredentialStorageTest.kt) | Already exists, verify coverage |

### 6.3 Integration Plan for DiagnosticExporter

**Changes Required:**

| File | Change |
|------|--------|
| [`VpnLogger.kt`](../app/src/main/java/kittoku/mvc/logging/VpnLogger.kt) | Delegate export functionality to `DiagnosticExporter` |
| [`AppModule.kt`](../app/src/main/java/kittoku/mvc/di/AppModule.kt) | Add `DiagnosticExporter` to DI |

---

## Milestone 7: Decide on UI Architecture (HIGH)

**Goal:** Choose between Fragment-based and Compose-based UI and remove the unused implementation.

**Estimated Effort:** 3-5 days (depending on decision)

### 7.1 Current State Analysis

**Fragment-based UI (Currently Active):**

- [`HomeFragment`](../app/src/main/java/kittoku/mvc/fragment/HomeFragment.kt)
- [`SettingFragment`](../app/src/main/java/kittoku/mvc/fragment/SettingFragment.kt)
- [`AboutFragment`](../app/src/main/java/kittoku/mvc/fragment/AboutFragment.kt)
- [`ProfileListFragment`](../app/src/main/java/kittoku/mvc/fragment/ProfileListFragment.kt)
- [`SplitTunnelFragment`](../app/src/main/java/kittoku/mvc/fragment/SplitTunnelFragment.kt)

**Compose-based UI (Not Integrated):**

- [`HomeScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt)
- [`SettingsScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/SettingsScreen.kt)
- [`AboutScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/AboutScreen.kt)
- [`AppNavigation.kt`](../app/src/main/java/kittoku/mvc/ui/navigation/AppNavigation.kt)
- Theme files in `ui/theme/`

### 7.2 Decision Required

**Option A: Keep Fragment-based UI (Recommended for stability)**

- Remove entire `ui/` package
- Remove Compose dependencies from `build.gradle`
- Simpler, less risk

**Option B: Migrate to Compose**

- Complete the Compose implementation
- Migrate `MainActivity` to use Compose navigation
- Remove Fragment-based UI
- More modern, but higher effort

### 7.3 If Option A (Remove Compose UI)

**Files to Remove:**

- `app/src/main/java/kittoku/mvc/ui/` (entire directory)

**Files to Update:**

- `app/build.gradle` - Remove Compose dependencies

### 7.4 If Option B (Complete Compose Migration)

This would require a separate detailed migration plan.

---

## Milestone 8: Consolidate State Management (MEDIUM) ✅ COMPLETED

**Goal:** Establish a single source of truth for connection state.

**Estimated Effort:** 2-3 days

**Status:** Completed on 8th January 2025

### 8.1 Current State Analysis

**Multiple State Sources (Before):**

1. [`ConnectionStateManager`](../app/src/main/java/kittoku/mvc/connection/ConnectionStateManager.kt) - Uses `kittoku.mvc.connection.ConnectionState`
2. [`VpnConnectionRepository`](../app/src/main/java/kittoku/mvc/repository/VpnConnectionRepository.kt) - Had its own `MutableStateFlow` and listened to `HOME_CONNECTOR` preference
3. SharedPreferences via `HOME_CONNECTOR` preference

### 8.2 Consolidation Plan

**Decision:** Use `ConnectionStateManager` as the single source of truth.

**Changes Made:**

| File | Change |
|------|--------|
| [`VpnConnectionRepository.kt`](../app/src/main/java/kittoku/mvc/repository/VpnConnectionRepository.kt) | Now delegates to `ConnectionStateManager` for all state management. The repository exposes `ConnectionStateManager.state` directly as `connectionState`. SharedPreferences (`HOME_CONNECTOR`) is now synchronized from the state manager, not the other way around. |
| [`AppModule.kt`](../app/src/main/java/kittoku/mvc/di/AppModule.kt) | Updated to inject `ConnectionStateManager` into `VpnConnectionRepository` |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| [`ConnectionStateManagerTest.kt`](../app/src/test/java/kittoku/mvc/connection/ConnectionStateManagerTest.kt) | Added "Single source of truth behavior" test nested class |
| [`VpnConnectionRepositoryTest.kt`](../app/src/test/java/kittoku/mvc/repository/VpnConnectionRepositoryTest.kt) | New test file verifying delegation works correctly |

**Validation Criteria:**

- [x] `VpnConnectionRepository` delegates to `ConnectionStateManager`
- [x] `ConnectionStateManager` is the single source of truth
- [x] SharedPreferences (`HOME_CONNECTOR`) is synchronized from state manager
- [x] Unit tests verify delegation behavior
- [x] Unit tests verify single source of truth behavior

---

## Milestone 9: Review JUnit 4 Dependency (LOW)

**Goal:** Remove unnecessary JUnit 4 dependency if not used.

**Estimated Effort:** 0.5 days

### 9.1 Verify JUnit 4 Usage

**Command to Run:**

```bash
rg "import org.junit.Test" app/src/test --type kotlin
rg "import org.junit.Before" app/src/test --type kotlin
rg "@RunWith" app/src/test --type kotlin
```

### 9.2 If No JUnit 4 Tests Found

**Changes Required:**

| File | Change |
|------|--------|
| `app/build.gradle` | Remove `testImplementation 'junit:junit:4.13.2'` |
| `app/build.gradle` | Remove `testRuntimeOnly "org.junit.vintage:junit-vintage-engine:${versions.junit5}"` |

---

## Milestone 10: Extract Log Export from VpnLogger (LOW)

**Goal:** Improve separation of concerns in logging module.

**Estimated Effort:** 1 day

### 10.1 Current State

[`VpnLogger`](../app/src/main/java/kittoku/mvc/logging/VpnLogger.kt) handles:

- Logging to Android Log
- Log buffering
- Log export (JSON and text)
- Global context management
- Listener management

### 10.2 Refactoring Plan

**Changes Required:**

| File | Change |
|------|--------|
| [`VpnLogger.kt`](../app/src/main/java/kittoku/mvc/logging/VpnLogger.kt) | Remove export methods, delegate to `DiagnosticExporter` |
| [`DiagnosticExporter.kt`](../app/src/main/java/kittoku/mvc/logging/DiagnosticExporter.kt) | Add methods to export from `VpnLogger` buffer |

**Unit Tests:**

| Test File | Test Cases |
|-----------|------------|
| [`VpnLoggerTest.kt`](../app/src/test/java/kittoku/mvc/logging/VpnLoggerTest.kt) | Update to verify delegation |
| New: `DiagnosticExporterTest.kt` | Test export functionality |

---

## Implementation Order

The milestones should be implemented in the following order to minimize conflicts and maximize value:

| Order | Milestone | Priority | Dependencies |
|-------|-----------|----------|--------------|
| 1 | Milestone 1: Consolidate Duplicate Types | CRITICAL | None |
| 2 | Milestone 2: Extract Shared Utilities | HIGH | Milestone 1 |
| 3 | Milestone 3: Consolidate Notification Constants | LOW | None |
| 4 | Milestone 4: Refactor SoftEtherVpnService | MEDIUM | Milestone 3 |
| 5 | Milestone 5: Remove Unused Koin Modules | MEDIUM | None |
| 6 | Milestone 7: Decide on UI Architecture | HIGH | Milestone 1 |
| 7 | Milestone 8: Consolidate State Management | MEDIUM | Milestones 1, 7 |
| 8 | Milestone 6: Address Unused Utility Classes | MEDIUM | Milestone 2 |
| 9 | Milestone 9: Review JUnit 4 Dependency | LOW | None |
| 10 | Milestone 10: Extract Log Export | LOW | Milestone 6 |

---

## Validation Checklist

After completing all milestones:

- [ ] All unit tests pass (`./gradlew test`)
- [ ] All instrumented tests pass (`./gradlew connectedAndroidTest`)
- [ ] `just validate` passes
- [ ] No duplicate classes remain
- [ ] No duplicate utility functions remain
- [ ] Single source of truth for connection state
- [ ] Clean separation of concerns across packages
- [ ] All unused code removed or integrated

---

## Appendix: Package Responsibility Matrix

| Package | Responsibility |
|---------|----------------|
| `kittoku.mvc.connection` | Connection state types, state management |
| `kittoku.mvc.service` | VPN service lifecycle, service contracts |
| `kittoku.mvc.service.contract` | Interfaces only (no implementations) |
| `kittoku.mvc.notification` | All notification-related code and constants |
| `kittoku.mvc.extension` | Utility functions and Kotlin extensions |
| `kittoku.mvc.repository` | Data access layer |
| `kittoku.mvc.viewmodel` | UI state management (MVVM) |
| `kittoku.mvc.fragment` | Fragment-based UI (if kept) |
| `kittoku.mvc.ui` | Compose-based UI (if kept) |
| `kittoku.mvc.logging` | Logging infrastructure |
| `kittoku.mvc.security` | Security utilities |
| `kittoku.mvc.performance` | Performance monitoring |
| `kittoku.mvc.statistics` | Connection statistics |
| `kittoku.mvc.di` | Dependency injection configuration |
