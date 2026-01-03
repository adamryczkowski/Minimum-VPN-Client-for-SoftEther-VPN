# SoftEther VPN Client Architecture Analysis

**Analysis Date:** December 25, 2025

## Overview

This document analyzes the SoftEther VPN client architecture to understand how to build an Android client that can connect to a family VPN server.

## SoftEther VPN Source Code Structure

### Key Directories

```
src/
├── Cedar/          # Core VPN protocol implementation
│   ├── Client.c/h  # VPN Client logic
│   ├── Connection.c/h  # Connection management
│   ├── Session.c/h # Session management
│   ├── Account.c/h # Account/credential management
│   └── ...
├── Mayaqua/        # Low-level networking library
├── vpnclient/      # VPN Client service wrapper
├── vpncmd/         # Command-line interface
└── vpnserver/      # VPN Server
```

### Core Components

#### 1. Client Structure (`Client.h`)

The main `CLIENT` structure contains:
- `AccountList` - List of VPN connection accounts
- `Config` - Client configuration settings
- `Cedar` - Reference to the Cedar networking core
- `UnixVLanList` - Virtual LAN cards (for Unix/Linux)

#### 2. Account Structure

Each VPN connection is represented by an `ACCOUNT`:
- `ClientOption` - Connection options (host, port, hub name, etc.)
- `ClientAuth` - Authentication data (username, password, certificates)
- `ServerCert` - Server certificate for verification
- `ClientSession` - Active session when connected

#### 3. Client Options (`CLIENT_OPTION`)

Key connection parameters:
- `Hostname` - VPN server hostname
- `Port` - Connection port (typically 443 for HTTPS/SSTP)
- `PortUDP` - UDP port for acceleration
- `HubName` - Virtual HUB name
- `ProxyType/ProxyName/ProxyPort` - Proxy settings
- `UseEncrypt` - Enable encryption
- `UseCompress` - Enable compression
- `DeviceName` - Virtual network adapter name

#### 4. Authentication Types (`CLIENT_AUTH`)

Supported authentication methods:
- Password authentication (hashed or plain)
- Certificate authentication (X.509)
- Secure device authentication (smart cards)

#### 5. Connection Structure (`CONNECTION`)

Manages the active VPN connection:
- `Protocol` - Connection protocol
- `Tcp/Udp` - Transport layer structures
- `ReceivedBlocks/SendBlocks` - Data queues
- `ServerX/ClientX` - SSL certificates
- `CipherName` - Encryption algorithm

#### 6. Session Structure (`SESSION`)

Represents an active VPN session:
- `ClientOption/ClientAuth` - Connection parameters
- `PacketAdapter` - Interface to virtual network adapter
- `Traffic` - Traffic statistics
- `UseEncrypt/UseCompress` - Session options
- `UdpAccel` - UDP acceleration support

## Protocol Support

SoftEther supports multiple VPN protocols:

1. **SoftEther VPN Protocol** (Native)
   - Proprietary protocol over HTTPS
   - Supports UDP acceleration
   - Best performance with SoftEther servers

2. **SSTP (Secure Socket Tunneling Protocol)**
   - Microsoft protocol over HTTPS (port 443)
   - Good firewall bypass capability
   - Well-documented (MS-SSTP specification)

3. **L2TP/IPsec**
   - Standard VPN protocol
   - Built into most operating systems

4. **OpenVPN**
   - Open-source VPN protocol
   - Wide compatibility

## Connection Flow

### 1. Initialization
```
CtStartClient() → CiNewClient() → CiLoadConfigurationFile()
```

### 2. Connection Establishment
```
CcConnect() → CtConnect() → NewClientSession() → ClientThread()
```

### 3. Session Management
```
SessionConnect() → ClientConnect() → [Protocol Negotiation] → SessionMain()
```

### 4. Data Transfer
```
PacketAdapter.GetNextPacket() → [Encrypt/Compress] → ConnectionSend()
ConnectionReceive() → [Decrypt/Decompress] → PacketAdapter.PutPacket()
```

## Android Implementation Reference (Open-SSTP-Client)

### Architecture

The Open-SSTP-Client provides an excellent reference for Android VPN implementation:

