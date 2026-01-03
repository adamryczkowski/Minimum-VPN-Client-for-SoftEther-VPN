# Baseline Test Report

**Date:** 2026-01-03
**Version:** 1.0.0
**Tester:** Automated (Gradle)

## Environment

- **Build System:** Gradle 8.13, AGP 8.13.2, Kotlin 2.3.0
- **JDK:** temurin-21
- **compileSdk/targetSdk:** 36
- **VPN Server:** 172.104.148.166:992 (SoftEther VPN)

## Unit Test Results

| Test | Result | Notes |
|------|--------|-------|
| testHashSha0 | ✅ PASS | SHA-0 hash implementation verified |
| testUDPDatagram | ✅ PASS | UDP datagram serialization/deserialization works |
| testIPPacket | ✅ PASS | IP packet serialization/deserialization works |
| testNATTRegex | ✅ PASS | NAT-T regex patterns match correctly |
| testByteArraySearch | ✅ PASS | Byte array search algorithm works |

## Integration Test Results

| Test | Result | Notes |
|------|--------|-------|
| testControlClient | ✅ PASS | Connected to VPN server 172.104.148.166:992 |
| testControlClientUDP | ✅ PASS | Connected to VPN server with UDP acceleration |

**Note:** Integration tests were run with environment variables from `.env` file pointing to the family VPN server.

## Manual Test Results

See [Manual Testing Checklist](manual-testing-checklist.md)

## Summary

- **Total Tests:** 7
- **Passed:** 7
- **Failed:** 0
- **Blocked:** 0

## Build Warnings

The following deprecation warnings were observed during build:
1. `startActivityForResult` deprecated in HomeFragment.kt
2. `stopForeground(Boolean)` deprecated in ControlClient.kt
3. Java type mismatch in NetworkObserver.kt
4. Instance check always false in TCPTerminal.kt (will become error in Kotlin 2.4)

## Recommendations

1. Address deprecation warnings before Kotlin 2.4 upgrade
2. Run integration tests with actual VPN server credentials
3. Complete manual testing checklist on physical Android device
4. Consider updating deprecated Android APIs (startActivityForResult, stopForeground)
