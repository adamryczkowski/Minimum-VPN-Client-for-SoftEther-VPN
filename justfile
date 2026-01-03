# SoftEther Connect - Android VPN Client
# See: https://just.systems/man/en

set shell := ["bash", "-eu", "-o", "pipefail", "-c"]
set dotenv-load := true

default: help

# Show available commands
help:
    @just --list

# === Setup ===

# Full setup: mise tools + Android SDK + pre-commit hooks
setup-full: mise-install ensure-android-sdk install-hooks
    @echo "✅ Full development environment ready!"

# Setup development environment (assumes tools already installed)
setup: install-hooks
    ./gradlew dependencies
    @echo "Development environment ready!"

# Install pre-commit hooks
install-hooks:
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -d .git ]; then
        pre-commit install --install-hooks
    else
        echo "Not a git repository; skipping pre-commit hook install"
    fi

# === Mise (Tool Version Management) ===

# Install mise tools (Java, Android SDK)
mise-install:
    #!/usr/bin/env bash
    set -euo pipefail
    if ! command -v mise &>/dev/null; then
        echo "❌ mise not found. Install it first:"
        echo "   curl https://mise.run | sh"
        echo "   # or: pipx install mise"
        exit 1
    fi
    echo "📦 Installing mise tools (Java, Android SDK)..."
    mise install

# Install Android SDK components (NDK, platform, build-tools)
mise-android-sdk:
    #!/usr/bin/env bash
    set -euo pipefail
    if [ -f "scripts/android-sdk-ensure.sh" ]; then
        bash scripts/android-sdk-ensure.sh
    else
        echo "Warning: android-sdk-ensure.sh not found"
    fi

# Ensure Android SDK is installed
[private]
ensure-android-sdk: mise-android-sdk

# Check mise tool versions and health
mise-doctor:
    mise doctor
    mise list

# === Build ===

# Build debug APK
build:
    ./gradlew assembleDebug

# Build release APK
build-release:
    ./gradlew assembleRelease

# Clean build artifacts
clean:
    ./gradlew clean

# === Test ===

# Run all unit tests
test:
    ./gradlew test

# Run unit tests with verbose output
test-verbose:
    ./gradlew test --info

# Run instrumented tests on connected device
test-instrumented:
    ./gradlew connectedAndroidTest

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
        echo "❌ Missing environment variables:$missing"
        echo "Copy .env.example to .env and fill in values"
        exit 1
    fi
    echo "✅ Test environment configured"

# === Code Quality ===

# Run ktlint check
lint:
    ./gradlew ktlintCheck

# Run ktlint format
format:
    ./gradlew ktlintFormat

# Run all formatting hooks (ktlint + pre-commit formatters)
format-all: format
    @echo "==> Running formatting hooks"
    @echo "Running end-of-file-fixer..."
    @pre-commit run end-of-file-fixer --all-files || true
    @echo "Running trailing-whitespace..."
    @pre-commit run trailing-whitespace --all-files || true
    @echo "Running mixed-line-ending..."
    @pre-commit run mixed-line-ending --all-files || true
    @echo "Running fix-byte-order-marker..."
    @pre-commit run fix-byte-order-marker --all-files || true
    @echo "Running yamlfix..."
    @pre-commit run yamlfix --all-files || true
    @echo "Running beautysh..."
    @pre-commit run beautysh --all-files || true
    @echo "Running format-xml..."
    @pre-commit run format-xml --all-files || true
    @echo "Running markdownlint-cli2..."
    @pre-commit run markdownlint-cli2 --all-files || true
    @echo "Running format-justfiles..."
    @pre-commit run format-justfiles --all-files || true
    @echo "Running typos..."
    @pre-commit run typos --all-files || true
    @echo "==> Formatting complete"

# Run all pre-commit hooks
validate:
    pre-commit run --all-files

# === Install ===

# Install debug APK to connected device
install:
    ./gradlew installDebug

# Install release APK to connected device
install-release:
    ./gradlew installRelease

# Build and run on connected device
run: install
    adb shell am start -n com.softether.connect/.MainActivity

# === Doctor ===

# Check development environment health
doctor:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "=== Java Version ==="
    java -version 2>&1 | head -3 || echo "Java not found"
    echo ""
    echo "=== Gradle Version ==="
    ./gradlew --version | head -5 || echo "Gradle wrapper not found"
    echo ""
    echo "=== Android SDK ==="
    if [ -n "${ANDROID_HOME:-}" ]; then
        echo "ANDROID_HOME: $ANDROID_HOME"
        ls -la "$ANDROID_HOME/platforms/" 2>/dev/null || echo "No platforms installed"
    else
        echo "ANDROID_HOME not set"
    fi
    echo ""
    echo "=== Connected Devices ==="
    adb devices 2>/dev/null || echo "adb not found"
