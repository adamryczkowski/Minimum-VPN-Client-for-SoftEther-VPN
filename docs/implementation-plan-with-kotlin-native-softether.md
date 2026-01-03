# Implementation Plan: Kotlin/Jetpack Compose with Native SoftEther Protocol

**Date:** December 25, 2025  
**Target:** Android 15 (API level 35)  
**Root Required:** No  
**Protocol:** Native SoftEther VPN Protocol

## Executive Summary

This plan details how to build an Android VPN client using Kotlin and Jetpack Compose that connects to SoftEther VPN servers using the native SoftEther protocol - the same protocol used by the desktop SoftEther VPN Client software.

## Why Native SoftEther Protocol?

Since you're using the SoftEther VPN Client software on your laptop, implementing the native protocol provides:
- **Same experience** as your laptop connection
- **Best performance** with UDP acceleration support
- **Full compatibility** with your existing server configuration

## Reference Implementation

The `Minimum-VPN-Client-for-SoftEther-VPN` project (cloned to `ref/`) demonstrates this approach:
- 100% Kotlin implementation
- Works without root using VpnService API
- Implements Layer 2 over Layer 3 adaptation

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Android Application                       │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                                 │
│  ├── MainActivity                                           │
│  ├── ConnectionScreen                                       │
│  ├── SettingsScreen                                         │
│  └── ProfilesScreen                                         │
├─────────────────────────────────────────────────────────────┤
│  Service Layer                                              │
│  ├── SoftEtherVpnService (extends VpnService)              │
│  ├── ConnectionController                                   │
│  └── NotificationManager                                    │
├─────────────────────────────────────────────────────────────┤
│  Protocol Layer                                             │
│  ├── SoftEtherClient (main protocol handler)               │
│  ├── HttpClient (HTTPS tunnel)                             │
│  ├── EthernetFrameHandler                                  │
│  ├── DHCPClient                                            │
│  └── ARPClient                                             │
├─────────────────────────────────────────────────────────────┤
│  Network Layer                                              │
│  ├── TcpTerminal (TCP connections)                         │
│  ├── UdpTerminal (UDP acceleration)                        │
│  └── IpTerminal (TUN interface)                            │
├─────────────────────────────────────────────────────────────┤
│  Android VpnService API                                     │
│  └── TUN Interface (ParcelFileDescriptor)                  │
└─────────────────────────────────────────────────────────────┘
```

## Server Requirements

For this implementation to work, your SoftEther server needs:
- **SecureNAT (DHCP) enabled** on the Virtual HUB
- This is often the default configuration

## Key Components

### 1. VpnService Implementation
- Extends `android.net.VpnService`
- Creates TUN interface for IP packet routing
- Runs as foreground service with notification

### 2. SoftEther Protocol Client
- Establishes HTTPS connection to server
- Performs SoftEther handshake and authentication
- Handles Ethernet frame encapsulation

### 3. Layer 2 to Layer 3 Adaptation
- DHCP client to obtain IP address from server
- ARP client for address resolution
- Ethernet frame wrapping/unwrapping

### 4. UDP Acceleration (Optional)
- Improves performance for real-time traffic
- Falls back to TCP if UDP is blocked

---

## Implementation Phases

### Phase 1: Project Setup (Week 1)
- Create Android project with Kotlin and Jetpack Compose
- Set up Gradle with required dependencies
- Configure VpnService in AndroidManifest.xml
- Implement basic UI scaffolding

### Phase 2: VpnService Foundation (Week 2)
- Implement VpnService with TUN interface
- Create foreground service with notification
- Implement connection state management
- Add basic IP packet reading/writing

### Phase 3: SoftEther Protocol (Weeks 3-4)
- Implement HTTPS tunnel connection
- Implement SoftEther handshake protocol
- Implement authentication (password, certificate)
- Implement Ethernet frame handling

### Phase 4: Network Clients (Week 5)
- Implement DHCP client for IP configuration
- Implement ARP client for address resolution
- Implement keepalive mechanism

### Phase 5: UDP Acceleration (Week 6)
- Implement UDP channel establishment
- Implement UDP packet handling
- Implement fallback to TCP

### Phase 6: UI and Polish (Week 7)
- Complete Jetpack Compose UI
- Add profile management
- Add connection statistics
- Implement settings persistence

### Phase 7: Testing and Release (Week 8)
- Test with various SoftEther server configurations
- Performance optimization
- Battery usage optimization
- Prepare for release

---

## Risks and Mitigations

### Risk 1: Protocol Complexity

**Risk Level:** HIGH

**Description:** The native SoftEther protocol is complex and not fully documented. Implementing it from scratch requires reverse-engineering or studying the C source code.

**Mitigation Strategies:**
1. **Use existing implementation as reference** - The `Minimum-VPN-Client-for-SoftEther-VPN` repository provides a working Kotlin implementation
2. **Study the SoftEther source code** - The protocol is implemented in `ref/SoftEtherVPN/src/Cedar/`
3. **Start with basic features** - Implement password authentication first, add certificate auth later
4. **Incremental testing** - Test each protocol component separately

**Sources:**
- GitHub: kittoku/Minimum-VPN-Client-for-SoftEther-VPN
- SoftEther VPN source code (Cedar module)

---

### Risk 2: SecureNAT Requirement

**Risk Level:** MEDIUM

**Description:** The Layer 2 to Layer 3 adaptation requires SecureNAT (DHCP) to be enabled on the server's Virtual HUB.

**Mitigation Strategies:**
1. **Verify server configuration** - Check that SecureNAT is enabled on your family VPN server
2. **Document requirement clearly** - Make it clear in app documentation
3. **Provide fallback** - Consider implementing SSTP as a fallback protocol
4. **Add diagnostic messages** - Detect and report when DHCP fails

**How to enable SecureNAT:**
```
vpncmd /SERVER localhost /CMD SecureNatEnable
```

---

### Risk 3: Android Background Service Restrictions

**Risk Level:** MEDIUM

**Description:** Android 14/15 have strict background service restrictions. VPN services may be killed by the system.

**Mitigation Strategies:**
1. **Use foreground service** - VpnService should run as foreground with persistent notification
2. **Implement Always-on VPN** - Register for system's always-on VPN feature
3. **Handle reconnection** - Implement automatic reconnection on service restart
4. **Battery optimization exemption** - Request user to disable battery optimization for the app

**Sources:**
- Android Developers: https://developer.android.com/develop/connectivity/vpn
- Android 15 behavior changes documentation

---

### Risk 4: Battery Drain

**Risk Level:** MEDIUM

**Description:** VPN apps process all network traffic, which can cause significant battery drain.

**Mitigation Strategies:**
1. **Efficient packet processing** - Use Kotlin coroutines for async I/O
2. **Minimize wakeups** - Batch packet processing where possible
3. **UDP acceleration** - Use UDP mode to reduce CPU overhead
4. **Optimize keepalive intervals** - Balance connection stability vs battery

**Sources:**
- SafetyDetectives: "Does a VPN Drain Your Battery in 2025?"
- Astrill VPN: "Tips to Avoid VPN Battery Drain"

---

### Risk 5: Network Transitions

**Risk Level:** MEDIUM

**Description:** Connection may break when switching between WiFi and mobile data.

**Mitigation Strategies:**
1. **Monitor network changes** - Use `ConnectivityManager` to detect network transitions
2. **Implement reconnection logic** - Automatically reconnect on network change
3. **Preserve session state** - Cache session information for quick reconnection
4. **Use NetworkCallback** - Register for network state changes

---

### Risk 6: TLS/SSL Certificate Validation

**Risk Level:** LOW-MEDIUM

**Description:** Proper certificate validation is critical for security but can cause connection issues.

**Mitigation Strategies:**
1. **Use Android's default trust store** - Leverage system CA certificates
2. **Allow custom CA certificates** - For self-signed server certificates
3. **Implement certificate pinning** - Optional for enhanced security
4. **Clear error messages** - Help users understand certificate issues

---

### Risk 7: Google Play Store Approval

**Risk Level:** LOW

**Description:** VPN apps face additional scrutiny on Google Play Store.

**Mitigation Strategies:**
1. **Follow VPN policy** - Comply with Google Play VPN app policies
2. **Privacy policy** - Create clear privacy policy (no data collection)
3. **Accurate description** - Clearly describe app functionality
4. **No misleading claims** - Don't claim to bypass restrictions

**Sources:**
- Google Play Developer Policy: VPN apps

---

### Risk 8: API Level 35 Requirements

**Risk Level:** LOW

**Description:** Starting August 31, 2025, new apps must target Android 15 (API level 35).

**Mitigation Strategies:**
1. **Target API 35 from start** - Build with latest SDK
2. **Test on Android 15** - Use Android 15 emulator and devices
3. **Review behavior changes** - Check Android 15 behavior changes documentation
4. **Use Jetpack libraries** - They handle API compatibility

**Sources:**
- Google Play Console Help: Target API level requirements

---

## Dependencies

```kotlin
// build.gradle.kts (app level)
dependencies {
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

---

## Conclusion

The native SoftEther protocol implementation is the recommended approach for your use case because:

1. **Matches your laptop experience** - Same protocol as SoftEther VPN Client
2. **Proven implementation exists** - Minimum-VPN-Client-for-SoftEther-VPN demonstrates feasibility
3. **No root required** - Works with standard VpnService API
4. **Best performance** - Supports UDP acceleration

The main requirement is that your server has SecureNAT enabled, which is common for home/family VPN setups.

---

## Next Steps

1. Verify SecureNAT is enabled on your SoftEther server
2. Set up Android development environment
3. Clone and study the Minimum-VPN-Client-for-SoftEther-VPN code
4. Begin Phase 1 implementation
