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
setup: install-hooks ensure-android-sdk
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

# Run all unit tests (excludes integration tests)
test:
    ./gradlew test

# Run unit tests with verbose output
test-verbose:
    ./gradlew test --info

# Run unit tests for debug build only
test-debug:
    ./gradlew testDebugUnitTest

# Run unit tests with code coverage report
test-coverage:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "🧪 Running tests with coverage..."
    ./gradlew testDebugUnitTest jacocoTestReport
    echo ""
    echo "📊 Coverage report generated:"
    echo "   HTML: app/build/reports/jacoco/jacocoTestReport/html/index.html"
    echo "   XML:  app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml"

# Verify coverage meets threshold (60%)
test-coverage-verify:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "🔍 Verifying coverage threshold..."
    ./gradlew jacocoTestCoverageVerification
    echo "✅ Coverage meets minimum threshold"

# Run integration tests (requires TEST_HOST, TEST_USERNAME, TEST_PASSWORD)
test-integration: test-env-check
    ./gradlew :app:testDebugUnitTest --tests "*IntegrationTest*"

# Run instrumented tests on connected device
test-instrumented:
    ./gradlew connectedAndroidTest

# Run all tests (unit + instrumented, excludes integration)
test-all: test test-instrumented

# Run complete test suite including integration tests
test-full: test-env-check test test-integration test-instrumented
    @echo "✅ Full test suite completed"

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

# Show test report in browser
test-report:
    #!/usr/bin/env bash
    set -euo pipefail
    report="app/build/reports/tests/testDebugUnitTest/index.html"
    if [ -f "$report" ]; then
        xdg-open "$report" 2>/dev/null || open "$report" 2>/dev/null || echo "Open: $report"
    else
        echo "❌ Test report not found. Run 'just test' first."
        exit 1
    fi

# Show coverage report in browser
coverage-report:
    #!/usr/bin/env bash
    set -euo pipefail
    report="app/build/reports/jacoco/jacocoTestReport/html/index.html"
    if [ -f "$report" ]; then
        xdg-open "$report" 2>/dev/null || open "$report" 2>/dev/null || echo "Open: $report"
    else
        echo "❌ Coverage report not found. Run 'just test-coverage' first."
        exit 1
    fi

# === Code Quality ===

# Run all linting (ktlint + detekt)
lint: lint-ktlint lint-detekt
    @echo "✅ All linting checks passed"

# Run ktlint check only
lint-ktlint:
    ./gradlew ktlintCheck

# Run detekt static analysis
lint-detekt:
    ./gradlew detekt

# Run ktlint format
format:
    ./gradlew ktlintFormat

# Generate detekt baseline (for suppressing existing issues)
detekt-baseline:
    ./gradlew detektBaseline

# Show detekt report in browser
detekt-report:
    #!/usr/bin/env bash
    set -euo pipefail
    report="app/build/reports/detekt/detekt.html"
    if [ -f "$report" ]; then
        xdg-open "$report" 2>/dev/null || open "$report" 2>/dev/null || echo "Open: $report"
    else
        echo "❌ Detekt report not found. Run 'just lint-detekt' first."
        exit 1
    fi

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

# === Emulator ===

# Setup Android emulator for integration testing (~2GB download)
emulator-setup: ensure-android-sdk
    bash scripts/android-emulator-setup.sh

# Check if emulator is ready
emulator-check:
    bash scripts/android-emulator-setup.sh --check

# Start emulator in background (headless mode for CI)
emulator-start:
    bash scripts/android-emulator-control.sh start

# Start emulator with GUI (for local development)
emulator-start-gui:
    bash scripts/android-emulator-control.sh start-gui

# Stop running emulator
emulator-stop:
    bash scripts/android-emulator-control.sh stop

# Wait for emulator to be fully booted
emulator-wait:
    bash scripts/android-emulator-control.sh wait

# Show emulator status
emulator-status:
    bash scripts/android-emulator-control.sh status

# List available AVDs
emulator-list:
    bash scripts/android-emulator-control.sh list

# Full emulator workflow: setup, start, wait
emulator-full: emulator-setup emulator-start emulator-wait
    @echo "✅ Emulator is ready for testing"

# Run instrumented tests on emulator (starts emulator if needed)
test-on-emulator: emulator-start emulator-wait test-instrumented emulator-stop
    @echo "✅ Instrumented tests completed"

# === Version Management ===

