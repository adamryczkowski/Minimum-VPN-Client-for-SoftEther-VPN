#!/usr/bin/env bash
# vpn-e2e-test.sh - End-to-end test for VPN connectivity verification using Maestro
#
# This script performs an E2E test to verify that the VPN connection works correctly
# using Maestro for UI automation and checking the public IP address.
#
# Prerequisites:
# - Android emulator running (just emulator-start-gui)
# - App installed (just install)
# - .env file with VPN credentials
# - Maestro installed (curl -fsSL "https://get.maestro.mobile.dev" | bash)
#
# Usage:
#   ./scripts/vpn-e2e-test.sh
#   ./scripts/vpn-e2e-test.sh --check-only    # Check prerequisites only
#   ./scripts/vpn-e2e-test.sh --skip-ip-check # Skip IP verification
#
# Sources:
# - https://docs.maestro.dev/
# - https://developer.android.com/studio/run/emulator-networking

set -euo pipefail

# Configuration
PACKAGE_NAME="kittoku.mvc"
# Maestro flows for connect and disconnect
MAESTRO_CONNECT_FLOW="maestro/vpn-connect-only.yaml"
MAESTRO_DISCONNECT_FLOW="maestro/vpn-disconnect-only.yaml"

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

function log_test() {
	echo -e "${BLUE}[TEST]${NC} $1"
}

function log_pass() {
	echo -e "${GREEN}[PASS]${NC} $1"
}

function log_fail() {
	echo -e "${RED}[FAIL]${NC} $1"
}

# Add Maestro to PATH
export PATH="$PATH:$HOME/.maestro/bin"

# Load environment variables from .env file
function load_env() {
	if [[ -f .env ]]; then
		log_info "Loading credentials from .env file"
		set -a
		# shellcheck source=/dev/null
		source .env
		set +a
	else
		log_error ".env file not found. Copy .env.example to .env and fill in values."
		exit 1
	fi

	# Validate required variables
	if [[ -z "${TEST_HOST:-}" ]]; then
		log_error "TEST_HOST not set in .env"
		exit 1
	fi
	if [[ -z "${TEST_USERNAME:-}" ]]; then
		log_error "TEST_USERNAME not set in .env"
		exit 1
	fi
	if [[ -z "${TEST_PASSWORD:-}" ]]; then
		log_error "TEST_PASSWORD not set in .env"
		exit 1
	fi

	log_info "VPN Server: ${TEST_HOST}:${TEST_PORT:-992}"
	log_info "Username: ${TEST_USERNAME}"
	log_info "Hub: ${TEST_HUB:-VPN}"
}

# Check if Maestro is installed
function check_maestro() {
	log_step "Checking Maestro installation..."

	if ! command -v maestro &>/dev/null; then
		log_error "Maestro is not installed. Install it with:"
		log_error "  curl -fsSL \"https://get.maestro.mobile.dev\" | bash"
		exit 1
	fi

	local version
	version=$(maestro --version 2>/dev/null | tail -1)
	log_info "Maestro version: $version"
}

