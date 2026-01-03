# Cookiecutter Flutter + C++ Template Plan

**Purpose:** Create a cookiecutter template for Flutter projects with C++ native code integration, specifically designed for building the SoftEther VPN Android client.

**Date:** December 25, 2025

## Table of Contents

1. [Overview](#overview)
2. [Template Variables](#template-variables)
3. [Project Structure](#project-structure)
4. [Flutter Tooling](#flutter-tooling)
5. [C++ Tooling](#c-tooling)
6. [Pre-commit Configuration](#pre-commit-configuration)
7. [Justfile Commands](#justfile-commands)
8. [Testing Framework](#testing-framework)
9. [CI/CD Integration](#cicd-integration)
10. [Sources](#sources)

---

## Overview

This template will provide a production-ready Flutter project scaffold with:
- Native C++23 code integration via FFI (Foreign Function Interface)
- Comprehensive linting, formatting, and static analysis for both Dart and C++
- Testing infrastructure for unit, widget, golden, and integration tests
- Pre-commit hooks for code quality enforcement
- Justfile-based workflow automation
- **Android-only** target platform, built and tested on Linux

### Target Use Case
Building a SoftEther VPN client for Android that requires:
- Flutter UI layer with Material Design 3
- C++23 native code for VPN protocol implementation
- Platform channels for Android VpnService integration

---

## Template Variables

```json
{
  "project_name": "My Flutter App",
  "project_slug": "{{ cookiecutter.project_name|lower|replace(' ', '_')|replace('-', '_') }}",
  "project_description": "A Flutter application with C++ native code",
  "author_name": "Your Name",
  "author_email": "your.email@example.com",
  "min_android_sdk": "24",
  "target_android_sdk": "35",
  "flutter_version": "3.27.0",
  "dart_version": "3.6.0",
  "use_riverpod": "y",
  "use_go_router": "y"
}
```

### Variable Explanations

#### `use_riverpod`

**Riverpod** is a reactive state management library for Flutter. It is the successor to the popular Provider package, created by the same author (Remi Rousselet).

**What it does:**
- Provides a way to manage application state in a predictable, testable manner
- Enables dependency injection without BuildContext
- Supports compile-time safety (catches errors at compile time, not runtime)
- Works well with async operations (API calls, database queries)

**Why it's useful for VPN client:**
- Managing VPN connection state (connected, disconnected, connecting)
- Storing user preferences and server configurations
- Handling async operations like authentication and server discovery

#### `use_go_router`

**GoRouter** is a declarative routing package for Flutter that simplifies navigation.

**What it does:**
- Provides URL-based routing (useful for deep linking)
- Supports nested navigation and shell routes
- Handles redirects (e.g., redirect to login if not authenticated)
- Works seamlessly with Riverpod for route guards

**Why it's useful for VPN client:**
- Navigate between screens (home, settings, server list, connection details)
- Deep link support for connecting to specific servers via URL
- Route guards to prevent access to certain screens when not connected

### Notes on Removed Variables

- **`organization`**: Not included because Flutter Android projects use `applicationId` in `build.gradle.kts` which is derived from `project_slug`. If you need a custom package name format like `com.example.myapp`, this can be configured manually after generation.

- **`license`**: Defaulted to MIT internally. This is a private project; licensing can be revised later.

- **`cpp_standard`**: Fixed at C++23 (not configurable). See compiler requirements below.

- **`include_ios/macos/windows/linux`**: Removed. This template is Android-only.

---

## Project Structure

```
{{ cookiecutter.project_slug }}/
├── android/                          # Android platform code
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── kotlin/               # Kotlin platform channels
│   │   │   └── jniLibs/              # Compiled native libraries
│   │   └── CMakeLists.txt            # Android NDK build config
│   └── build.gradle.kts
├── lib/                              # Dart/Flutter source
│   ├── src/
│   │   ├── core/                     # Core utilities
│   │   ├── features/                 # Feature modules
│   │   ├── ffi/                      # FFI bindings
│   │   └── widgets/                  # Reusable widgets
│   └── main.dart
├── native/                           # C++ native code
│   ├── include/                      # Public headers
│   ├── src/                          # Implementation
│   ├── tests/                        # C++ unit tests
│   └── CMakeLists.txt                # CMake build config
├── test/                             # Dart unit tests
├── integration_test/                 # Integration tests
├── golden_test/                      # Golden/snapshot tests
├── scripts/                          # Build and utility scripts
│   ├── build-native.sh
│   ├── generate-ffi-bindings.sh
│   └── run-tests.sh
├── .pre-commit-config.yaml
├── analysis_options.yaml             # Dart analyzer config
├── justfile
├── pubspec.yaml
└── README.md
```

---

## Flutter Tooling

### Dart Analyzer and Linting

The Dart analyzer is the core engine for static analysis. Configuration is done via `analysis_options.yaml`.

#### Official Lint Package: `flutter_lints`

Google's officially recommended rule set for Flutter apps.

```yaml
# analysis_options.yaml
include: package:flutter_lints/flutter.yaml

analyzer:
  exclude:
    - "**/*.g.dart"
    - "**/*.freezed.dart"
    - "build/**"
    - ".dart_tool/**"
  errors:
    invalid_annotation_target: ignore
  language:
    strict-casts: true
    strict-inference: true
    strict-raw-types: true

linter:
  rules:
    # Error rules
    - avoid_dynamic_calls
    - avoid_returning_null_for_future
    - avoid_slow_async_io
    - cancel_subscriptions
    - close_sinks
    - literal_only_boolean_expressions
    - throw_in_finally
    - unnecessary_statements
    
    # Style rules
    - always_declare_return_types
    - always_put_required_named_parameters_first
    - avoid_bool_literals_in_conditional_expressions
    - avoid_catches_without_on_clauses
    - avoid_catching_errors
    - avoid_classes_with_only_static_members
    - avoid_double_and_int_checks
    - avoid_equals_and_hash_code_on_mutable_classes
    - avoid_escaping_inner_quotes
    - avoid_field_initializers_in_const_classes
    - avoid_final_parameters
    - avoid_implementing_value_types
    - avoid_js_rounded_ints
    - avoid_multiple_declarations_per_line
    - avoid_positional_boolean_parameters
    - avoid_private_typedef_functions
    - avoid_redundant_argument_values
    - avoid_returning_this
    - avoid_setters_without_getters
    - avoid_types_on_closure_parameters
    - avoid_unused_constructor_parameters
    - avoid_void_async
    - cascade_invocations
    - cast_nullable_to_non_nullable
    - deprecated_consistency
    - directives_ordering
    - do_not_use_environment
    - eol_at_end_of_file
    - join_return_with_assignment
    - leading_newlines_in_multiline_strings
    - missing_whitespace_between_adjacent_strings
    - no_runtimeType_toString
    - noop_primitive_operations
    - omit_local_variable_types
    - one_member_abstracts
    - only_throw_errors
    - parameter_assignments
    - prefer_asserts_in_initializer_lists
    - prefer_constructors_over_static_methods
    - prefer_final_in_for_each
    - prefer_final_locals
    - prefer_foreach
    - prefer_if_elements_to_conditional_expressions
    - prefer_int_literals
    - prefer_mixin
    - prefer_null_aware_method_calls
    - prefer_single_quotes
    - require_trailing_commas
    - sort_constructors_first
    - sort_unnamed_constructors_first
    - tighten_type_of_initializing_formals
    - type_annotate_public_apis
    - unawaited_futures
    - unnecessary_await_in_return
    - unnecessary_lambdas
    - unnecessary_null_aware_operator_on_extension_on_nullable
    - unnecessary_null_checks
    - unnecessary_parenthesis
    - unnecessary_raw_strings
    - unreachable_from_main
    - use_enums
    - use_if_null_to_convert_nulls_to_bools
    - use_is_even_rather_than_modulo
    - use_late_for_private_fields_and_variables
    - use_named_constants
    - use_raw_strings
    - use_setters_to_change_properties
    - use_string_buffers
    - use_super_parameters
    - use_to_and_as_if_applicable
```

#### Advanced Analysis: DCM (Dart Code Metrics)

DCM provides 450+ additional rules for production-grade applications.

```yaml
# pubspec.yaml (dev_dependencies)
dev_dependencies:
  dcm: ^1.34.0
```

DCM metrics include:
- **CYCLO** - Cyclomatic Complexity
- **MNL** - Max Nesting Level
- **NOI** - Number of Imports
- **NOM** - Number of Methods
- **NOP** - Number of Parameters

### Dart Formatter

Built-in `dart format` command for consistent code formatting.

```bash
# Format all Dart files
dart format .

# Check formatting without modifying
dart format --set-exit-if-changed .
```

### Pre-commit Package: `dart_pre_commit`

A collection of pre-commit hooks for Dart/Flutter projects.

```yaml
# pubspec.yaml (dev_dependencies)
dev_dependencies:
  dart_pre_commit: ^6.1.0
```

Features:
- Run `dart format`
- Run `dart analyze`
- Check for invalid imports in test files
- Ensure all src files publicly visible are exported
- Check Flutter compatibility
- Check for outdated packages
- OSV-Scanner for security vulnerabilities

---

## C++ Tooling

The C++ tooling follows patterns established in the existing `simple-cpp23` cookiecutter template.

### Compiler Support (C++23)

| Compiler | Version | Notes |
|----------|---------|-------|
| GCC | 14+ | Required for full C++23 support |
| Clang/LLVM | 17+ | Required for C++23, preferred for sanitizers |
| Android NDK | r27+ | Uses Clang 18 internally |

**Note:** C++23 requires relatively recent compilers. GCC 14 and Clang 17 provide comprehensive C++23 feature support including `std::expected`, `std::print`, deducing `this`, and more.

### CMake Configuration

```cmake
# native/CMakeLists.txt
cmake_minimum_required(VERSION 3.22)
project({{ cookiecutter.project_slug }}_native VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 23)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_EXPORT_COMPILE_COMMANDS ON)

# Compiler warnings
add_compile_options(
    -Wall -Wextra -Wpedantic
    -Werror=return-type
    -Werror=uninitialized
    -Wno-unused-parameter
)

# Sanitizer options
option(ENABLE_ASAN "Enable AddressSanitizer" OFF)
option(ENABLE_UBSAN "Enable UndefinedBehaviorSanitizer" OFF)
option(ENABLE_TSAN "Enable ThreadSanitizer" OFF)
option(ENABLE_MSAN "Enable MemorySanitizer" OFF)
option(ENABLE_COVERAGE "Enable code coverage" OFF)

if(ENABLE_ASAN)
    add_compile_options(-fsanitize=address -fno-omit-frame-pointer)
    add_link_options(-fsanitize=address)
endif()

if(ENABLE_UBSAN)
    add_compile_options(-fsanitize=undefined)
    add_link_options(-fsanitize=undefined)
endif()

if(ENABLE_TSAN)
    add_compile_options(-fsanitize=thread)
    add_link_options(-fsanitize=thread)
endif()

if(ENABLE_MSAN)
    add_compile_options(-fsanitize=memory -fno-omit-frame-pointer)
    add_link_options(-fsanitize=memory)
endif()

if(ENABLE_COVERAGE)
    add_compile_options(--coverage -fprofile-arcs -ftest-coverage)
    add_link_options(--coverage)
endif()

# Library target
add_library(${PROJECT_NAME} SHARED
    src/library.cpp
)

target_include_directories(${PROJECT_NAME} PUBLIC
    $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/include>
    $<INSTALL_INTERFACE:include>
)
```

### C++ Formatters and Linters

#### clang-format

Code formatting for C/C++ files.

```yaml
# .clang-format
BasedOnStyle: Google
IndentWidth: 4
ColumnLimit: 120
AllowShortFunctionsOnASingleLine: Inline
AllowShortIfStatementsOnASingleLine: Never
AllowShortLoopsOnASingleLine: false
BreakBeforeBraces: Attach
PointerAlignment: Left
```

#### clang-tidy

Static analysis for C/C++ code.

```yaml
# .clang-tidy
Checks: >
  -*,
  bugprone-*,
  cert-*,
  clang-analyzer-*,
  cppcoreguidelines-*,
  google-*,
  hicpp-*,
  misc-*,
  modernize-*,
  performance-*,
  portability-*,
  readability-*,
  -modernize-use-trailing-return-type,
  -readability-identifier-length,
  -cppcoreguidelines-avoid-magic-numbers,
  -readability-magic-numbers

WarningsAsErrors: ''
HeaderFilterRegex: '.*'
FormatStyle: file
```

#### cppcheck

Additional static analysis tool.

```bash
cppcheck --enable=warning,style,performance,portability \
         --error-exitcode=1 \
         --inline-suppr \
         --std=c++23 \
         -I include \
         src/
```

#### cmake-format and cmake-lint

CMake file formatting and linting.

```yaml
# .cmake-format.yaml
format:
  line_width: 120
  tab_size: 4
  use_tabchars: false
  separate_ctrl_name_with_space: false
  separate_fn_name_with_space: false
  dangle_parens: true
```

### C++ Sanitizers

| Sanitizer | Purpose | Compiler Support |
|-----------|---------|------------------|
| AddressSanitizer (ASan) | Memory errors, buffer overflows | GCC, Clang |
| UndefinedBehaviorSanitizer (UBSan) | Undefined behavior detection | GCC, Clang |
| ThreadSanitizer (TSan) | Data race detection | Clang preferred |
| MemorySanitizer (MSan) | Uninitialized memory reads | Clang only |

### C++ Testing: GoogleTest

```cmake
# native/tests/CMakeLists.txt
include(FetchContent)
FetchContent_Declare(
    googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG v1.15.2
)
FetchContent_MakeAvailable(googletest)

enable_testing()

add_executable(native_tests
    test_main.cpp
)

target_link_libraries(native_tests
    native_lib
    GTest::gtest_main
)

include(GoogleTest)
gtest_discover_tests(native_tests)
```

### C++ Code Coverage

Using `gcovr` for GCC or `llvm-cov` for Clang.

```bash
# Generate coverage report with GCC
gcovr --object-directory build -r . --html --html-details -o coverage/index.html

# Generate coverage report with Clang
llvm-profdata merge -sparse *.profraw -o coverage.profdata
llvm-cov show ./tests -instr-profile=coverage.profdata -format=html -output-dir=coverage
```

---

## Pre-commit Configuration

The `.pre-commit-config.yaml` combines hooks for both Dart/Flutter and C++ code quality.

```yaml
# .pre-commit-config.yaml
fail_fast: false

repos:
  # Core hygiene hooks
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v6.0.0
    hooks:
      - id: check-added-large-files
      - id: check-case-conflict
      - id: check-executables-have-shebangs
      - id: check-merge-conflict
      - id: check-shebang-scripts-are-executable
      - id: check-symlinks
      - id: check-yaml
      - id: check-json
      - id: detect-private-key
      - id: end-of-file-fixer
      - id: fix-byte-order-marker
      - id: mixed-line-ending
      - id: trailing-whitespace

  # Secret scanning
  - repo: https://github.com/sirwart/ripsecrets
    rev: v0.1.11
    hooks:
      - id: ripsecrets

  # Typo checking
  - repo: https://github.com/crate-ci/typos
    rev: v1.40.0
    hooks:
      - id: typos
        args: [--write-changes]

  # YAML formatting
  - repo: https://github.com/lyz-code/yamlfix
    rev: 1.17.0
    hooks:
      - id: yamlfix

  # Shell script formatting
  - repo: https://github.com/adamryczkowski/beautysh
    rev: v6.2.3
    hooks:
      - id: beautysh
        args: [--indent-size=4, --tab, --force-function-style=fnpar]

  # Justfile formatting
  - repo: local
    hooks:
      - id: format-justfiles
        name: Format justfiles
        entry: bash -c 'just --justfile "$1" --dump > "$1.formatted" && mv "$1.formatted" "$1"' --
        files: justfile$
        language: system
        pass_filenames: true

  # CMake formatting and linting
  - repo: https://github.com/cheshirekow/cmake-format-precommit
    rev: v0.6.10
    hooks:
      - id: cmake-format
        files: CMakeLists\.txt$|\.cmake$
        exclude: build/
      - id: cmake-lint
        files: CMakeLists\.txt$|\.cmake$
        exclude: build/
        args: [--linelength=120]

  # C/C++ formatting
  - repo: https://github.com/pre-commit/mirrors-clang-format
    rev: v18.1.8
    hooks:
      - id: clang-format
        args: [--Werror]
        files: \.(c|cc|cpp|cxx|h|hpp)$
        stages: [pre-push]

  # C/C++ static analysis
  - repo: local
    hooks:
      - id: cppcheck
        name: cppcheck
        entry: cppcheck
        language: system
        args:
          - --enable=warning,style,performance,portability
          - --error-exitcode=1
          - --inline-suppr
          - --std=c++23
          - -I
          - native/include
        files: \.(c|cc|cpp|cxx|h|hpp)$
        exclude: build/
        stages: [pre-push]

  # Dart/Flutter hooks
  - repo: local
    hooks:
      - id: dart-format
        name: dart format
        entry: dart format
        language: system
        files: \.dart$
        args: [--set-exit-if-changed]

      - id: dart-analyze
        name: dart analyze
        entry: dart analyze
        language: system
        files: \.dart$
        args: [--fatal-infos]
        pass_filenames: false

      - id: flutter-test
        name: flutter test
        entry: flutter test
        language: system
        files: \.dart$
        pass_filenames: false
        stages: [pre-push]
```

---

## Justfile Commands

The justfile provides a unified interface for all development tasks.

```just
# justfile for Flutter + C++ project
set shell := ["bash", "-eu", "-o", "pipefail", "-c"]
set dotenv-load := true

# Default task prints available recipes
default: help

help:
    just --list

# Setup development environment
setup: ensure-tools install-deps install-hooks
    @echo "Development environment ready!"

# Install Flutter dependencies
install-deps:
    flutter pub get

# Install pre-commit hooks
install-hooks:
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -d .git ]; then
      pre-commit install --install-hooks
    else
      echo "Not a git repository; skipping pre-commit hook install"
    fi

# Ensure required tools are available
ensure-tools:
    #!/usr/bin/env bash
    set -euo pipefail
    command -v flutter >/dev/null 2>&1 || { echo "Flutter not found"; exit 1; }
    command -v dart >/dev/null 2>&1 || { echo "Dart not found"; exit 1; }
    command -v cmake >/dev/null 2>&1 || { echo "CMake not found"; exit 1; }
    command -v pre-commit >/dev/null 2>&1 || pipx install pre-commit

# Build native C++ library for host platform
build-native:
    #!/usr/bin/env bash
    set -euo pipefail
    mkdir -p native/build
    cmake -S native -B native/build -DCMAKE_BUILD_TYPE=Debug
    cmake --build native/build -j$(nproc 2>/dev/null || echo 4)

# Build native C++ library for Android
build-native-android arch="arm64-v8a":
    #!/usr/bin/env bash
    set -euo pipefail
    mkdir -p native/build-android-{{ arch }}
    cmake -S native -B native/build-android-{{ arch }} \
      -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI={{ arch }} \
      -DANDROID_PLATFORM=android-24 \
      -DCMAKE_BUILD_TYPE=Release
    cmake --build native/build-android-{{ arch }} -j$(nproc 2>/dev/null || echo 4)

# Run C++ tests
test-native: build-native
    #!/usr/bin/env bash
    set -euo pipefail
    ctest --test-dir native/build --output-on-failure

# Run Flutter unit tests
test:
    flutter test

# Run Flutter unit tests with coverage
test-coverage:
    flutter test --coverage
    @echo "Coverage report: coverage/lcov.info"

# Run widget tests
test-widget:
    flutter test test/widget/

# Run golden tests
test-golden:
    flutter test golden_test/

# Update golden files
update-goldens:
    flutter test --update-goldens golden_test/

# Run integration tests
test-integration:
    flutter test integration_test/

# Run all tests
test-all: test-native test test-widget test-golden

# Format Dart code
format-dart:
    dart format .

# Format C++ code
format-cpp:
    #!/usr/bin/env bash
    set -euo pipefail
    find native -name "*.cpp" -o -name "*.hpp" -o -name "*.h" | xargs clang-format -i

# Format all code
format: format-dart format-cpp
    @echo "All code formatted"

# Analyze Dart code
analyze:
    dart analyze --fatal-infos

# Run all pre-commit hooks
validate: format
    pre-commit run --all-files

# Update pre-commit hooks
update-pre-commit:
    pre-commit autoupdate --freeze

# Generate FFI bindings
generate-ffi:
    dart run ffigen

# Build Android APK (debug)
build-android-debug: build-native-android
    flutter build apk --debug

# Build Android APK (release)
build-android-release: build-native-android
    flutter build apk --release

# Build Android App Bundle
build-android-bundle: build-native-android
    flutter build appbundle

# Run the app on connected device
run:
    flutter run

# Clean build artifacts
clean:
    flutter clean
    rm -rf native/build native/build-android-* coverage/
    find . -type d -name ".dart_tool" -prune -exec rm -rf {} + 2>/dev/null || true

# Run C++ tests with AddressSanitizer
asan-ubsan:
    #!/usr/bin/env bash
    set -euo pipefail
    mkdir -p native/build-asan
    cmake -S native -B native/build-asan \
      -DCMAKE_BUILD_TYPE=Debug \
      -DENABLE_ASAN=ON \
      -DENABLE_UBSAN=ON
    cmake --build native/build-asan -j$(nproc 2>/dev/null || echo 4)
    ctest --test-dir native/build-asan --output-on-failure

# Generate C++ coverage report
coverage-native:
    #!/usr/bin/env bash
    set -euo pipefail
    mkdir -p native/build-coverage
    cmake -S native -B native/build-coverage \
      -DCMAKE_BUILD_TYPE=Debug \
      -DENABLE_COVERAGE=ON
    cmake --build native/build-coverage -j$(nproc 2>/dev/null || echo 4)
    ctest --test-dir native/build-coverage --output-on-failure
    gcovr --object-directory native/build-coverage -r native \
      --html --html-details -o native/coverage/index.html
    @echo "Coverage report: native/coverage/index.html"
```

---

## Testing Framework

### Flutter Testing Summary

| Testing Type | Tooling | Purpose | Justfile Command |
|-------------|---------|---------|------------------|
| Unit Test | `test`, `mocktail` | Business logic validation | `just test` |
| Widget Test | `flutter_test` | UI interaction in isolation | `just test-widget` |
| Golden Test | `alchemist` | Visual regression detection | `just test-golden` |
| E2E Test | `patrol` | Real-device, real-flow interaction | `just test-integration` |

### C++ Testing Summary

| Testing Type | Tooling | Purpose | Justfile Command |
|-------------|---------|---------|------------------|
| Unit Test | GoogleTest | C++ logic validation | `just test-native` |
| Sanitizer Test | ASan/UBSan | Memory and undefined behavior | `just asan-ubsan` |
| Coverage | gcovr/llvm-cov | Code coverage reporting | `just coverage-native` |

### Flutter Test Dependencies

```yaml
# pubspec.yaml
dev_dependencies:
  # Core testing
  flutter_test:
    sdk: flutter
  test: ^1.25.0
  
  # Mocking
  mocktail: ^1.0.0
  
  # Golden testing
  alchemist: ^0.10.0
  
  # Integration testing
  patrol: ^3.0.0
  integration_test:
    sdk: flutter
```

### Flutter Coverage Configuration

```yaml
# pubspec.yaml
flutter:
  # Enable coverage collection
  test:
    coverage:
      enabled: true
```

Generate and view coverage:

```bash
# Generate coverage
flutter test --coverage

# Generate HTML report (requires lcov)
genhtml coverage/lcov.info -o coverage/html

# Open report
open coverage/html/index.html
```

---

## CI/CD Integration

This template does not include pre-configured CI/CD workflows since the project is not hosted on GitHub.

For local development, use the justfile commands to run tests and builds:

```bash
# Run all quality checks
just validate

# Run all tests
just test-all

# Build Android APK
just build-android-release
```

If you later need CI/CD, consider:
- **Self-hosted GitLab CI**: Use `.gitlab-ci.yml` with Flutter Docker images
- **Jenkins**: Configure pipelines with Flutter SDK installed on agents
- **Local automation**: Use `just` commands in cron jobs or git hooks

---

## Sources

### Flutter Tooling Research

| Source | URL | Date Accessed |
|--------|-----|---------------|
| DCM - Flutter Lint and Static Analysis | https://dcm.dev/blog/2025/10/21/getting-started-flutter-static-analytics-lints | Dec 25, 2025 |
| dart_pre_commit package | https://pub.dev/packages/dart_pre_commit | Dec 25, 2025 |
| Flutter Mobile Testing Methodologies 2025 | https://dev.to/3lvv0w/flutter-mobile-testing-methodologies-recap-2025-523j | Dec 25, 2025 |
| Using FFI in a Flutter plugin | https://codelabs.developers.google.com/codelabs/flutter-ffigen | Dec 25, 2025 |

### C++ Tooling (from existing templates)

| Source | Location |
|--------|----------|
| simple-cpp23 template | ~/tmp/Cookiecutters/templates/simple-cpp23 |
| nanobind-cpp23-poetry template | ~/tmp/Cookiecutters/templates/nanobind-cpp23-poetry |

### Key Packages and Tools

| Tool | Version | Purpose |
|------|---------|---------|
| flutter_lints | latest | Official Flutter lint rules |
| dcm | ^1.34.0 | Advanced Dart code metrics |
| dart_pre_commit | ^6.1.0 | Pre-commit hooks for Dart |
| mocktail | ^1.0.0 | Mocking library for tests |
| alchemist | ^0.10.0 | Golden testing framework |
| patrol | ^3.0.0 | Integration testing |
| GoogleTest | v1.15.2 | C++ unit testing |
| clang-format | v18.1.8 | C++ code formatting |
| cppcheck | latest | C++ static analysis |
| gcovr | latest | C++ coverage reporting |

---

## Implementation Notes for Template Team

1. **Template Structure**: Follow the patterns established in `simple-cpp23` and `nanobind-cpp23-poetry` templates for consistency.

2. **Post-generation Hook**: Create `hooks/post_gen_project.py` to:
   - Initialize git repository
   - Run `flutter pub get`
   - Install pre-commit hooks
   - Print next steps

3. **Version Pinning**: Pin all tool versions in `.pre-commit-config.yaml` using `--freeze` to ensure reproducibility.

4. **Documentation**: Include comprehensive README.md with:
   - Quick start guide
   - Available justfile commands
   - Testing instructions
   - Build instructions for Android

5. **FFI Generation**: Include `ffigen.yaml` configuration for automatic FFI binding generation from C headers.

6. **Android-Only Focus**: Since this template targets Android exclusively:
   - No iOS/macOS/Windows/Linux Flutter platform directories
   - Android NDK toolchain configuration is mandatory
   - Testing on Linux host with Android emulator or connected device