# Show current version
version:
    #!/usr/bin/env bash
    set -euo pipefail
    if [[ -f version.properties ]]; then
        major=$(grep "^VERSION_MAJOR=" version.properties | cut -d'=' -f2)
        minor=$(grep "^VERSION_MINOR=" version.properties | cut -d'=' -f2)
        patch=$(grep "^VERSION_PATCH=" version.properties | cut -d'=' -f2)
        code=$(grep "^VERSION_CODE=" version.properties | cut -d'=' -f2)
        echo "Version: ${major}.${minor}.${patch} (code: ${code})"
    else
        echo "version.properties not found"
        exit 1
    fi

# Bump patch version (1.0.0 -> 1.0.1)
version-bump-patch:
    bash scripts/version-bump.sh patch

# Bump minor version (1.0.0 -> 1.1.0)
version-bump-minor:
    bash scripts/version-bump.sh minor

# Bump major version (1.0.0 -> 2.0.0)
version-bump-major:
    bash scripts/version-bump.sh major

# === Keystore Management ===

# Full interactive keystore setup (generate + configure)
keystore-setup:
    bash scripts/keystore-manage.sh setup

# Generate a new release keystore
keystore-generate:
    bash scripts/keystore-manage.sh generate

# Configure keystore.properties interactively
keystore-setup-props:
    bash scripts/keystore-manage.sh setup-props

# Show keystore information
keystore-info:
    bash scripts/keystore-manage.sh info

# Verify keystore configuration
keystore-verify:
    bash scripts/keystore-manage.sh verify

# === Release ===

# Build release APK and AAB (requires keystore.properties)
release: release-check
    #!/usr/bin/env bash
    set -euo pipefail
    echo "🚀 Building release artifacts..."
    ./gradlew clean assembleRelease bundleRelease
    echo ""
    echo "✅ Release build complete!"
    echo ""
    echo "📦 Artifacts:"
    find app/build/outputs -name "*.apk" -o -name "*.aab" | head -10
    echo ""
    echo "📝 Next steps:"
    echo "   1. Test the release APK on a device"
    echo "   2. Update docs/RELEASE_NOTES_TEMPLATE.md"
    echo "   3. Upload to Google Play Console"

# Check release prerequisites
release-check:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "🔍 Checking release prerequisites..."
    errors=0
    warnings=0

    # Check keystore file
    if [[ ! -f release.keystore ]]; then
        echo "⚠️  release.keystore not found"
        echo "   Run 'just keystore-setup' to generate one"
        warnings=$((warnings + 1))
    else
        echo "✅ release.keystore found"
    fi

    # Check keystore.properties
    if [[ ! -f keystore.properties ]]; then
        echo "❌ keystore.properties not found"
        echo "   Run 'just keystore-setup' for interactive setup, or:"
        echo "   cp keystore.properties.example keystore.properties"
        errors=$((errors + 1))
    else
        # Check for placeholder values
        if grep -q "your_keystore_password" keystore.properties 2>/dev/null; then
            echo "⚠️  keystore.properties has placeholder passwords"
            echo "   Run 'just keystore-setup-props' to configure"
            warnings=$((warnings + 1))
        else
            echo "✅ keystore.properties configured"
        fi
    fi

    # Check version.properties
    if [[ ! -f version.properties ]]; then
        echo "❌ version.properties not found"
        errors=$((errors + 1))
    else
        echo "✅ version.properties found"
    fi

    # Check proguard rules
    if [[ ! -f app/proguard-rules.pro ]]; then
        echo "⚠️  proguard-rules.pro not found (using defaults)"
    else
        echo "✅ proguard-rules.pro found"
    fi

    echo ""
    if [[ $errors -gt 0 ]]; then
        echo "❌ Release check failed with $errors error(s)"
        echo ""
        echo "💡 Quick fix: Run 'just keystore-setup' for guided setup"
        exit 1
    elif [[ $warnings -gt 0 ]]; then
        echo "⚠️  Release check passed with $warnings warning(s)"
        echo "   The build may work but signing might not be configured correctly."
    else
        echo "✅ All release prerequisites met"
    fi

# Build release APK only
release-apk: release-check
    ./gradlew assembleRelease
    @echo "APK: app/build/outputs/apk/release/app-release.apk"

# Build release AAB (Android App Bundle) only
release-bundle: release-check
    ./gradlew bundleRelease
    @echo "AAB: app/build/outputs/bundle/release/app-release.aab"

# Generate SHA-256 checksums for release artifacts
release-checksums:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "📝 Generating checksums..."
    for file in app/build/outputs/apk/release/*.apk app/build/outputs/bundle/release/*.aab; do
        if [[ -f "$file" ]]; then
            sha256sum "$file"
        fi
    done

# Full release workflow: test, build, checksums
release-full: test lint release release-checksums
    @echo "✅ Full release workflow complete"
