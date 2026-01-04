#!/usr/bin/env bash
# android-emulator-control.sh - Start, stop, and manage Android emulator
#
# Usage:
#   ./scripts/android-emulator-control.sh start     # Start emulator in background
#   ./scripts/android-emulator-control.sh stop      # Stop running emulator
#   ./scripts/android-emulator-control.sh status    # Check emulator status
#   ./scripts/android-emulator-control.sh wait      # Wait for emulator to be ready
#   ./scripts/android-emulator-control.sh list      # List available AVDs
#
# Sources:
# - https://developer.android.com/studio/run/emulator-commandline

set -euo pipefail

# Configuration
AVD_NAME="${AVD_NAME:-test_avd}"
EMULATOR_TIMEOUT="${EMULATOR_TIMEOUT:-120}"  # seconds to wait for boot

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

# Check prerequisites
function check_prerequisites() {
	if ! command_exists emulator; then
		log_error "emulator not found. Run: just emulator-setup"
		exit 1
	fi

	if ! command_exists adb; then
		log_error "adb not found. Ensure android-sdk is installed via mise"
		exit 1
	fi
}

# Check if emulator is running
function is_emulator_running() {
	adb devices 2>/dev/null | grep -q "emulator-"
}

# Get emulator device ID
function get_emulator_device() {
	adb devices 2>/dev/null | grep "emulator-" | head -1 | cut -f1
}

# Check if emulator is fully booted
function is_emulator_booted() {
	local device
	device=$(get_emulator_device)
	if [[ -z "$device" ]]; then
		return 1
	fi

	local boot_completed
	boot_completed=$(adb -s "$device" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
	[[ "$boot_completed" == "1" ]]
}

# List available AVDs
function list_avds() {
	log_step "Available AVDs:"
	emulator -list-avds 2>/dev/null || {
		log_error "Failed to list AVDs"
		return 1
	}
}

# Start emulator
function start_emulator() {
	check_prerequisites

	if is_emulator_running; then
		log_info "Emulator is already running"
		return 0
	fi

	# Check if AVD exists
	if ! emulator -list-avds 2>/dev/null | grep -q "^${AVD_NAME}$"; then
		log_error "AVD '$AVD_NAME' not found. Run: just emulator-setup"
		log_info "Available AVDs:"
		emulator -list-avds 2>/dev/null || true
		exit 1
	fi

	log_step "Starting emulator with AVD: $AVD_NAME"

	# Start emulator in background with headless mode for CI
	# -no-window: Run without GUI (for CI/headless)
	# -no-audio: Disable audio
	# -no-boot-anim: Skip boot animation for faster startup
	# -gpu swiftshader_indirect: Software rendering (works without GPU)
	nohup emulator -avd "$AVD_NAME" \
		-no-audio \
		-no-boot-anim \
		-gpu swiftshader_indirect \
		>/tmp/emulator.log 2>&1 &

	local emulator_pid=$!
	echo "$emulator_pid" > /tmp/emulator.pid

	log_info "Emulator started with PID: $emulator_pid"
	log_info "Log file: /tmp/emulator.log"
	log_info ""
	log_info "To wait for boot: just emulator-wait"
	log_info "To stop: just emulator-stop"
}

# Start emulator with GUI (for local development)
function start_emulator_gui() {
	check_prerequisites

	if is_emulator_running; then
		log_info "Emulator is already running"
		return 0
	fi

	# Check if AVD exists
	if ! emulator -list-avds 2>/dev/null | grep -q "^${AVD_NAME}$"; then
		log_error "AVD '$AVD_NAME' not found. Run: just emulator-setup"
		exit 1
	fi

	log_step "Starting emulator with GUI: $AVD_NAME"

	# Start emulator with GUI
	nohup emulator -avd "$AVD_NAME" \
		-no-boot-anim \
		>/tmp/emulator.log 2>&1 &

	local emulator_pid=$!
	echo "$emulator_pid" > /tmp/emulator.pid

	log_info "Emulator started with PID: $emulator_pid"
}

# Stop emulator
function stop_emulator() {
	if ! is_emulator_running; then
		log_info "No emulator is running"
		return 0
	fi

	log_step "Stopping emulator..."

	local device
	device=$(get_emulator_device)

	if [[ -n "$device" ]]; then
		adb -s "$device" emu kill 2>/dev/null || true
	fi

	# Wait for emulator to stop
	local timeout=30
	local count=0
	while is_emulator_running && [[ $count -lt $timeout ]]; do
		sleep 1
		((count++))
	done

	if is_emulator_running; then
		log_warn "Emulator didn't stop gracefully, killing process..."
		if [[ -f /tmp/emulator.pid ]]; then
			kill -9 "$(cat /tmp/emulator.pid)" 2>/dev/null || true
			rm -f /tmp/emulator.pid
		fi
		pkill -9 -f "emulator.*-avd" 2>/dev/null || true
	fi

	log_info "Emulator stopped"
}

# Wait for emulator to be ready
function wait_for_emulator() {
	check_prerequisites

	if ! is_emulator_running; then
		log_error "No emulator is running. Start it first: just emulator-start"
		exit 1
	fi

	log_step "Waiting for emulator to boot (timeout: ${EMULATOR_TIMEOUT}s)..."

	local count=0
	while ! is_emulator_booted && [[ $count -lt $EMULATOR_TIMEOUT ]]; do
		sleep 2
		((count+=2))
		echo -n "."
	done
	echo ""

	if is_emulator_booted; then
		log_info "✓ Emulator is ready!"

		# Wait a bit more for system to stabilize
		log_info "Waiting for system to stabilize..."
		sleep 5

		# Unlock screen
		local device
		device=$(get_emulator_device)
		adb -s "$device" shell input keyevent 82 2>/dev/null || true

		log_info "Emulator is fully ready for testing"
		return 0
	else
		log_error "Emulator failed to boot within ${EMULATOR_TIMEOUT} seconds"
		log_error "Check log: cat /tmp/emulator.log"
		return 1
	fi
}

# Show emulator status
function show_status() {
	log_step "Emulator Status"

	if is_emulator_running; then
		local device
		device=$(get_emulator_device)
		log_info "✓ Emulator is running: $device"

		if is_emulator_booted; then
			log_info "✓ Emulator is fully booted"
		else
			log_warn "⏳ Emulator is still booting..."
		fi
	else
		log_info "✗ No emulator is running"
	fi

	echo ""
	log_step "Connected devices:"
	adb devices 2>/dev/null || true
}

# Print usage
function usage() {
	echo "Usage: $0 <command>"
	echo ""
	echo "Commands:"
	echo "  start      Start emulator in background (headless mode)"
	echo "  start-gui  Start emulator with GUI"
	echo "  stop       Stop running emulator"
	echo "  status     Check emulator status"
	echo "  wait       Wait for emulator to be ready"
	echo "  list       List available AVDs"
	echo ""
	echo "Environment variables:"
	echo "  AVD_NAME          AVD to use (default: test_avd)"
	echo "  EMULATOR_TIMEOUT  Boot timeout in seconds (default: 120)"
}

# Main
function main() {
	case "${1:-}" in
		start)
			start_emulator
			;;
		start-gui)
			start_emulator_gui
			;;
		stop)
			stop_emulator
			;;
		status)
			show_status
			;;
		wait)
			wait_for_emulator
			;;
		list)
			list_avds
			;;
		--help|-h|"")
			usage
			;;
		*)
			log_error "Unknown command: $1"
			usage
			exit 1
			;;
	esac
}

main "$@"
