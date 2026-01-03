# Implementation Plan: SoftEther Android Client

**Based on:** Forking "Minimum-VPN-Client-for-SoftEther-VPN"
**Template Reference:** `~/tmp/my-softether-android`
**Date:** January 2, 2026

## Executive Summary

This plan outlines a milestone-based approach to building a production-ready SoftEther VPN client for Android by forking the existing "Minimum-VPN-Client-for-SoftEther-VPN" project. The approach prioritizes:

1. **Validation first** - Ensure existing functionality works before making changes
2. **Testing infrastructure** - Adopt modern testing patterns from the template
3. **TDD approach** - All new features developed test-first

---

## Current State Analysis

### Minimum-VPN-Client-for-SoftEther-VPN

**Repository:** `ref/Minimum-VPN-Client-for-SoftEther-VPN`

**Architecture:**

- Pure Kotlin Android application (no native C++ code)
- Uses Android VpnService for tunnel management
- Implements SoftEther protocol directly in Kotlin
- Key components:
  - [`ClientBridge`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/client/ClientBridge.kt) - Central state management and configuration
  - [`ControlClient`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/client/ControlClient.kt) - Main VPN connection orchestrator
  - [`SoftEtherClient`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/client/softether/SoftEtherClient.kt) - Protocol implementation
  - [`TCPTerminal`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/teminal/tcp/TCPTerminal.kt) - SSL/TLS socket handling
  - [`UDPTerminal`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/teminal/udp/UDPTerminal.kt) - UDP acceleration
  - [`DhcpClient`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/client/dhcp/DhcpClient.kt) - DHCP negotiation
  - [`ARPClient`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/main/java/kittoku/mvc/service/client/arp/ARPClient.kt) - ARP resolution

**Build Configuration:**

- Kotlin 1.5.31 (outdated)
- Android Gradle Plugin 7.0.3 (outdated)
- compileSdkVersion 31, targetSdkVersion 31 (outdated)
- minSdkVersion 26

**Existing Tests:**

- Single test file: [`MvcTest.kt`](../ref/Minimum-VPN-Client-for-SoftEther-VPN/app/src/test/java/kittoku/mvc/MvcTest.kt)
- Tests include:
  - `testControlClient()` - Integration test requiring live server
  - `testControlClientUDP()` - UDP acceleration integration test
  - `testHashSha0()` - Unit test for SHA-0 hash
  - `testUDPDatagram()` - Unit test for UDP packet serialization
  - `testIPPacket()` - Unit test for IP packet serialization
  - `testNATTRegex()` - Unit test for NAT-T regex parsing
  - `testByteArraySearch()` - Unit test for byte array search

**Limitations:**

- No instrumented tests (androidTest)
- No mocking framework
- Integration tests require environment variables for server credentials
- No CI/CD configuration
- No pre-commit hooks
- No code formatting/linting configuration

### Template: my-softether-android

**Location:** `~/tmp/my-softether-android`

**Features to adopt:**

- Flutter + C++ FFI architecture (for future native code if needed)
- Comprehensive justfile with 40+ commands
- Pre-commit hooks (Dart, C++, YAML, shell, CMake)
- Testing infrastructure:
  - Unit tests (`test/unit/`)
  - Widget tests (`test/widget/`)
  - Golden tests (`golden_test/`)
  - Integration tests (`integration_test/`)
- Mise for tool management (Flutter, Java, Android SDK)
- Spack for C++ toolchain
- Coverage reporting

---

## Milestones

### Milestone 0: Project Setup and Fork

**Goal:** Create a clean fork with proper project structure and tooling.

**Tasks:**

- [ ] **0.1** Fork Minimum-VPN-Client-for-SoftEther-VPN to new repository
- [ ] **0.2** Rename project (e.g., "SoftEther Connect" or similar)
- [ ] **0.3** Update package name from `kittoku.mvc` to new namespace
- [ ] **0.4** Create `justfile` with basic commands:
  - `just build` - Build debug APK
  - `just test` - Run unit tests
  - `just install` - Install to device
  - `just clean` - Clean build artifacts
- [ ] **0.5** Create `.pre-commit-config.yaml` with:
  - Kotlin formatting (ktlint)
  - YAML/JSON validation
  - Secret scanning
  - Trailing whitespace
- [ ] **0.6** Create `.mise.toml` for tool management:
  - Java 17
  - Android SDK components
- [ ] **0.7** Update Gradle to latest stable:
  - Kotlin 2.0+
  - Android Gradle Plugin 8.x
  - Gradle 8.x
  - compileSdk 35, targetSdk 35
