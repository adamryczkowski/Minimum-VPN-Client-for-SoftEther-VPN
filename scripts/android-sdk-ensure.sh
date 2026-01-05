#!/usr/bin/env bash
# android-sdk-ensure.sh - Ensure Android SDK components are installed
# This script is called by the justfile to install Android SDK components.
#
# CACHING BEHAVIOR:
# - All SDK components are stored in ANDROID_HOME (managed by mise)
# - Components are shared across all projects using the same mise android-sdk version
# - The script checks if each component is already installed before downloading
# - For NDK, it also verifies the installation is healthy (critical binaries exist)
#
# Prerequisites:
# - mise must be installed and activated
# - android-sdk must be installed via mise (provides sdkmanager)
# - Java must be installed (required by sdkmanager)

set -euo pipefail

# Configuration
ANDROID_PLATFORM="android-36"
BUILD_TOOLS_VERSION="35.0.0"
NDK_VERSION="27.0.12077973"

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

# Check if NDK installation is healthy
# Returns 0 if healthy, 1 if broken or missing
function is_ndk_healthy() {
	local ndk_version="$1"
	local ndk_path="${ANDROID_HOME}/ndk/${ndk_version}"
	local llvm_bin="${ndk_path}/toolchains/llvm/prebuilt/linux-x86_64/bin"

	# Check if NDK directory exists
	if [[ ! -d "$ndk_path" ]]; then
		log_warn "NDK directory not found: $ndk_path"
		return 1
	fi

	# Check if critical LLVM binaries exist and are executable
	# These are required for stripping debug symbols during release builds
	local critical_binaries=(
		"llvm-objcopy"
		"llvm-ar"
		"clang"
	)

	for binary in "${critical_binaries[@]}"; do
		local binary_path="${llvm_bin}/${binary}"
		if [[ -L "$binary_path" ]]; then
			# It's a symlink - check if target exists
			local target
			target=$(readlink -f "$binary_path" 2>/dev/null || echo "")
			if [[ -z "$target" ]] || [[ ! -f "$target" ]]; then
				log_warn "NDK binary is a broken symlink: $binary_path"
				return 1
			fi
		elif [[ ! -f "$binary_path" ]]; then
			log_warn "NDK binary not found: $binary_path"
			return 1
		fi

		if [[ ! -x "$binary_path" ]]; then
			log_warn "NDK binary not executable: $binary_path"
			return 1
		fi
	done

	return 0
}

# Reinstall NDK (uninstall then install)
function reinstall_ndk() {
	local ndk_component="$1"

	log_warn "Reinstalling NDK due to corrupted installation..."

	# Uninstall first
	log_info "Uninstalling $ndk_component..."
	yes | sdkmanager --uninstall "$ndk_component" 2>/dev/null || true

	# Install fresh
	log_info "Installing $ndk_component..."
	yes | sdkmanager "$ndk_component" || {
		log_error "Failed to install $ndk_component"
		return 1
	}

	log_info "✓ $ndk_component (reinstalled)"
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

# Install and verify NDK
function install_ndk() {
	local ndk_component="ndk;${NDK_VERSION}"

	log_info "Checking NDK..."

	if is_component_installed "$ndk_component"; then
		# NDK is installed, but check if it's healthy
		if is_ndk_healthy "$NDK_VERSION"; then
			log_info "✓ $ndk_component (already installed and healthy)"
			return 0
		else
			# NDK is broken, reinstall it
			reinstall_ndk "$ndk_component" || return 1

			# Verify the reinstalled NDK is healthy
			if ! is_ndk_healthy "$NDK_VERSION"; then
				log_error "NDK reinstallation failed - still unhealthy"
				return 1
			fi
		fi
	else
		# NDK not installed, install it
		log_info "Installing $ndk_component..."
		yes | sdkmanager "$ndk_component" || {
			log_error "Failed to install $ndk_component"
			return 1
		}
		log_info "✓ $ndk_component (installed)"

		# Verify the installation is healthy
		if ! is_ndk_healthy "$NDK_VERSION"; then
			log_error "NDK installation is unhealthy after install"
			return 1
		fi
	fi
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

	# Install and verify NDK
	install_ndk || exit 1

	log_info "=== Android SDK Setup Complete ==="
	log_info "ANDROID_HOME: $ANDROID_HOME"
	log_info "Platform: $ANDROID_PLATFORM"
	log_info "Build Tools: $BUILD_TOOLS_VERSION"
	log_info "NDK: $NDK_VERSION"
}

main "$@"
