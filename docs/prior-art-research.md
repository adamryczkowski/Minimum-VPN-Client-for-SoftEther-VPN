# Prior Art Research: SoftEther VPN Android Client

**Research Date:** December 25, 2025

## Summary

This document summarizes the research on existing open-source projects that can serve as references for building a SoftEther VPN client for Android.

## Repositories Cloned

### 1. SoftEtherVPN (Official)

- **Repository:** https://github.com/SoftEtherVPN/SoftEtherVPN
- **Location:** `ref/SoftEtherVPN`
- **Description:** Official SoftEther VPN source code
- **Language:** C (95.3%), C++ (4.1%)
- **License:** Apache-2.0
- **Purpose:** Reference for understanding the VPN protocol implementation

### 2. Open-SSTP-Client ⭐ (Recommended Primary Reference)

- **Repository:** https://github.com/kittoku/Open-SSTP-Client
- **Location:** `ref/Open-SSTP-Client`
- **Stars:** 485
- **Forks:** 120
- **Language:** Kotlin (100%)
- **License:** MIT
- **Last Updated:** October 2025 (actively maintained)
- **Description:** Open-sourced Secure Socket Tunneling Protocol (MS-SSTP) client for Android, developed for accessing VPN Azure Cloud or SoftEther VPN Server
- **Key Features:**
  - Available on Google Play
  - Supports Host, Username, Password authentication
  - SSL/TLS configuration options
  - DNS Server settings
  - HTTP Proxy support
  - Profile save/load functionality
- **Why Important:** Most mature and actively maintained Android VPN client that works with SoftEther

### 3. Minimum-VPN-Client-for-SoftEther-VPN

- **Repository:** https://github.com/kittoku/Minimum-VPN-Client-for-SoftEther-VPN
- **Location:** `ref/Minimum-VPN-Client-for-SoftEther-VPN`
- **Stars:** 63
- **Forks:** 19
- **Language:** Kotlin (100%)
- **License:** Apache-2.0
- **Last Updated:** November 2021
- **Description:** Open-source SoftEther-VPN-protocol-based VPN client for Android
- **Limitations:**
  - Requires DHCP (SecureNAT) enabled on server
  - Requires Password authentication enabled
- **Milestones Achieved:**
  - Works with global-IP-address-assigned server
  - Works with VPN Azure
  - Works with UDP acceleration
- **Why Important:** Implements the native SoftEther VPN protocol (not just SSTP)

### 4. softethervpn-android

- **Repository:** https://github.com/omayanrey/softethervpn-android
- **Location:** `ref/softethervpn-android`
- **Stars:** 18
- **Forks:** 4
- **Language:** C (95.3%), C++ (4.1%)
- **License:** GPL-2.0
- **Last Updated:** 7 years ago (archived/inactive)
- **Description:** Fork of SoftEtherVPN_Stable with Android build configuration
- **Why Important:** Shows approach to porting native SoftEther code to Android using NDK

### 5. ZedPass

- **Repository:** https://github.com/CluvexStudio/ZedPass
- **Location:** `ref/ZedPass`
- **Stars:** 30
- **Language:** Dart (Flutter)
- **License:** Unknown
- **Last Updated:** December 2025 (actively maintained)
- **Description:** Secure SSTP VPN Client for Android built with Flutter
- **Why Important:** Demonstrates cross-platform approach using Flutter/Dart

## Commercial/Closed-Source References

### Softether TLS SSL VPN Adapter (Google Play)

- **Developer:** GNUAPP UNIPESSOAL LDA
- **Downloads:** 10K+
- **Package:** com.excdev.vpn
- **Description:** Specifically designed to work with original SoftEther Server solution
- **Note:** Claims to be open-source under Apache License 2.0 but source code not found on GitHub

## Key Findings

### Protocol Options for SoftEther Connection

1. **SSTP (Secure Socket Tunneling Protocol)**
   - Most mature Android implementation available (Open-SSTP-Client)
   - Uses HTTPS (port 443) - good for firewall bypass
   - Well-documented protocol (Microsoft specification)

2. **Native SoftEther VPN Protocol**
   - More efficient than SSTP
   - Supports UDP acceleration
   - Less mature Android implementation (Minimum-VPN-Client)

3. **L2TP/IPsec**
   - Built into Android natively
   - No custom app needed, but limited configuration

4. **OpenVPN**
   - Mature Android clients exist (OpenVPN Connect, OpenVPN for Android)
   - SoftEther can act as OpenVPN server

### Framework Approaches Observed

1. **Native Kotlin/Java** (Open-SSTP-Client, Minimum-VPN-Client)
   - Direct Android API access
   - Best performance
   - Uses Android VpnService API

2. **Native C/C++ with NDK** (softethervpn-android)
   - Port of existing C codebase
   - Complex build process
   - Potentially best protocol compatibility

3. **Flutter/Dart** (ZedPass)
   - Cross-platform development
   - May require platform channels for VPN functionality

## Recommendations

1. **Primary Reference:** Use `Open-SSTP-Client` as the main reference for Android VPN implementation patterns
2. **Protocol Study:** Study `Minimum-VPN-Client-for-SoftEther-VPN` for native SoftEther protocol implementation
3. **Build System:** Reference `softethervpn-android` for NDK integration if native C code is needed
4. **Cross-platform:** Consider `ZedPass` if Flutter approach is chosen

## Sources Used

1. GitHub Search: https://github.com/search?q=softether+android
2. GitHub Search: https://github.com/search?q=sstp+android
3. Google Play Store: https://play.google.com/store/apps/details?id=com.excdev.vpn
4. DuckDuckGo Search: "softether vpn android client github"
5. DuckDuckGo Search: "softether android client open source"