- [ ] **0.8** Verify project builds successfully

**Deliverables:**

- Clean, buildable project with modern tooling
- Justfile with basic commands
- Pre-commit hooks installed

---

### Milestone 1: Validate Existing Functionality

**Goal:** Ensure all existing features work correctly before making changes.

**Tasks:**

- [ ] **1.1** Set up test environment:
  - Document required environment variables (TEST_HOST, TEST_PORT, TEST_USERNAME, TEST_PASSWORD)
  - Create `.env.example` file
  - Add `just test-env-check` command
- [ ] **1.2** Run existing unit tests:
  - `testHashSha0()` - Verify SHA-0 implementation
  - `testUDPDatagram()` - Verify UDP serialization
  - `testIPPacket()` - Verify IP packet handling
  - `testNATTRegex()` - Verify NAT-T parsing
  - `testByteArraySearch()` - Verify byte search
- [ ] **1.3** Run integration tests against family VPN server:
  - `testControlClient()` - TCP connection
  - `testControlClientUDP()` - UDP acceleration
- [ ] **1.4** Manual testing checklist:
  - [ ] Connect to VPN server
  - [ ] Verify IP address assignment
  - [ ] Test internet connectivity through VPN
  - [ ] Test UDP acceleration toggle
  - [ ] Test disconnect
  - [ ] Test reconnection
  - [ ] Test connection persistence across app backgrounding
- [ ] **1.5** Document any issues found in `docs/known-issues.md`
- [ ] **1.6** Create baseline test report

**Deliverables:**

- All existing tests passing
- Manual test checklist completed
- Known issues documented
- Baseline test report

---

### Milestone 2: Improve Testing Infrastructure

**Goal:** Adopt modern testing patterns from the template to enable TDD.

**Tasks:**

#### 2.1 Unit Testing Framework

- [ ] **2.1.1** Add testing dependencies to `build.gradle.kts`:

  ```kotlin
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
  testImplementation("io.mockk:mockk:1.13.9")
  testImplementation("com.google.truth:truth:1.4.0")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
  ```

- [ ] **2.1.2** Migrate from JUnit 4 to JUnit 5
- [ ] **2.1.3** Create test utilities package:
  - `TestClientBridge` - Mock-friendly bridge for testing
  - `FakeVpnService` - Fake VpnService for unit tests
  - `TestCoroutineRule` - Coroutine test helper
- [ ] **2.1.4** Refactor existing tests to use MockK

#### 2.2 Instrumented Testing

- [ ] **2.2.1** Add instrumented test dependencies:

  ```kotlin
  androidTestImplementation("androidx.test:runner:1.5.2")
  androidTestImplementation("androidx.test:rules:1.5.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation("io.mockk:mockk-android:1.13.9")
  ```

- [ ] **2.2.2** Create base test classes for instrumented tests
- [ ] **2.2.3** Add UI tests for settings screens

#### 2.3 Integration Testing

- [ ] **2.3.1** Create `integration_test/` directory structure
- [ ] **2.3.2** Create mock SoftEther server for local testing:
  - Implement minimal protocol responses
  - Support connection handshake
  - Support DHCP simulation
- [ ] **2.3.3** Add integration test for full connection flow
- [ ] **2.3.4** Add `just test-integration` command

#### 2.4 Coverage and Reporting

- [ ] **2.4.1** Configure JaCoCo for code coverage
- [ ] **2.4.2** Add `just test-coverage` command
- [ ] **2.4.3** Set coverage thresholds (target: 60% initially)
- [ ] **2.4.4** Add coverage badge to README

#### 2.5 Justfile Testing Commands

Update justfile with comprehensive testing commands:

```just
# Run all unit tests
test:
    ./gradlew test

# Run unit tests with coverage
test-coverage:
    ./gradlew testDebugUnitTestCoverage
    @echo "Coverage report: app/build/reports/jacoco/index.html"

# Run instrumented tests on connected device
test-instrumented:
    ./gradlew connectedAndroidTest

# Run integration tests (requires mock server)
test-integration:
    ./gradlew :app:testDebugUnitTest --tests "*IntegrationTest*"

# Run all tests
test-all: test test-instrumented

# Check test environment variables
test-env-check:
    #!/usr/bin/env bash
    set -euo pipefail
    missing=""
    [ -z "${TEST_HOST:-}" ] && missing="$missing TEST_HOST"
    [ -z "${TEST_USERNAME:-}" ] && missing="$missing TEST_USERNAME"
    [ -z "${TEST_PASSWORD:-}" ] && missing="$missing TEST_PASSWORD"
    if [ -n "$missing" ]; then
        echo "Missing environment variables:$missing"
        echo "Copy .env.example to .env and fill in values"
        exit 1
    fi
    echo "✅ Test environment configured"
```

