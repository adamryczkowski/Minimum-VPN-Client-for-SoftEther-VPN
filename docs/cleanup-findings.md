# Project Cleanup Findings

**Date:** 8th January 2025
**Auditor:** Automated Code Audit

## Executive Summary

This document presents the findings from a comprehensive audit of the SoftEther Connect Android VPN client project. The audit identified several categories of issues including duplicate code, unused classes, architectural inconsistencies, and violations of the Single Responsibility Principle.

---

## 1. Duplicate Classes and Types

### 1.1 Duplicate `ConnectionState` Classes (CRITICAL)

**Issue:** Two separate `ConnectionState` sealed classes exist with nearly identical functionality.

| Location | Package |
|----------|---------|
| [`ConnectionState`](../app/src/main/java/kittoku/mvc/connection/ConnectionState.kt:9) | `kittoku.mvc.connection` |
| [`ConnectionState`](../app/src/main/java/kittoku/mvc/service/contract/IVpnConnection.kt:6) | `kittoku.mvc.service.contract` |

**Usage:**

- `kittoku.mvc.connection.ConnectionState` is used by:
  - [`VpnNotificationManager`](../app/src/main/java/kittoku/mvc/notification/VpnNotificationManager.kt:13)
  - [`VpnNotificationContent`](../app/src/main/java/kittoku/mvc/notification/VpnNotificationContent.kt:3)
  - [`VpnTileService`](../app/src/main/java/kittoku/mvc/tile/VpnTileService.kt:9)
  - [`VpnTileState`](../app/src/main/java/kittoku/mvc/tile/VpnTileState.kt:3)
  - [`ConnectionStateManager`](../app/src/main/java/kittoku/mvc/connection/ConnectionStateManager.kt:14)

- `kittoku.mvc.service.contract.ConnectionState` is used by:
  - [`HomeScreen`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt:43)
  - [`HomeViewModel`](../app/src/main/java/kittoku/mvc/viewmodel/HomeViewModel.kt:8)
  - [`VpnConnectionRepository`](../app/src/main/java/kittoku/mvc/repository/VpnConnectionRepository.kt:9)
  - [`VpnConnectionManager`](../app/src/main/java/kittoku/mvc/service/VpnConnectionManager.kt:7)

**Recommendation:** Consolidate into a single `ConnectionState` class in `kittoku.mvc.connection` package and update all imports.

### 1.2 Duplicate `ConnectionStats` Classes (CRITICAL)

**Issue:** Two separate `ConnectionStats` data classes exist.

| Location | Package |
|----------|---------|
| [`ConnectionStats`](../app/src/main/java/kittoku/mvc/connection/ConnectionStats.kt:12) | `kittoku.mvc.connection` |
| [`ConnectionStats`](../app/src/main/java/kittoku/mvc/service/contract/IVpnConnection.kt:50) | `kittoku.mvc.service.contract` |

**Recommendation:** Consolidate into a single class, preferring the more feature-rich version in `kittoku.mvc.connection`.

---

## 2. Duplicate Utility Functions

### 2.1 `formatBytes()` Function (HIGH)

**Issue:** The `formatBytes()` function is duplicated in 6 different locations:

