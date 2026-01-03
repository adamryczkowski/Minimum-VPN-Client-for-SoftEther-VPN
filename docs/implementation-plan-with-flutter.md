# Implementation Plan: Flutter with Native SoftEther Protocol

**Date:** December 25, 2025  
**Target:** Android 15 (API level 35)  
**Root Required:** No  
**Protocol:** Native SoftEther VPN Protocol (via Platform Channels)

## Executive Summary

This plan details an alternative approach using Flutter for the UI while implementing the VPN protocol logic in native Kotlin via platform channels. This is a fallback option if cross-platform support (iOS) becomes a future requirement.

## Why Flutter?

- **Cross-platform potential** - Same UI codebase for Android and iOS
- **Rapid UI development** - Hot reload and rich widget library
- **Modern UI** - Material 3 support out of the box

## Why NOT Flutter (Trade-offs)?

- **Platform channels overhead** - VPN logic must still be native Kotlin
- **Two languages** - Dart for UI, Kotlin for VPN
- **Debugging complexity** - Issues may span both layers
- **Larger APK size** - Flutter runtime adds ~5-10MB

## Reference Implementation

The `ZedPass` project (cloned to `ref/ZedPass`) demonstrates Flutter VPN development:
- Uses Flutter for UI
- Implements SSTP protocol
- 30 stars, actively maintained

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Flutter Application                       │
├─────────────────────────────────────────────────────────────┤
│  UI Layer (Flutter/Dart)                                    │
│  ├── main.dart                                              │
│  ├── screens/                                               │
│  │   ├── home_screen.dart                                   │
│  │   ├── connection_screen.dart                             │
│  │   └── settings_screen.dart                               │
│  ├── widgets/                                               │
│  └── providers/ (state management)                          │
├─────────────────────────────────────────────────────────────┤
│  Platform Channel Bridge                                    │
│  ├── MethodChannel (commands)                               │
│  └── EventChannel (status updates)                          │
├─────────────────────────────────────────────────────────────┤
│  Native Kotlin Layer                                        │
│  ├── SoftEtherVpnService (extends VpnService)              │
│  ├── SoftEtherClient                                        │
│  ├── DHCPClient                                            │
│  └── ARPClient                                             │
├─────────────────────────────────────────────────────────────┤
│  Android VpnService API                                     │
│  └── TUN Interface (ParcelFileDescriptor)                  │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Phases

### Phase 1: Project Setup (Week 1)
- Create Flutter project
- Set up platform channels
- Configure Android module for VpnService
- Implement basic Flutter UI

### Phase 2: Platform Channel Bridge (Week 2)
- Define MethodChannel for VPN commands
- Define EventChannel for status updates
- Implement Kotlin plugin structure
- Test bidirectional communication

### Phase 3: Native VPN Implementation (Weeks 3-5)
- Port/adapt Minimum-VPN-Client code to plugin
- Implement VpnService in Kotlin
- Implement SoftEther protocol
- Implement DHCP and ARP clients

### Phase 4: Flutter UI (Week 6)
- Implement connection screen
- Implement settings screen
- Implement profile management
- Add state management (Provider/Riverpod)

### Phase 5: Integration and Testing (Week 7-8)
- Integrate UI with native layer
- Test connection scenarios
- Performance optimization
- Battery usage optimization

## Risks and Mitigations

### Risk 1: Platform Channel Complexity

**Risk Level:** HIGH

**Description:** VPN operations require continuous data flow between Flutter and native code, which can be complex to manage.

**Mitigation:**
1. Use EventChannel for streaming status updates
2. Keep VPN logic entirely in native code
3. Only use Flutter for UI, not for packet processing
4. Reference ZedPass implementation patterns

### Risk 2: Performance Overhead

**Risk Level:** MEDIUM

**Description:** Platform channel calls have overhead that could impact VPN performance.

**Mitigation:**
1. Minimize channel calls - batch updates
2. Keep all packet processing in native code
3. Only send status updates, not packet data, to Flutter
4. Use background isolates if needed

### Risk 3: Debugging Difficulty

**Risk Level:** MEDIUM

**Description:** Bugs may span Flutter and native code, making debugging harder.

**Mitigation:**
1. Extensive logging on both sides
2. Test native code independently first
3. Use Flutter DevTools and Android Studio together
4. Clear separation of concerns

### Risk 4: Flutter VPN Package Limitations

**Risk Level:** LOW

**Description:** Existing Flutter VPN packages may not support SoftEther protocol.

**Mitigation:**
1. Don't rely on existing packages
2. Implement custom platform channel plugin
3. Use packages only for UI components

## Dependencies

### Flutter (pubspec.yaml)
```yaml
dependencies:
  flutter:
    sdk: flutter
  provider: ^6.1.1
  shared_preferences: ^2.2.2
  flutter_secure_storage: ^9.0.0

dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^3.0.1
```

### Native Kotlin (build.gradle.kts)
```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
```

## Comparison with Native Kotlin Approach

| Aspect | Native Kotlin | Flutter |
|--------|--------------|---------|
| Development Speed | Medium | Fast (UI) |
| Performance | Best | Good |
| APK Size | ~5MB | ~15MB |
| Debugging | Easy | Medium |
| iOS Support | No | Possible |
| Maintenance | Single language | Two languages |
| VPN Logic | Direct | Via channels |

## Recommendation

**Flutter is NOT recommended as the primary approach** for this project because:

1. VPN logic must still be in native Kotlin
2. No significant advantage for Android-only app
3. Added complexity without clear benefit
4. Native Kotlin with Jetpack Compose provides similar modern UI

**Consider Flutter only if:**
- iOS support is planned for the future
- You have existing Flutter expertise
- Cross-platform UI consistency is important

## Conclusion

While Flutter is a viable option, the native Kotlin approach is recommended for a SoftEther VPN client because the VPN protocol implementation must be native regardless, and Flutter adds complexity without providing significant benefits for an Android-only application.
