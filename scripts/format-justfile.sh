#!/usr/bin/env bash
# Format justfiles using just --dump
# This reformats the justfile to a canonical format

set -euo pipefail

# Check if just is available
if ! command -v just &>/dev/null; then
	echo "just not found, skipping justfile formatting"
	exit 0
fi

# Format each justfile passed as argument
for file in "$@"; do
	if [[ -f "$file" ]]; then
		# Create a temporary file
		tmp_file="${file}.formatted"

		# Format the justfile
		if just --justfile "$file" --dump > "$tmp_file" 2>/dev/null; then
			# Only replace if formatting succeeded
			mv "$tmp_file" "$file"
		else
			# Remove temp file if formatting failed
			rm -f "$tmp_file"
			echo "Warning: Failed to format $file"
		fi
	fi
done
