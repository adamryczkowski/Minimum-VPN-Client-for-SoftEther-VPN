# SoftEther Android VPN Client - Planning Documentation

**Project Goal:** Build an Android VPN client that connects to a family SoftEther VPN server using the same native SoftEther protocol as the desktop client.

**Planning Date:** December 25, 2025

## Quick Summary

### Recommended Approach
**Kotlin with Jetpack Compose + Native SoftEther Protocol**

### Key Findings
1. ✅ **VpnService API is compatible** with SoftEther (no root required)
2. ✅ **Working reference implementation exists** - `Minimum-VPN-Client-for-SoftEther-VPN`
3. ✅ **Native protocol can work on Android** via Layer 2 over Layer 3 adaptation
4. ⚠️ **Requires SecureNAT (DHCP) enabled** on server's Virtual HUB

## Documentation Index

| Document | Description |
|----------|-------------|
| [prior-art-research.md](prior-art-research.md) | Survey of existing open-source SoftEther/VPN Android projects |
| [softether-client-analysis.md](softether-client-analysis.md) | Analysis of SoftEther VPN source code and protocol |
| [android-frameworks-overview.md](android-frameworks-overview.md) | Comparison of Android development frameworks for VPN apps |
| [implementation-plan-with-kotlin-native-softether.md](implementation-plan-with-kotlin-native-softether.md) | **PRIMARY** - Detailed implementation plan with Kotlin |
| [implementation-plan-with-flutter.md](implementation-plan-with-flutter.md) | FALLBACK - Flutter implementation plan |

## Reference Repositories

Located in `ref/` directory:

| Repository | Purpose | Stars |
|------------|---------|-------|
| `SoftEtherVPN` | Official SoftEther source code | - |
| `Open-SSTP-Client` | SSTP client for Android (Kotlin) | 485 |
| `Minimum-VPN-Client-for-SoftEther-VPN` | Native SoftEther protocol for Android | 63 |
| `softethervpn-android` | SoftEther NDK port attempt | 18 |
| `ZedPass` | Flutter SSTP client | 30 |

## Architecture Decision

### Why Native SoftEther Protocol?
- Matches your laptop's SoftEther VPN Client experience
- Best performance with UDP acceleration support
- Full compatibility with existing server configuration

### Why Kotlin/Jetpack Compose?
- Direct VpnService API access (no bridging)
- Best performance for VPN apps
- Google's recommended Android development stack
- Proven by Open-SSTP-Client (485 stars, on Google Play)

### How it Works Without Root

```
┌─────────────────────────────────────────────────────────────┐
│  Android VpnService creates TUN interface (Layer 3)        │
│                         ↓                                   │
│  App wraps IP packets in Ethernet frames (Layer 2)         │
│                         ↓                                   │
│  SoftEther protocol sends frames over HTTPS                │
│                         ↓                                   │
│  Server's SecureNAT provides DHCP for IP configuration     │
└─────────────────────────────────────────────────────────────┘
```

## Key Risks Summary

| Risk | Level | Mitigation |
|------|-------|------------|
| Protocol complexity | HIGH | Use Minimum-VPN-Client as reference |
| SecureNAT requirement | MEDIUM | Verify server config, document requirement |
| Background service restrictions | MEDIUM | Foreground service, Always-on VPN |
| Battery drain | MEDIUM | UDP acceleration, efficient packet processing |
| Network transitions | MEDIUM | Auto-reconnect on network change |

## Prerequisites

Before starting implementation:

1. **Verify server configuration:**
   - SecureNAT (DHCP) must be enabled on Virtual HUB
   - Check with: `vpncmd /SERVER /CMD SecureNatStatusGet`

2. **Development environment:**
   - Android Studio (latest)
   - Android SDK with API level 35
   - Kotlin 1.9+
   - Physical Android device for testing (emulator has VPN limitations)

## Estimated Timeline

| Phase | Duration | Description |
|-------|----------|-------------|
| Setup | 1 week | Project setup, dependencies, basic UI |
| VpnService | 1 week | TUN interface, foreground service |
| Protocol | 2 weeks | SoftEther handshake, authentication |
| Network | 1 week | DHCP, ARP clients |
| UDP Accel | 1 week | Optional UDP acceleration |
| UI/Polish | 1 week | Complete UI, settings, profiles |
| Testing | 1 week | Testing, optimization, release prep |
| **Total** | **8 weeks** | |

## Next Steps

1. ✅ Planning complete (this documentation)
2. ⬜ Verify SecureNAT is enabled on your SoftEther server
3. ⬜ Set up Android development environment
4. ⬜ Study Minimum-VPN-Client-for-SoftEther-VPN code
5. ⬜ Begin Phase 1 implementation

## Sources Used

- GitHub repositories (see Reference Repositories above)
- Android Developers VPN documentation
- Google Play Console API requirements
- DuckDuckGo searches for Android VPN development 2025
- SoftEther VPN official source code
