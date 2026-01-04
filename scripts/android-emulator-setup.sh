#!/usr/bin/env bash
# android-emulator-setup.sh - Setup Android emulator for integration testing
# This script installs emulator components and creates an AVD for testing.
#
# Prerequisites:
# - mise must be installed and activated
# - android-sdk must be installed via mise (provides sdkmanager, avdmanager)
# - Java must be installed (required by sdkmanager)
# - KVM access for hardware acceleration (check with: ls -la /dev/kvm)
#
# Usage:
#   ./scripts/android-emulator-setup.sh           # Full setup
#   ./scripts/android-emulator-setup.sh --check   # Check if emulator is ready
#
# Sources:
# - https://developer.android.com/studio/run/emulator-commandline
# - https://developer.android.com/tools/avdmanager
# - https://developer.android.com/tools/sdkmanager

set -euo pipefail

# Configuration
ANDROID_API_LEVEL="36"
SYSTEM_IMAGE_TYPE="google_apis"
SYSTEM_IMAGE_ARCH="x86_64"
AVD_NAME="test_avd"

# Derived values
SYSTEM_IMAGE="system-images;android-${ANDROID_API_LEVEL};${SYSTEM_IMAGE_TYPE};${SYSTEM_IMAGE_ARCH}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

function log_info() {
	echo -e "${GREEN}[INFO]${NC} $1"
}

function log_warn() {
	echo -e "${YELLOW}[WARN]${NC} $1"
}

function log_error() {
	echo -e "${RED}[ERROR]${NC} $1"
}

function log_step() {
	echo -e "${BLUE}[STEP]${NC} $1"
}

function command_exists() {
	command -v "$1" &>/dev/null
}

# Check if mise is available
function check_mise() {
	if ! command_exists mise; then
		log_error "mise is not installed. Install it first:"
		log_error "  curl https://mise.run | sh"
		log_error "  # or: pipx install mise"
		return 1
	fi
}

# Check if sdkmanager is available
function check_sdkmanager() {
	if ! command_exists sdkmanager; then
		log_error "sdkmanager not found. Ensure android-sdk is installed via mise:"
		log_error "  mise install"
		log_error "  eval \"\$(mise activate bash)\""
		return 1
	fi
}

# Check if avdmanager is available
function check_avdmanager() {
	if ! command_exists avdmanager; then
		log_error "avdmanager not found. Ensure android-sdk is installed via mise:"
		log_error "  mise install"
		return 1
	fi
}

# Check if emulator is available
function check_emulator() {
	if ! command_exists emulator; then
		log_error "emulator not found. Run this script to install it."
		return 1
	fi
}

# Check if ANDROID_HOME is set
function check_android_home() {
	if [[ -z "${ANDROID_HOME:-}" ]]; then
		log_error "ANDROID_HOME is not set. Ensure mise is activated:"
		log_error "  eval \"\$(mise activate bash)\""
		return 1
	fi
	log_info "ANDROID_HOME: $ANDROID_HOME"
}

# Check KVM access for hardware acceleration
function check_kvm() {
	if [[ -e /dev/kvm ]]; then
		if [[ -r /dev/kvm && -w /dev/kvm ]]; then
			log_info "✓ KVM available (hardware acceleration enabled)"
			return 0
		else
			log_warn "KVM exists but not accessible. Add user to kvm group:"
			log_warn "  sudo usermod -aG kvm \$USER"
			log_warn "  # Then log out and back in"
			return 1
		fi
	else
		log_warn "KVM not available. Emulator will run without hardware acceleration (slow)"
		return 1
	fi
}

# Check if a component is installed
function is_component_installed() {
	local component="$1"
	sdkmanager --list_installed 2>/dev/null | grep -q "$component"
}

# Check if AVD exists
function avd_exists() {
	local avd_name="$1"
	avdmanager list avd 2>/dev/null | grep -q "Name: $avd_name"
}

# Install emulator components
function install_emulator_components() {
	local components=(
		"emulator"
		"$SYSTEM_IMAGE"
	)

	log_step "Installing emulator components..."

	for component in "${components[@]}"; do
		if is_component_installed "$component"; then
			log_info "✓ $component (already installed)"
		else
			log_info "Installing $component..."
			yes | sdkmanager "$component" || {
				log_error "Failed to install $component"
				return 1
			}
			log_info "✓ $component (installed)"
		fi
	done
}

# Accept licenses
function accept_licenses() {
	log_step "Accepting Android SDK licenses..."
	yes | sdkmanager --licenses >/dev/null 2>&1 || true
}

# Create AVD
function create_avd() {
	log_step "Creating AVD: $AVD_NAME"

	if avd_exists "$AVD_NAME"; then
		log_info "✓ AVD '$AVD_NAME' already exists"
		return 0
	fi

	log_info "Creating AVD with system image: $SYSTEM_IMAGE"

	# Create AVD without device profile first (more compatible)
	echo "no" | avdmanager create avd \
		--name "$AVD_NAME" \
		--package "$SYSTEM_IMAGE" \
		--force || {
		log_error "Failed to create AVD"
		return 1
	}

	log_info "✓ AVD '$AVD_NAME' created"
}

# Check if emulator setup is complete
function check_setup() {
	local all_ok=true

	log_step "Checking emulator setup..."

	# Check prerequisites
	if ! check_mise; then all_ok=false; fi
	if ! check_android_home; then all_ok=false; fi
	if ! check_sdkmanager; then all_ok=false; fi
	if ! check_avdmanager; then all_ok=false; fi

	# Check KVM (warning only)
	check_kvm || true

	# Check emulator installed
	if is_component_installed "emulator"; then
		log_info "✓ Emulator installed"
	else
		log_warn "✗ Emulator not installed"
		all_ok=false
	fi

	# Check system image installed
	if is_component_installed "$SYSTEM_IMAGE"; then
		log_info "✓ System image installed: $SYSTEM_IMAGE"
	else
		log_warn "✗ System image not installed: $SYSTEM_IMAGE"
		all_ok=false
	fi

	# Check AVD exists
	if avd_exists "$AVD_NAME"; then
		log_info "✓ AVD exists: $AVD_NAME"
	else
		log_warn "✗ AVD not found: $AVD_NAME"
		all_ok=false
	fi

	if $all_ok; then
		log_info "=== Emulator setup is complete ==="
		return 0
	else
		log_warn "=== Emulator setup incomplete. Run: just emulator-setup ==="
		return 1
	fi
}

# Print usage
function usage() {
	echo "Usage: $0 [--check]"
	echo ""
	echo "Options:"
	echo "  --check    Check if emulator is ready (don't install anything)"
	echo ""
	echo "Without options, performs full emulator setup."
}

# Main
function main() {
	if [[ "${1:-}" == "--check" ]]; then
		check_setup
		exit $?
	fi

	if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
		usage
		exit 0
	fi

	log_info "=== Android Emulator Setup ==="

	# Check prerequisites
	check_mise || exit 1
	check_android_home || exit 1
	check_sdkmanager || exit 1
	check_avdmanager || exit 1

	# Check KVM (warning only, continue anyway)
	check_kvm || log_warn "Continuing without KVM..."

	# Accept licenses first
	accept_licenses

	# Install components
	install_emulator_components || exit 1

	# Create AVD
	create_avd || exit 1

	log_info "=== Android Emulator Setup Complete ==="
	log_info ""
	log_info "To start the emulator:"
	log_info "  just emulator-start"
	log_info ""
	log_info "To run instrumented tests:"
	log_info "  just test-instrumented"
}

main "$@"