```
kittoku.osc/
├── service/
│   └── SstpVpnService.kt    # Android VpnService implementation
├── control/
│   └── Controller.kt        # Main connection controller
├── client/
│   ├── SstpClient.kt        # SSTP protocol client
│   └── ppp/                 # PPP protocol implementation
│       ├── PPPClient.kt
│       ├── LCPClient.kt     # Link Control Protocol
│       ├── IpcpClient.kt    # IP Control Protocol
│       └── auth/            # Authentication protocols
│           ├── PAPClient.kt
│           ├── ChapClient.kt
│           └── EAPClient.kt
├── io/
│   ├── IncomingManager.kt   # Incoming packet handling
│   └── OutgoingManager.kt   # Outgoing packet handling
└── terminal/
    └── SSLTerminal.kt       # SSL/TLS connection
```

### Key Android Components

#### 1. VpnService (`SstpVpnService.kt`)

Extends `android.net.VpnService`:
- Manages VPN lifecycle (connect/disconnect)
- Handles foreground service notifications
- Manages reconnection logic
- Integrates with Quick Settings tile

#### 2. Controller (`Controller.kt`)

Orchestrates the connection:
- Initializes SSL terminal
- Manages SSTP client
- Handles PPP negotiation (LCP, Authentication, IPCP)
- Manages IP terminal for packet routing

#### 3. Protocol Stack

```
Application Layer
       ↓
   VpnService (Android TUN interface)
       ↓
   IP Terminal (packet routing)
       ↓
   PPP Layer (framing, authentication)
       ↓
   SSTP Layer (encapsulation)
       ↓
   SSL/TLS Layer (encryption)
       ↓
   TCP/IP (transport)
```

### Connection Sequence

1. **SSL Handshake**
   - Establish TLS connection to server
   - Verify server certificate

2. **SSTP Negotiation**
   - Send SSTP_CALL_CONNECT_REQUEST
   - Receive SSTP_CALL_CONNECT_ACK

3. **PPP LCP Negotiation**
   - Configure link parameters (MRU, authentication protocol)

4. **PPP Authentication**
   - PAP, CHAP (MS-CHAPv2), or EAP

5. **PPP IPCP Negotiation**
   - Obtain IP address from server
   - Configure DNS servers

6. **VPN Tunnel Active**
   - Route packets through TUN interface
   - Encapsulate in PPP/SSTP/TLS

## Key Considerations for Android Implementation

### 1. Android VpnService API

- Must extend `VpnService`
- Requires `BIND_VPN_SERVICE` permission
- User must approve VPN connection
- Creates TUN interface for packet routing

### 2. Background Service

- Must run as foreground service with notification
- Handle process lifecycle (don't get killed)
- Support reconnection on network changes

### 3. Network Handling

- Monitor network connectivity changes
- Handle WiFi ↔ Mobile transitions
- Support split tunneling (optional)

### 4. Security

- Secure credential storage (Android Keystore)
- Certificate validation
- Support for client certificates

### 5. User Interface

- Connection status display
- Server configuration
- Profile management
- Quick Settings tile integration

## Recommended Approach for Android Client

Based on the analysis, the recommended approach is:

### Option A: Extend Open-SSTP-Client (Recommended)

**Pros:**
- Already implements SSTP protocol (works with SoftEther)
- Mature, tested codebase
- MIT license allows modification
- Kotlin/Android native

**Cons:**
- Only supports SSTP protocol
- May need updates for newer Android versions

### Option B: Implement Native SoftEther Protocol

**Pros:**
- Full protocol support including UDP acceleration
- Best performance

**Cons:**
- Complex protocol implementation
- Less documentation than SSTP

### Option C: Use NDK with SoftEther C Code

**Pros:**
- Reuse existing C codebase
- Full protocol compatibility

**Cons:**
- Complex build system
- Platform-specific issues
- Larger APK size

## Conclusion

For connecting to a family SoftEther VPN server, the **SSTP protocol** approach (Option A) is recommended because:

1. SoftEther servers support SSTP natively
2. Open-SSTP-Client provides a working reference implementation
3. SSTP uses HTTPS (port 443) which works through most firewalls
4. The protocol is well-documented and stable

The implementation should:
1. Use Open-SSTP-Client as a foundation
2. Add any missing features (profile management, etc.)
3. Update for latest Android API requirements
4. Add proper error handling and user feedback