# Check if emulator is running
function check_emulator() {
	log_step "Checking emulator status..."

	if ! adb devices 2>/dev/null | grep -q "emulator-"; then
		log_error "No emulator is running. Start it with: just emulator-start-gui"
		exit 1
	fi

	local device
	device=$(adb devices 2>/dev/null | grep "emulator-" | head -1 | cut -f1)
	log_info "Emulator found: $device"

	# Check if emulator is fully booted
	local boot_completed
	boot_completed=$(adb -s "$device" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
	if [[ "$boot_completed" != "1" ]]; then
		log_error "Emulator is not fully booted. Wait for it to complete."
		exit 1
	fi

	log_info "Emulator is fully booted"
}

# Check if app is installed
function check_app_installed() {
	log_step "Checking if app is installed..."

	if ! adb shell pm list packages 2>/dev/null | grep -q "$PACKAGE_NAME"; then
		log_error "App $PACKAGE_NAME is not installed. Run: just install"
		exit 1
	fi

	log_info "App is installed: $PACKAGE_NAME"
}

# Check if Maestro flow files exist
function check_maestro_flows() {
	log_step "Checking Maestro flow files..."

	if [[ ! -f "$MAESTRO_CONNECT_FLOW" ]]; then
		log_error "Maestro connect flow file not found: $MAESTRO_CONNECT_FLOW"
		exit 1
	fi

	if [[ ! -f "$MAESTRO_DISCONNECT_FLOW" ]]; then
		log_error "Maestro disconnect flow file not found: $MAESTRO_DISCONNECT_FLOW"
		exit 1
	fi

	log_info "Maestro flow files found"
}

# Set VPN preferences via adb
# This is more reliable than entering credentials via Maestro UI automation
function set_vpn_preferences() {
	log_step "Setting VPN preferences via adb..."

	local port="${TEST_PORT:-992}"
	local hub="${TEST_HUB:-VPN}"
	local skip_verify="${TEST_SKIP_CERT_VERIFY:-true}"
	local prefs_file="shared_prefs/kittoku.mvc_preferences.xml"

	# Create preferences XML content
	# Note: SSL_PORT must be stored as a string because the app uses getString() to read it
	# SSL_SKIP_VERIFY is set to true to skip certificate verification for self-signed certs
	local prefs_xml="<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
	<map>
	<string name=\"HOME_HOSTNAME\">${TEST_HOST}</string>
	<string name=\"HOME_HUB\">${hub}</string>
	<string name=\"HOME_USERNAME\">${TEST_USERNAME}</string>
	<string name=\"HOME_PASSWORD\">${TEST_PASSWORD}</string>
	<string name=\"SSL_PORT\">${port}</string>
	<boolean name=\"HOME_CONNECTOR\" value=\"false\" />
	<boolean name=\"SSL_SKIP_VERIFY\" value=\"${skip_verify}\" />
	</map>"

	# Stop the app first to ensure preferences are written correctly
	adb shell am force-stop "$PACKAGE_NAME"

	# Write preferences file using run-as
	echo "$prefs_xml" | adb shell "run-as $PACKAGE_NAME sh -c 'cat > $prefs_file'"

	log_info "VPN preferences configured: ${TEST_HOST}:${port} (skip cert verify: ${skip_verify})"
}

# Get public IP address from emulator using netcat (curl is not available on emulator)
function get_public_ip() {
	local ip
	# Use netcat to make raw HTTP request (curl is not available on Android emulator)
	# The request format: GET / HTTP/1.1\r\nHost: <host>\r\nConnection: close\r\n\r\n
	ip=$(adb shell "echo -e 'GET / HTTP/1.1\r\nHost: ident.me\r\nConnection: close\r\n\r\n' | nc ident.me 80 2>/dev/null | tail -1" | tr -d '\r\n') ||
	ip=$(adb shell "echo -e 'GET / HTTP/1.1\r\nHost: ifconfig.me\r\nConnection: close\r\n\r\n' | nc ifconfig.me 80 2>/dev/null | tail -1" | tr -d '\r\n') ||
	ip=""

	# Validate that we got a valid IP address (basic check)
	if [[ "$ip" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
		echo "$ip"
	else
		echo ""
	fi
}

# Get VPN IP address from the app's UI using accessibility hierarchy
# The app displays the assigned IP in the Connection Statistics card
function get_vpn_ip_from_ui() {
	# Use uiautomator to dump the UI hierarchy and extract the IP address
	# The IP is displayed after "IP Address" label in the stats card
	local hierarchy
	hierarchy=$(adb shell uiautomator dump /dev/tty 2>/dev/null | tr -d '\r')

	# Extract IP address pattern that appears after "IP Address" text
	# The format in the UI is: IP Address ... <IP>
	local ip
	ip=$(echo "$hierarchy" | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+' | head -1)

	echo "$ip"
}

# Get VPN server hostname from the app's UI
function get_vpn_server_from_ui() {
	local hierarchy
	hierarchy=$(adb shell uiautomator dump /dev/tty 2>/dev/null | tr -d '\r')

	# Look for server hostname pattern (IP or domain)
	# This appears after "Server" label
	local server
	server=$(echo "$hierarchy" | grep -oE 'text="[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+"' | head -1 | sed 's/text="//;s/"//')

	echo "$server"
}

# Get VPN network info from Android connectivity service
# Returns: interface_name|ip_address|mac_address (pipe-separated)
function get_vpn_network_info() {
	# Look for VPN network in connectivity dump
	local vpn_info
	vpn_info=$(adb shell dumpsys connectivity 2>/dev/null | grep -A5 "TRANSPORT_VPN" | head -10)

	if [[ -z "$vpn_info" ]]; then
		echo "||"
		return
	fi

	# Extract interface name from LinkProperties
	local iface
	iface=$(echo "$vpn_info" | grep -o "InterfaceName: [^ ]*" | head -1 | cut -d' ' -f2 | tr -d '\r\n')

	# Extract IP address from LinkAddresses
	local ip
	ip=$(echo "$vpn_info" | grep -o "LinkAddresses: \[[^]]*\]" | head -1 | grep -oE "[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+/[0-9]+" | head -1 | cut -d'/' -f1 | tr -d '\r\n')

	# VPN interfaces typically don't have MAC addresses (L3 tunnel)
	local mac="N/A (L3 tunnel)"

	echo "${iface:-}|${ip:-}|${mac}"
}

# Get VPN interface name (tun0, tun1, etc.)
function get_vpn_interface() {
	# Look for tun interfaces that are UP with an IP address
	local iface
	iface=$(adb shell "ip addr show 2>/dev/null" | grep -E "^[0-9]+: tun[0-9]+" | head -1 | awk -F': ' '{print $2}' | tr -d '\r\n')

	# If no tun interface, try to get from connectivity service
	if [[ -z "$iface" ]]; then
		iface=$(get_vpn_network_info | cut -d'|' -f1)
	fi

	echo "$iface"
}

# Get VPN internal IP address
function get_vpn_ip() {
	local iface="$1"
	local ip=""

	# First try to get from UI (most reliable when connected)
	ip=$(get_vpn_ip_from_ui)

	# If not from UI, try from interface
	if [[ -z "$ip" && -n "$iface" ]]; then
		ip=$(adb shell "ip addr show $iface 2>/dev/null" | grep "inet " | awk '{print $2}' | cut -d'/' -f1 | tr -d '\r\n')
	fi

	# If still no IP, try connectivity service
	if [[ -z "$ip" ]]; then
		ip=$(get_vpn_network_info | cut -d'|' -f2)
	fi

	echo "$ip"
}

# Get VPN interface MAC address
function get_vpn_mac() {
	local iface="$1"
	if [[ -n "$iface" ]]; then
		# Note: tun interfaces typically don't have MAC addresses (they're layer 3)
		# But we can try to get it anyway
		local mac
		mac=$(adb shell "ip link show $iface 2>/dev/null" | grep "link/" | awk '{print $2}' | tr -d '\r\n')
		# If it's all zeros or empty, return N/A
		if [[ -z "$mac" || "$mac" == "00:00:00:00:00:00" ]]; then
			echo "N/A (L3 tunnel)"
		else
			echo "$mac"
		fi
	else
		echo "N/A (L3 tunnel)"
	fi
}

# Run Maestro connect flow
function run_maestro_connect() {
	log_step "Running Maestro connect flow..."

	# Create screenshots directory
	mkdir -p screenshots

	# Set defaults
	local port="${TEST_PORT:-992}"
	local hub="${TEST_HUB:-VPN}"

	# Run Maestro connect flow
	if maestro test \
		-e TEST_HOST="$TEST_HOST" \
		-e TEST_PORT="$port" \
		-e TEST_HUB="$hub" \
		-e TEST_USERNAME="$TEST_USERNAME" \
		-e TEST_PASSWORD="$TEST_PASSWORD" \
		"$MAESTRO_CONNECT_FLOW"; then
		log_pass "VPN connected successfully!"
		return 0
	else
		log_fail "VPN connection failed!"
		return 1
	fi
}

# Run Maestro disconnect flow
function run_maestro_disconnect() {
	log_step "Running Maestro disconnect flow..."

	# Run Maestro disconnect flow
	if maestro test "$MAESTRO_DISCONNECT_FLOW"; then
		log_pass "VPN disconnected successfully!"
		return 0
	else
		log_fail "VPN disconnection failed!"
		return 1
	fi
}

# Print test summary
function print_summary() {
	local ip_before="$1"
	local vpn_public_ip="$2"
	local vpn_interface="$3"
	local vpn_internal_ip="$4"
	local vpn_mac="$5"
	local maestro_result="$6"

	echo ""
	echo "=========================================="
	echo "         E2E VPN Test Summary"
	echo "=========================================="
	echo ""
	echo "VPN Server:       ${TEST_HOST}:${TEST_PORT:-992}"
	echo "Hub:              ${TEST_HUB:-VPN}"
	echo "Username:         ${TEST_USERNAME}"
	echo ""
	echo "--- Network Info (Before VPN) ---"
	echo "Public IP:        ${ip_before:-N/A}"
	echo ""
	echo "--- Network Info (VPN Connected) ---"
	echo "Public IP:        ${vpn_public_ip:-N/A}"
	echo "VPN Interface:    ${vpn_interface:-N/A}"
	echo "VPN Internal IP:  ${vpn_internal_ip:-N/A}"
	echo "VPN MAC Address:  ${vpn_mac:-N/A}"
	echo ""
	echo "Maestro Test:     ${maestro_result}"
	echo ""

	if [[ "$maestro_result" == "PASS" ]]; then
		if [[ -n "$ip_before" && -n "$vpn_public_ip" && "$ip_before" != "$vpn_public_ip" ]]; then
			log_pass "VPN is working! Public IP changed from $ip_before to $vpn_public_ip"
		elif [[ -n "$vpn_public_ip" ]]; then
			log_info "VPN connected. Public IP through VPN: $vpn_public_ip"
		fi
		if [[ -n "$vpn_internal_ip" ]]; then
			log_info "VPN internal IP assigned: $vpn_internal_ip"
		fi
		echo ""
		echo "=========================================="
		echo "         TEST PASSED ✓"
		echo "=========================================="
		return 0
	else
		log_fail "VPN test failed"
		echo ""
		echo "=========================================="
		echo "         TEST FAILED ✗"
		echo "=========================================="
		return 1
	fi
}

# Main test function
function run_test() {
	local ip_before=""
	local vpn_public_ip=""
	local vpn_interface=""
	local vpn_internal_ip=""
	local vpn_mac=""
	local maestro_result="FAIL"
	local skip_ip_check="${1:-false}"

	echo ""
	echo "=========================================="
	echo "     SoftEther VPN E2E Test (Maestro)"
	echo "=========================================="
	echo ""

	# Load credentials
	load_env

	# Pre-flight checks
	check_maestro
	check_emulator
	check_app_installed
	check_maestro_flows

	# Configure VPN credentials via adb (more reliable than UI automation)
	set_vpn_preferences

	# Step 1: Get public IP before VPN (optional)
	if [[ "$skip_ip_check" != "true" ]]; then
		log_test "Step 1: Getting public IP before VPN connection..."
		ip_before=$(get_public_ip)
		if [[ -n "$ip_before" ]]; then
			log_info "Public IP before VPN: $ip_before"
		else
			log_warn "Could not determine public IP"
		fi
	fi

	# Step 2: Connect to VPN
	log_test "Step 2: Connecting to VPN..."
	if run_maestro_connect; then
		maestro_result="PASS"

		# Step 3: Capture VPN info while connected
		log_test "Step 3: Capturing VPN network info..."

		# Give the VPN a moment to fully establish
		sleep 2

		# Get VPN interface info
		vpn_interface=$(get_vpn_interface)
		if [[ -n "$vpn_interface" ]]; then
			log_info "VPN interface: $vpn_interface"
			vpn_internal_ip=$(get_vpn_ip "$vpn_interface")
			vpn_mac=$(get_vpn_mac "$vpn_interface")
			log_info "VPN internal IP: ${vpn_internal_ip:-N/A}"
			log_info "VPN MAC: ${vpn_mac:-N/A}"
		else
			log_warn "Could not detect VPN interface"
		fi

		# Get public IP through VPN
		if [[ "$skip_ip_check" != "true" ]]; then
			vpn_public_ip=$(get_public_ip)
			if [[ -n "$vpn_public_ip" ]]; then
				log_info "Public IP through VPN: $vpn_public_ip"
			fi
		fi

		# Step 4: Disconnect from VPN
		log_test "Step 4: Disconnecting from VPN..."
		if ! run_maestro_disconnect; then
			log_warn "Disconnect flow had issues, but connection test passed"
		fi
	fi

	# Print summary
	print_summary "$ip_before" "$vpn_public_ip" "$vpn_interface" "$vpn_internal_ip" "$vpn_mac" "$maestro_result"
}

# Print usage
function usage() {
	echo "Usage: $0 [options]"
	echo ""
	echo "Options:"
	echo "  --help, -h       Show this help message"
	echo "  --check-only     Only check prerequisites, don't run test"
	echo "  --skip-ip-check  Skip IP address verification"
	echo ""
	echo "This script performs an E2E test to verify VPN connectivity using Maestro."
	echo "It automates the UI interaction and verifies the VPN connection works."
}

# Main
function main() {
	case "${1:-}" in
		--help | -h)
			usage
			exit 0
			;;
		--check-only)
			load_env
			check_maestro
			check_emulator
			check_app_installed
			check_maestro_flows
			log_pass "All prerequisites met!"
			exit 0
			;;
		--skip-ip-check)
			run_test "true"
			;;
		*)
			run_test "false"
			;;
	esac
}

main "$@"