**Deliverables:**

- Modern testing framework with MockK
- JUnit 5 migration complete
- Instrumented test infrastructure
- Mock server for integration tests
- Coverage reporting with JaCoCo
- Comprehensive justfile testing commands

---

### Milestone 3: Code Quality and Architecture Improvements

**Goal:** Improve code quality and prepare architecture for new features.

**Tasks:**

#### 3.1 Code Quality Tools

- [ ] **3.1.1** Add ktlint for Kotlin formatting:

  ```kotlin
  plugins {
      id("org.jlleitschuh.gradle.ktlint") version "12.0.0"
  }
  ```

- [ ] **3.1.2** Add detekt for static analysis:

  ```kotlin
  plugins {
      id("io.gitlab.arturbosch.detekt") version "1.23.0"
  }
  ```

- [ ] **3.1.3** Configure detekt rules in `detekt.yml`
- [ ] **3.1.4** Add `just lint` and `just format` commands
- [ ] **3.1.5** Fix all linting issues

#### 3.2 Architecture Improvements

- [ ] **3.2.1** Extract interfaces for testability:
  - `IVpnConnection` - Connection abstraction
  - `IProtocolHandler` - Protocol abstraction
  - `INetworkTerminal` - Network I/O abstraction
- [ ] **3.2.2** Implement dependency injection (Hilt or Koin)
- [ ] **3.2.3** Separate UI from business logic
- [ ] **3.2.4** Create ViewModel layer for UI state

#### 3.3 Documentation

- [ ] **3.3.1** Add KDoc comments to public APIs
- [ ] **3.3.2** Create architecture diagram (Mermaid)
- [ ] **3.3.3** Document protocol implementation details
- [ ] **3.3.4** Create developer setup guide

**Deliverables:**

- ktlint and detekt configured
- All linting issues resolved
- Interfaces extracted for testability
- Dependency injection implemented
- Architecture documentation

---

### Milestone 4: Feature Development (TDD)

**Goal:** Add new features using Test-Driven Development.

All features in this milestone follow TDD:

1. Write failing test
2. Implement minimum code to pass
3. Refactor
4. Repeat

#### 4.1 Connection Profiles

- [ ] **4.1.1** Write tests for profile data model
- [ ] **4.1.2** Implement `VpnProfile` data class
- [ ] **4.1.3** Write tests for profile repository
- [ ] **4.1.4** Implement `ProfileRepository` with Room database
- [ ] **4.1.5** Write tests for profile import/export
- [ ] **4.1.6** Implement profile import/export (JSON format)
- [ ] **4.1.7** Write UI tests for profile management
- [ ] **4.1.8** Implement profile management UI

#### 4.2 Connection Status and Notifications

- [ ] **4.2.1** Write tests for connection state machine
- [ ] **4.2.2** Implement `ConnectionState` sealed class:

  ```kotlin
  sealed class ConnectionState {
      object Disconnected : ConnectionState()
      data class Connecting(val step: String) : ConnectionState()
      data class Connected(val stats: ConnectionStats) : ConnectionState()
      data class Error(val message: String) : ConnectionState()
  }
  ```

- [ ] **4.2.3** Write tests for notification manager
- [ ] **4.2.4** Implement persistent notification with connection stats
- [ ] **4.2.5** Write tests for quick settings tile
- [ ] **4.2.6** Implement quick settings tile

#### 4.3 Connection Statistics

- [ ] **4.3.1** Write tests for traffic statistics
- [ ] **4.3.2** Implement traffic counter (bytes sent/received)
- [ ] **4.3.3** Write tests for connection duration
- [ ] **4.3.4** Implement connection timer
- [ ] **4.3.5** Write tests for statistics persistence
- [ ] **4.3.6** Implement statistics history (Room database)
- [ ] **4.3.7** Write UI tests for statistics display
- [ ] **4.3.8** Implement statistics UI

#### 4.4 Auto-Connect Features

