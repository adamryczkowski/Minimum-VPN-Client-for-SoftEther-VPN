#!/usr/bin/env bash
# Format XML files using xmllint
# This script is optional - if xmllint is not installed, it will skip formatting

set -euo pipefail

# Check if xmllint is available
if ! command -v xmllint &>/dev/null; then
	echo "xmllint not found, skipping XML formatting"
	echo "Install with: sudo apt-get install libxml2-utils"
	exit 0
fi

# Format each XML file passed as argument
for file in "$@"; do
	if [[ -f "$file" ]]; then
		# Create a temporary file
		tmp_file="${file}.tmp"

		# Format the XML file
		if xmllint --format "$file" > "$tmp_file" 2>/dev/null; then
			# Only replace if formatting succeeded
			mv "$tmp_file" "$file"
		else
			# Remove temp file if formatting failed
			rm -f "$tmp_file"
			echo "Warning: Failed to format $file"
		fi
	fi
done
