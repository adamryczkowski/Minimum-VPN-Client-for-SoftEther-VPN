# Android Frameworks Overview for SoftEther VPN Client

**Research Date:** December 25, 2025

## Executive Summary

This document provides an overview of Android development frameworks suitable for building a SoftEther VPN client. Based on research, the frameworks are evaluated for their ability to:

1. Implement Android's VpnService API
2. Handle low-level networking operations
3. Provide good user experience
4. Support long-term maintenance

## Framework Options

### 1. Native Kotlin with Jetpack Compose (Recommended)

**Description:** Google's official modern toolkit for building native Android UI, combined with Kotlin as the primary programming language.

**Pros:**

- ✅ **First-class VpnService support** - Direct access to Android's VpnService API
- ✅ **Official Google recommendation** for Android development in 2025
- ✅ **Best performance** - Native code runs directly on the device
- ✅ **Full Android API access** - No bridging or platform channels needed
- ✅ **Proven for VPN apps** - Open-SSTP-Client (485 stars) uses pure Kotlin
- ✅ **Modern UI toolkit** - Jetpack Compose provides declarative UI
- ✅ **Strong typing** - Kotlin's type system catches errors at compile time
- ✅ **Coroutines support** - Excellent for async networking operations
- ✅ **Large community** - Extensive documentation and Stack Overflow support
- ✅ **Long-term support** - Google's primary Android development stack

**Cons:**

- ❌ **Android only** - No code sharing with iOS
- ❌ **Learning curve** - Requires Android-specific knowledge
- ❌ **Verbose for simple apps** - More boilerplate than cross-platform solutions

**VPN-Specific Considerations:**

- Direct implementation of `android.net.VpnService`
- Native TUN interface access via `ParcelFileDescriptor`
- Full control over socket protection and routing
- Easy integration with Android's notification system for foreground services

**Reference Implementations:**