- [ ] **4.4.1** Write tests for boot receiver
- [ ] **4.4.2** Implement connect-on-boot option
- [ ] **4.4.3** Write tests for network change handling
- [ ] **4.4.4** Implement auto-reconnect on network change
- [ ] **4.4.5** Write tests for trusted networks
- [ ] **4.4.6** Implement trusted network detection (don't connect on home WiFi)

#### 4.5 Split Tunneling

- [ ] **4.5.1** Write tests for app list provider
- [ ] **4.5.2** Implement installed apps list
- [ ] **4.5.3** Write tests for split tunnel configuration
- [ ] **4.5.4** Implement split tunnel settings storage
- [ ] **4.5.5** Write tests for VpnService builder with split tunnel
- [ ] **4.5.6** Implement split tunnel in VpnService
- [ ] **4.5.7** Write UI tests for app selection
- [ ] **4.5.8** Implement split tunnel UI

**Deliverables:**

- Connection profiles with import/export
- Rich connection status and notifications
- Connection statistics with history
- Auto-connect features
- Split tunneling support
- All features with comprehensive tests

---

### Milestone 5: UI Modernization

**Goal:** Update UI to Material Design 3 with modern Android patterns.

**Tasks:**

- [ ] **5.1** Migrate to Jetpack Compose:
  - Add Compose dependencies
  - Create Compose theme with Material 3
  - Migrate screens incrementally
- [ ] **5.2** Implement new screens:
  - Home screen with connection button
  - Profile list screen
  - Profile edit screen
  - Settings screen
  - Statistics screen
  - About screen
- [ ] **5.3** Add dark mode support
- [ ] **5.4** Add dynamic color support (Android 12+)
- [ ] **5.5** Implement animations:
  - Connection state transitions
  - List item animations
  - Navigation transitions
- [ ] **5.6** Add accessibility support:
  - Content descriptions
  - Touch target sizes
  - Screen reader support

**Deliverables:**

- Full Jetpack Compose UI
- Material Design 3 theming
- Dark mode and dynamic colors
- Smooth animations
- Accessibility compliance

---

### Milestone 6: Production Readiness

**Goal:** Prepare for production release.

**Tasks:**

#### 6.1 Security Hardening

- [ ] **6.1.1** Implement certificate pinning
- [ ] **6.1.2** Secure credential storage (EncryptedSharedPreferences)
- [ ] **6.1.3** Add ProGuard/R8 rules
- [ ] **6.1.4** Security audit of protocol implementation

#### 6.2 Performance Optimization

- [ ] **6.2.1** Profile memory usage
- [ ] **6.2.2** Optimize buffer allocations
- [ ] **6.2.3** Reduce battery consumption
- [ ] **6.2.4** Benchmark connection speed

#### 6.3 Error Handling and Logging

- [ ] **6.3.1** Implement structured logging
- [ ] **6.3.2** Add crash reporting (optional, privacy-respecting)
- [ ] **6.3.3** Improve error messages for users
- [ ] **6.3.4** Add diagnostic export feature

#### 6.4 Release Preparation

- [ ] **6.4.1** Create release signing configuration
- [ ] **6.4.2** Configure version management
- [ ] **6.4.3** Write release notes template
- [ ] **6.4.4** Create app store assets (icons, screenshots)
- [ ] **6.4.5** Write privacy policy
- [ ] **6.4.6** Add `just release` command

**Deliverables:**

- Security-hardened application
- Optimized performance
- Comprehensive error handling
- Release-ready build configuration

---

## Testing Strategy

### Test Pyramid

```
                    /\
                   /  \
                  / E2E \        <- Integration tests with real/mock server
                 /--------\
                /  Widget  \     <- UI component tests
               /------------\
              /    Unit      \   <- Business logic tests
             /----------------\
```

### Test Categories

| Category | Location | Purpose | Run Command |
|----------|----------|---------|-------------|
| Unit | `app/src/test/` | Business logic, data models | `just test` |
| Instrumented | `app/src/androidTest/` | UI, Android components | `just test-instrumented` |
| Integration | `app/src/test/*IntegrationTest*` | Full connection flow | `just test-integration` |

### Mock Server

For integration testing without a real SoftEther server:

```kotlin
class MockSoftEtherServer {
    fun start(port: Int)
    fun stop()
    fun setResponse(step: ProtocolStep, response: ByteArray)
    fun getReceivedPackets(): List<ByteArray>
}
```

### Test Data

Create test fixtures in `app/src/test/resources/`:

- `valid-profile.json` - Valid VPN profile
- `invalid-profile.json` - Invalid profile for error testing
- `dhcp-response.bin` - Sample DHCP response
- `softether-handshake.bin` - Sample handshake packets

---

## Risk Mitigation

### R1: Protocol Compatibility

**Risk:** Changes may break compatibility with SoftEther servers.

**Mitigation:**

- Maintain integration tests against real server
- Document protocol behavior
- Keep original implementation as reference
- Test against multiple SoftEther versions

### R2: Android API Changes

**Risk:** Future Android versions may change VpnService behavior.

**Mitigation:**

- Test on multiple Android versions (26-35)
- Follow Android VPN best practices
- Monitor Android developer announcements
- Use AndroidX compatibility libraries

### R3: Testing Without Server

**Risk:** Integration tests require live server access.

**Mitigation:**

- Create mock server for CI/CD
- Document manual testing procedures
- Use recorded packet captures for replay tests

### R4: Kotlin/Gradle Version Upgrades

**Risk:** Major version upgrades may introduce breaking changes.

**Mitigation:**

- Upgrade incrementally
- Run full test suite after each upgrade
- Keep dependencies up to date
- Use version catalogs for dependency management

---

## Timeline Estimate

| Milestone | Duration | Dependencies |
|-----------|----------|--------------|
| M0: Project Setup | 1-2 days | None |
| M1: Validate Existing | 2-3 days | M0 |
| M2: Testing Infrastructure | 1-2 weeks | M1 |
| M3: Code Quality | 1 week | M2 |
| M4: Feature Development | 3-4 weeks | M3 |
| M5: UI Modernization | 2-3 weeks | M4 |
| M6: Production Readiness | 1-2 weeks | M5 |

**Total Estimated Duration:** 10-14 weeks

---

## Success Criteria

### Milestone 0

- [ ] Project builds without errors
- [ ] Pre-commit hooks run successfully
- [ ] Justfile commands work

### Milestone 1

- [ ] All existing unit tests pass
- [ ] Integration tests pass against family VPN server
- [ ] Manual testing checklist completed

### Milestone 2

- [ ] Test coverage > 60%
- [ ] MockK-based unit tests
- [ ] Mock server for integration tests
- [ ] All justfile test commands work

### Milestone 3

- [ ] Zero ktlint/detekt violations
- [ ] Dependency injection configured
- [ ] Architecture documentation complete

### Milestone 4

- [ ] All new features have tests
- [ ] Test coverage > 70%
- [ ] Features work on Android 26-35

### Milestone 5

- [ ] Full Compose UI
- [ ] Material 3 theming
- [ ] Accessibility audit passed

### Milestone 6

- [ ] Security audit passed
- [ ] Performance benchmarks met
- [ ] Release build signed and tested

---

## Appendix: Justfile Reference

Complete justfile for the project:

```just
# SoftEther Connect - Android VPN Client
# See: https://just.systems/man/en

set shell := ["bash", "-eu", "-o", "pipefail", "-c"]
set dotenv-load := true

default: help

help:
    just --list

# === Setup ===

setup: install-hooks
    ./gradlew dependencies

install-hooks:
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -d .git ]; then
        pre-commit install --install-hooks
    fi

# === Build ===

build:
    ./gradlew assembleDebug

build-release:
    ./gradlew assembleRelease

clean:
    ./gradlew clean

# === Test ===

test:
    ./gradlew test

test-coverage:
    ./gradlew testDebugUnitTestCoverage

test-instrumented:
    ./gradlew connectedAndroidTest

test-integration:
    ./gradlew :app:testDebugUnitTest --tests "*IntegrationTest*"

test-all: test test-instrumented

test-env-check:
    #!/usr/bin/env bash
    set -euo pipefail
    missing=""
    [ -z "${TEST_HOST:-}" ] && missing="$missing TEST_HOST"
    [ -z "${TEST_USERNAME:-}" ] && missing="$missing TEST_USERNAME"
    [ -z "${TEST_PASSWORD:-}" ] && missing="$missing TEST_PASSWORD"
    if [ -n "$missing" ]; then
        echo "Missing:$missing"
        exit 1
    fi
    echo "✅ Test environment OK"

# === Code Quality ===

lint:
    ./gradlew ktlintCheck detekt

format:
    ./gradlew ktlintFormat

validate: format lint test

# === Install ===

install:
    ./gradlew installDebug

install-release:
    ./gradlew installRelease

run:
    ./gradlew installDebug
    adb shell am start -n com.example.softether/.MainActivity

# === Release ===

release:
    ./gradlew assembleRelease bundleRelease
```

---

## References

- [Minimum-VPN-Client-for-SoftEther-VPN](https://github.com/kittoku/Minimum-VPN-Client-for-SoftEther-VPN)
- [SoftEther VPN Protocol Specification](https://www.softether.org/)
- [Android VpnService Documentation](https://developer.android.com/reference/android/net/VpnService)
- [MockK - Mocking library for Kotlin](https://mockk.io/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
