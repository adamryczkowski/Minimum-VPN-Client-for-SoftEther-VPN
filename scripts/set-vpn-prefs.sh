#!/usr/bin/env bash
# set-vpn-prefs.sh - Set VPN preferences via adb for testing
#
# Usage: ./scripts/set-vpn-prefs.sh

set -euo pipefail

# Load environment variables from .env file
if [[ -f .env ]]; then
	set -a
	# shellcheck source=/dev/null
	source .env
	set +a
else
	echo "Error: .env file not found"
	exit 1
fi

PACKAGE="kittoku.mvc"
PREFS_FILE="shared_prefs/kittoku.mvc_preferences.xml"

# Create preferences XML content
# Note: SSL_PORT must be stored as a string because the app uses getString() to read it
PREFS_XML="<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
<string name=\"HOME_HOSTNAME\">${TEST_HOST}</string>
<string name=\"HOME_HUB\">${TEST_HUB:-VPN}</string>
<string name=\"HOME_USERNAME\">${TEST_USERNAME}</string>
<string name=\"HOME_PASSWORD\">${TEST_PASSWORD}</string>
<string name=\"SSL_PORT\">${TEST_PORT:-992}</string>
<boolean name=\"HOME_CONNECTOR\" value=\"false\" />
</map>"

echo "Setting VPN preferences..."
echo "Host: ${TEST_HOST}:${TEST_PORT:-992}"
echo "Hub: ${TEST_HUB:-VPN}"
echo "Username: ${TEST_USERNAME}"

# Stop the app first
adb shell am force-stop "$PACKAGE"

# Write preferences file using run-as
echo "$PREFS_XML" | adb shell "run-as $PACKAGE sh -c 'cat > $PREFS_FILE'"

# Verify the file was written
echo ""
echo "Verifying preferences..."
adb shell run-as "$PACKAGE" cat "$PREFS_FILE"

echo ""
echo "✅ VPN preferences set successfully!"
echo ""
echo "Now you can start the app and tap Connect."