| File | Line |
|------|------|
| [`PerformanceMonitor.kt`](../app/src/main/java/kittoku/mvc/performance/PerformanceMonitor.kt:246) | 246 |
| [`AggregateStatistics.kt`](../app/src/main/java/kittoku/mvc/statistics/AggregateStatistics.kt:84) | 84 |
| [`ConnectionSession.kt`](../app/src/main/java/kittoku/mvc/statistics/ConnectionSession.kt:130) | 130 |
| [`HomeScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt:372) | 372 |
| [`ConnectionStats.kt`](../app/src/main/java/kittoku/mvc/connection/ConnectionStats.kt:78) | 78 |
| [`StatisticsViewModel.kt`](../app/src/main/java/kittoku/mvc/viewmodel/StatisticsViewModel.kt:167) | 167 |

**Recommendation:** Extract to a utility object in `kittoku.mvc.extension` package:

```kotlin
// app/src/main/java/kittoku/mvc/extension/Formatting.kt
object Formatting {
    fun formatBytes(bytes: Long): String { ... }
    fun formatDuration(durationMs: Long): String { ... }
}
```

### 2.2 `formatDuration()` / `getFormattedDuration()` Functions (HIGH)

**Issue:** Duration formatting is duplicated in 6 different locations:

| File | Line |
|------|------|
| [`HomeScreen.kt`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt:380) | 380 |
| [`ConnectionSession.kt`](../app/src/main/java/kittoku/mvc/statistics/ConnectionSession.kt:68) | 68 |
| [`ConnectionStats.kt`](../app/src/main/java/kittoku/mvc/connection/ConnectionStats.kt:49) | 49 |
| [`AggregateStatistics.kt`](../app/src/main/java/kittoku/mvc/statistics/AggregateStatistics.kt:69) | 69 |
| [`ConnectionTimer.kt`](../app/src/main/java/kittoku/mvc/statistics/ConnectionTimer.kt:119) | 119 |
| [`StatisticsViewModel.kt`](../app/src/main/java/kittoku/mvc/viewmodel/StatisticsViewModel.kt:159) | 159 |

**Recommendation:** Consolidate into the same utility object as `formatBytes()`.

---

## 3. Unused Classes and Modules

### 3.1 Unused Koin Modules (MEDIUM)

**Issue:** Empty Koin modules defined but never used.

| Module | Location |
|--------|----------|
| `networkModule` | [`AppModule.kt:157`](../app/src/main/java/kittoku/mvc/di/AppModule.kt:157) |
| `protocolModule` | [`AppModule.kt:170`](../app/src/main/java/kittoku/mvc/di/AppModule.kt:170) |
| `allModules` | [`AppModule.kt:183`](../app/src/main/java/kittoku/mvc/di/AppModule.kt:183) |

**Recommendation:** Remove these empty modules or implement them if planned for future use.

### 3.2 Unused Utility Classes (MEDIUM)

The following classes are defined but never instantiated or used:

| Class | Location | Purpose |
|-------|----------|---------|
| [`BatteryOptimizer`](../app/src/main/java/kittoku/mvc/performance/BatteryOptimizer.kt:28) | `performance/` | Battery optimization for VPN |
| [`SecureCredentialStorage`](../app/src/main/java/kittoku/mvc/security/SecureCredentialStorage.kt:34) | `security/` | Encrypted credential storage |
| [`DiagnosticExporter`](../app/src/main/java/kittoku/mvc/logging/DiagnosticExporter.kt:29) | `logging/` | Export diagnostic data |
| [`CrashHandler`](../app/src/main/java/kittoku/mvc/logging/CrashHandler.kt:24) | `logging/` | Crash handling |
| [`PerformanceMonitor`](../app/src/main/java/kittoku/mvc/performance/PerformanceMonitor.kt:25) | `performance/` | Performance monitoring |

**Recommendation:** Either integrate these classes into the application or remove them if not needed.

### 3.3 Unused Compose UI Components (MEDIUM)

**Issue:** Complete Compose UI layer exists but is not integrated with the app.

| Component | Location |
|-----------|----------|
| [`AppNavigation`](../app/src/main/java/kittoku/mvc/ui/navigation/AppNavigation.kt:48) | `ui/navigation/` |
| [`HomeScreen`](../app/src/main/java/kittoku/mvc/ui/screen/HomeScreen.kt:52) | `ui/screen/` |
| [`SettingsScreen`](../app/src/main/java/kittoku/mvc/ui/screen/SettingsScreen.kt:31) | `ui/screen/` |
| [`AboutScreen`](../app/src/main/java/kittoku/mvc/ui/screen/AboutScreen.kt:38) | `ui/screen/` |
| Theme files in `ui/theme/` | Color.kt, Theme.kt, Type.kt |

**Current State:** The app uses Fragment-based UI via [`MainActivity`](../app/src/main/java/kittoku/mvc/MainActivity.kt:13) with `HomeFragment`, `SettingFragment`, and `AboutFragment`.

**Recommendation:** Either:

1. Complete migration to Compose and remove Fragment-based UI, OR
2. Remove unused Compose components if Fragment-based UI is preferred

---

## 4. Duplicate Constants

### 4.1 Notification Channel Constants (LOW)

**Issue:** Notification channel constants are defined in two places:

| Location | Constants |
|----------|-----------|
| [`service/constant.kt`](../app/src/main/java/kittoku/mvc/service/constant.kt:6-7) | `CHANNEL_ID`, `CHANNEL_NAME` |
| [`VpnNotificationManager`](../app/src/main/java/kittoku/mvc/notification/VpnNotificationManager.kt:26-28) | `CHANNEL_ID`, `CHANNEL_NAME`, `CHANNEL_DESCRIPTION` |

**Recommendation:** Consolidate notification constants in one location.

---

## 5. Architectural Issues

### 5.1 Mixed UI Architecture (HIGH)

**Issue:** The project has two parallel UI implementations:

1. **Fragment-based UI** (currently active):
   - [`HomeFragment`](../app/src/main/java/kittoku/mvc/fragment/HomeFragment.kt:17)
   - [`SettingFragment`](../app/src/main/java/kittoku/mvc/fragment/SettingFragment.kt:14)
   - [`AboutFragment`](../app/src/main/java/kittoku/mvc/fragment/AboutFragment.kt:7)
   - [`ProfileListFragment`](../app/src/main/java/kittoku/mvc/fragment/ProfileListFragment.kt:32)
   - [`SplitTunnelFragment`](../app/src/main/java/kittoku/mvc/fragment/SplitTunnelFragment.kt:32)

2. **Compose-based UI** (not integrated):
   - `ui/screen/` package with HomeScreen, SettingsScreen, AboutScreen
   - `ui/navigation/AppNavigation.kt`
   - `ui/theme/` package

**Recommendation:** Choose one UI framework and remove the other to reduce maintenance burden and confusion.

### 5.2 Inconsistent State Management (MEDIUM)

**Issue:** Connection state is managed through multiple mechanisms:

1. [`ConnectionStateManager`](../app/src/main/java/kittoku/mvc/connection/ConnectionStateManager.kt:13) - uses `kittoku.mvc.connection.ConnectionState`
2. [`VpnConnectionRepository`](../app/src/main/java/kittoku/mvc/repository/VpnConnectionRepository.kt:23) - uses `kittoku.mvc.service.contract.ConnectionState`
3. SharedPreferences via `HOME_CONNECTOR` preference

**Recommendation:** Consolidate state management into a single source of truth.

---

## 6. Single Responsibility Principle Violations

### 6.1 [`SoftEtherVpnService`](../app/src/main/java/kittoku/mvc/service/SoftEtherVpnService.kt:27) (MEDIUM)

**Issue:** This class handles:

- VPN service lifecycle
- Notification creation and management
- Split tunnel configuration creation
- Client bridge creation

**Recommendation:** Extract notification handling to use the existing [`VpnNotificationManager`](../app/src/main/java/kittoku/mvc/notification/VpnNotificationManager.kt:21).

### 6.2 [`VpnLogger`](../app/src/main/java/kittoku/mvc/logging/VpnLogger.kt:34) (LOW)

**Issue:** The class handles:

- Logging to Android Log
- Log buffering
- Log export (JSON and text)
- Global context management
- Listener management

**Recommendation:** Consider extracting log export functionality to [`DiagnosticExporter`](../app/src/main/java/kittoku/mvc/logging/DiagnosticExporter.kt:29) (which is currently unused).

---

## 7. Dependency Issues

### 7.1 JUnit 4 Legacy Support (LOW)

**Issue:** The project includes JUnit 4 for "legacy tests" but all tests appear to use JUnit 5.

```groovy
// app/build.gradle lines 221-223
testImplementation 'junit:junit:4.13.2'
testRuntimeOnly "org.junit.vintage:junit-vintage-engine:${versions.junit5}"
```

**Recommendation:** Verify if any tests actually require JUnit 4. If not, remove these dependencies.

---

## 8. Summary of Recommended Actions

### Priority: Critical

1. Consolidate duplicate `ConnectionState` classes
2. Consolidate duplicate `ConnectionStats` classes

### Priority: High

1. Extract `formatBytes()` and `formatDuration()` to a shared utility
2. Decide on UI framework (Fragment vs Compose) and remove the unused one
3. Consolidate state management approach

### Priority: Medium

1. Remove or integrate unused utility classes (BatteryOptimizer, SecureCredentialStorage, etc.)
2. Remove empty Koin modules
3. Refactor `SoftEtherVpnService` to use `VpnNotificationManager`

### Priority: Low

1. Consolidate notification constants
2. Review JUnit 4 dependency necessity
3. Consider extracting log export from VpnLogger

---

## Appendix: File Count by Category

| Category | Count | Notes |
|----------|-------|-------|
| Duplicate classes | 4 | ConnectionState (2), ConnectionStats (2) |
| Duplicate functions | 12 | formatBytes (6), formatDuration (6) |
| Unused classes | 5 | BatteryOptimizer, SecureCredentialStorage, DiagnosticExporter, CrashHandler, PerformanceMonitor |
| Unused Compose screens | 3 | HomeScreen, SettingsScreen, AboutScreen |
| Unused Koin modules | 3 | networkModule, protocolModule, allModules |
