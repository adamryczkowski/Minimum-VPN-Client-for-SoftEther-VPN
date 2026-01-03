#!/usr/bin/env bash
# android-sdk-ensure.sh - Ensure Android SDK components are installed
# This script is called by the justfile to install Android SDK components.
#
# CACHING BEHAVIOR:
# - All SDK components are stored in ANDROID_HOME (managed by mise)
# - Components are shared across all projects using the same mise android-sdk version
# - The script checks if each component is already installed before downloading
#
# Prerequisites:
# - mise must be installed and activated
# - android-sdk must be installed via mise (provides sdkmanager)
# - Java must be installed (required by sdkmanager)

set -euo pipefail

# Configuration
ANDROID_PLATFORM="android-36"
BUILD_TOOLS_VERSION="35.0.0"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
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

# Check if ANDROID_HOME is set
function check_android_home() {
	if [[ -z "${ANDROID_HOME:-}" ]]; then
		log_error "ANDROID_HOME is not set. Ensure mise is activated:"
		log_error "  eval \"\$(mise activate bash)\""
		return 1
	fi
	log_info "ANDROID_HOME: $ANDROID_HOME"
}

# Check if a component is installed
function is_component_installed() {
	local component="$1"
	sdkmanager --list_installed 2>/dev/null | grep -q "$component"
}

# Install SDK components
function install_sdk_components() {
	local components=(
		"platforms;${ANDROID_PLATFORM}"
		"build-tools;${BUILD_TOOLS_VERSION}"
		"platform-tools"
	)

	log_info "Checking SDK components..."

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
	log_info "Accepting Android SDK licenses..."
	yes | sdkmanager --licenses >/dev/null 2>&1 || true
}

# Main
function main() {
	log_info "=== Android SDK Setup ==="

	# Check prerequisites
	check_mise || exit 1
	check_android_home || exit 1
	check_sdkmanager || exit 1

	# Accept licenses first
	accept_licenses

	# Install components
	install_sdk_components || exit 1

	log_info "=== Android SDK Setup Complete ==="
	log_info "ANDROID_HOME: $ANDROID_HOME"
	log_info "Platform: $ANDROID_PLATFORM"
	log_info "Build Tools: $BUILD_TOOLS_VERSION"
}

main "$@"