- [Open-SSTP-Client](https://github.com/kittoku/Open-SSTP-Client) - 485 stars, MIT license
- [Minimum-VPN-Client-for-SoftEther-VPN](https://github.com/kittoku/Minimum-VPN-Client-for-SoftEther-VPN) - 63 stars
- [android-vpn-implementation-guide](https://github.com/satishnada/android-vpn-implementation-guide) - 22 stars

---

### 2. Flutter (Dart)

**Description:** Google's cross-platform UI toolkit for building natively compiled applications from a single codebase.

**Pros:**

- ✅ **Cross-platform** - Single codebase for Android and iOS
- ✅ **Hot reload** - Fast development iteration
- ✅ **Rich widget library** - Beautiful, customizable UI components
- ✅ **Growing ecosystem** - Active community and pub.dev packages
- ✅ **Good performance** - Compiles to native ARM code
- ✅ **Existing VPN packages** - `flutter_vpn_service` available on pub.dev

**Cons:**

- ❌ **Platform channels required** - VpnService must be implemented in native Kotlin/Java
- ❌ **Larger app size** - Flutter runtime adds ~5-10MB to APK
- ❌ **Limited low-level access** - Networking code still needs native implementation
- ❌ **Two languages** - Dart for UI, Kotlin for VPN logic
- ❌ **Debugging complexity** - Issues may span Dart and native code

**VPN-Specific Considerations:**

- UI can be built in Flutter/Dart
- VPN connection logic must use platform channels to native Kotlin
- `flutter_vpn_service` package provides basic VpnService wrapper
- Complex protocol implementations (SSTP, SoftEther) still need native code

**Reference Implementations:**

- [ZedPass](https://github.com/CluvexStudio/ZedPass) - Flutter SSTP client, 30 stars
- [VPNclient-app](https://github.com/VPNclient/VPNclient-app) - Flutter VPN client

---

### 3. React Native (JavaScript/TypeScript)

**Description:** Facebook's framework for building native apps using React and JavaScript.

**Pros:**

- ✅ **Cross-platform** - Shared codebase for Android and iOS
- ✅ **Large ecosystem** - npm packages and React community
- ✅ **Web developer friendly** - JavaScript/TypeScript skills transfer
- ✅ **Hot reload** - Fast development iteration
- ✅ **Mature framework** - Used by major apps (Facebook, Instagram)

**Cons:**

- ❌ **Native modules required** - VpnService must be implemented in native code
- ❌ **JavaScript bridge overhead** - Performance impact for intensive operations
- ❌ **Complex native integration** - Bridging VPN logic is non-trivial
- ❌ **Limited VPN packages** - Fewer ready-made VPN solutions
- ❌ **Memory overhead** - JavaScript runtime consumes more memory
- ❌ **Not ideal for background services** - VPN services run continuously

**VPN-Specific Considerations:**

- Not recommended for VPN apps due to bridge overhead
- Background service management is complex
- Low-level networking requires native modules
- No significant advantage over native Kotlin for VPN use case

---

### 4. Kotlin Multiplatform (KMP)

**Description:** JetBrains' technology for sharing Kotlin code between platforms while keeping native UI.

**Pros:**

- ✅ **Shared business logic** - VPN protocol code can be shared
- ✅ **Native UI** - Use Jetpack Compose for Android, SwiftUI for iOS
- ✅ **Kotlin everywhere** - Single language for shared code
- ✅ **Gradual adoption** - Can start with Android and add iOS later
- ✅ **No runtime overhead** - Compiles to native code

**Cons:**

- ❌ **Newer technology** - Less mature than other options
- ❌ **Platform-specific VPN code** - VpnService is Android-only
- ❌ **Smaller community** - Fewer resources and examples
- ❌ **iOS complexity** - Requires Kotlin/Native knowledge for iOS

**VPN-Specific Considerations:**

- Protocol implementation (SSTP, encryption) can be shared
- VpnService integration remains platform-specific
- Good option if iOS support is planned for future

---

### 5. Native C/C++ with NDK

**Description:** Using Android NDK to compile C/C++ code, potentially porting SoftEther's existing codebase.

**Pros:**

- ✅ **Code reuse** - Can port existing SoftEther C code
- ✅ **Maximum performance** - Native machine code
- ✅ **Protocol compatibility** - Exact same implementation as desktop

**Cons:**

- ❌ **Complex build system** - CMake/NDK configuration is challenging
- ❌ **Platform-specific issues** - Android has different system calls
- ❌ **UI still needs Java/Kotlin** - Only networking can be in C
- ❌ **Debugging difficulty** - Native crashes are harder to diagnose
- ❌ **Larger APK size** - Must include native libraries for all ABIs
- ❌ **Maintenance burden** - Two codebases to maintain

**VPN-Specific Considerations:**

- `softethervpn-android` repository attempted this approach (abandoned)
- VpnService still requires Kotlin/Java wrapper
- JNI bridging adds complexity
- Not recommended unless full protocol compatibility is critical

---

## Android VpnService API Overview

All frameworks must ultimately use Android's VpnService API. Key components:

### VpnService Class

```kotlin
class MyVpnService : VpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start VPN connection
    }
}
```

### VpnService.Builder

Configures the VPN tunnel:

- `addAddress()` - Set VPN interface IP address
- `addRoute()` - Configure routing
- `addDnsServer()` - Set DNS servers
- `setSession()` - Set session name (shown in system UI)
- `establish()` - Create the TUN interface

### TUN Interface

- Returns `ParcelFileDescriptor` for reading/writing IP packets
- Read: Outgoing packets from device apps
- Write: Incoming packets from VPN server

### Required Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<service
    android:name=".MyVpnService"
    android:permission="android.permission.BIND_VPN_SERVICE">
    <intent-filter>
        <action android:name="android.net.VpnService" />
    </intent-filter>
</service>
```

---

## Comparison Matrix

| Feature | Kotlin/Compose | Flutter | React Native | KMP | NDK |
|---------|---------------|---------|--------------|-----|-----|
| VpnService Access | Direct | Platform Channel | Native Module | Direct | JNI |
| Performance | Excellent | Good | Fair | Excellent | Excellent |
| Development Speed | Good | Excellent | Good | Good | Poor |
| Cross-Platform | No | Yes | Yes | Partial | No |
| VPN Examples | Many | Few | Very Few | None | One (abandoned) |
| Maintenance | Easy | Medium | Medium | Medium | Hard |
| APK Size | Small | Medium | Medium | Small | Large |
| Community Support | Excellent | Good | Good | Growing | Limited |

---

## Recommendation

### Primary Recommendation: Native Kotlin with Jetpack Compose

For building a SoftEther VPN client, **Native Kotlin with Jetpack Compose** is the recommended framework because:

1. **Proven Track Record** - Open-SSTP-Client demonstrates this approach works well
2. **Direct API Access** - No bridging overhead for VpnService
3. **Best Performance** - Critical for VPN apps that process all network traffic
4. **Simpler Architecture** - Single language, single platform to debug
5. **Future-Proof** - Google's primary Android development stack

### Alternative: Flutter (if cross-platform is required)

If iOS support is a future requirement, Flutter is a viable alternative:

- Use Flutter for UI
- Implement VPN logic in native Kotlin via platform channels
- Reference ZedPass for implementation patterns

### Not Recommended

- **React Native** - Too much overhead for VPN use case
- **NDK/C++** - Complexity not justified unless porting existing code

---

## Sources Used

1. Android Developers - VPN Documentation: https://developer.android.com/develop/connectivity/vpn
2. DuckDuckGo Search: "android app development frameworks 2025 kotlin flutter react native"
3. DuckDuckGo Search: "flutter vpn app development android 2025 vpnservice"
4. GitHub: kittoku/Open-SSTP-Client
5. GitHub: satishnada/android-vpn-implementation-guide
6. Pangea.ai: Mobile App Framework Comparison for 2025
7. pub.dev: flutter_vpn_service package documentation
