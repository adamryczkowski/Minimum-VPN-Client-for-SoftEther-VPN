#!/usr/bin/env bash
# Version bump script for SoftEther Connect
# Usage: ./scripts/version-bump.sh [major|minor|patch]

set -euo pipefail

VERSION_FILE="version.properties"

if [[ ! -f "$VERSION_FILE" ]]; then
	echo "Error: $VERSION_FILE not found"
	exit 1
fi

# Read current version
VERSION_MAJOR=$(grep "^VERSION_MAJOR=" "$VERSION_FILE" | cut -d'=' -f2)
VERSION_MINOR=$(grep "^VERSION_MINOR=" "$VERSION_FILE" | cut -d'=' -f2)
VERSION_PATCH=$(grep "^VERSION_PATCH=" "$VERSION_FILE" | cut -d'=' -f2)
VERSION_CODE=$(grep "^VERSION_CODE=" "$VERSION_FILE" | cut -d'=' -f2)

CURRENT_VERSION="${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
echo "Current version: $CURRENT_VERSION (code: $VERSION_CODE)"

# Determine bump type
BUMP_TYPE="${1:-patch}"

case "$BUMP_TYPE" in
	major)
		VERSION_MAJOR=$((VERSION_MAJOR + 1))
		VERSION_MINOR=0
		VERSION_PATCH=0
		;;
	minor)
		VERSION_MINOR=$((VERSION_MINOR + 1))
		VERSION_PATCH=0
		;;
	patch)
		VERSION_PATCH=$((VERSION_PATCH + 1))
		;;
	*)
		echo "Usage: $0 [major|minor|patch]"
		echo "  major: 1.0.0 -> 2.0.0 (breaking changes)"
		echo "  minor: 1.0.0 -> 1.1.0 (new features)"
		echo "  patch: 1.0.0 -> 1.0.1 (bug fixes)"
		exit 1
		;;
esac

# Always increment version code
VERSION_CODE=$((VERSION_CODE + 1))

NEW_VERSION="${VERSION_MAJOR}.${VERSION_MINOR}.${VERSION_PATCH}"
echo "New version: $NEW_VERSION (code: $VERSION_CODE)"

# Update version.properties
cat > "$VERSION_FILE" << EOF
# Version properties for SoftEther Connect
# This file is used by the build system to manage version numbers.
#
# Version Format: MAJOR.MINOR.PATCH
# - MAJOR: Breaking changes or major new features
# - MINOR: New features, backward compatible
# - PATCH: Bug fixes, backward compatible
#
# versionCode: Integer version code for Android (must increase with each release)
#
# To bump version, use:
#   just version-bump-patch  (1.0.0 -> 1.0.1)
#   just version-bump-minor  (1.0.0 -> 1.1.0)
#   just version-bump-major  (1.0.0 -> 2.0.0)

VERSION_MAJOR=$VERSION_MAJOR
VERSION_MINOR=$VERSION_MINOR
VERSION_PATCH=$VERSION_PATCH
VERSION_CODE=$VERSION_CODE
EOF

echo "✅ Version updated: $CURRENT_VERSION -> $NEW_VERSION"
echo "   Version code: $VERSION_CODE"
