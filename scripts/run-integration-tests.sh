#!/bin/bash
# Run integration tests with environment variables from .env file

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Load environment variables from .env file
if [ -f "$PROJECT_DIR/.env" ]; then
    echo "Loading environment from .env file..."
    set -a
    source "$PROJECT_DIR/.env"
    set +a
else
    echo "ERROR: .env file not found. Please copy .env.example to .env and fill in your credentials."
    exit 1
fi

# Verify required variables are set
if [ -z "$TEST_HOST" ] || [ "$TEST_HOST" = "your-vpn-server.example.com" ]; then
    echo "ERROR: TEST_HOST is not set or still has placeholder value"
    exit 1
fi

if [ -z "$TEST_USERNAME" ] || [ "$TEST_USERNAME" = "your-username" ]; then
    echo "ERROR: TEST_USERNAME is not set or still has placeholder value"
    exit 1
fi

if [ -z "$TEST_PASSWORD" ] || [ "$TEST_PASSWORD" = "your-password" ]; then
    echo "ERROR: TEST_PASSWORD is not set or still has placeholder value"
    exit 1
fi

echo "Running integration tests with:"
echo "  TEST_HOST: $TEST_HOST"
echo "  TEST_PORT: ${TEST_PORT:-443}"
echo "  TEST_USERNAME: $TEST_USERNAME"
echo "  TEST_PASSWORD: [hidden]"
echo ""

cd "$PROJECT_DIR"
./gradlew :app:testDebugUnitTest --info 2>&1 | grep -E "(testControlClient|testControlClientUDP|PASSED|FAILED|Tests:)"
