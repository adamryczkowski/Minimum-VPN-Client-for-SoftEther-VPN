# SoftEther Protocol Implementation

## Overview

This document describes the SoftEther VPN protocol implementation in SoftEther Connect.
The implementation is based on reverse-engineering the SoftEther VPN protocol and
reference implementations.

## Protocol Stack

```
┌─────────────────────────────────────┐
│         Application Data            │
├─────────────────────────────────────┤
│      SoftEther Protocol Layer       │
├─────────────────────────────────────┤
│         HTTP/HTTPS Layer            │
├─────────────────────────────────────┤
│          SSL/TLS Layer              │
├─────────────────────────────────────┤
│           TCP Layer                 │
└─────────────────────────────────────┘
```

## Connection Sequence

### 1. TCP Connection

The client establishes a TCP connection to the SoftEther VPN server on the
configured port (default: 443 or 5555).

### 2. SSL/TLS Handshake

An SSL/TLS handshake is performed to establish a secure channel:

- Client sends ClientHello
- Server responds with ServerHello, Certificate
- Client verifies certificate (optional)
- Key exchange completes
- Encrypted channel established

### 3. HTTP Upgrade

The connection is upgraded using an HTTP-like handshake:

```http
POST /vpnsvc/connect.cgi HTTP/1.1
Host: <server>
Content-Type: application/octet-stream
Connection: Keep-Alive

<binary data>
```

### 4. SoftEther Authentication

Authentication is performed using the SoftEther protocol:

1. Client sends authentication request with:
   - Username
   - Password (hashed)
   - Hub name
   - Client version info

2. Server validates credentials and responds with:
   - Session ID
   - Connection parameters
   - Virtual hub info

### 5. DHCP Address Assignment

After authentication, the client obtains an IP address:

1. Client sends DHCP Discover
2. Server responds with DHCP Offer
3. Client sends DHCP Request
4. Server responds with DHCP Ack

The DHCP response includes:

- Assigned IP address
- Subnet mask
- Gateway address
- DNS servers

### 6. ARP Gateway Resolution

The client resolves the gateway MAC address:

1. Client sends ARP Request for gateway IP
2. Server responds with ARP Reply containing gateway MAC

### 7. VPN Tunnel Active

The VPN tunnel is now active. IP packets are:

- Encapsulated in Ethernet frames
- Wrapped in SoftEther protocol packets
- Sent over the SSL/TLS connection

## Packet Formats

### Ethernet Frame

```
┌────────────────┬────────────────┬──────────┬─────────────┐
│ Dest MAC (6)   │ Src MAC (6)    │ Type (2) │ Payload     │
└────────────────┴────────────────┴──────────┴─────────────┘
```

### IPv4 Packet

```
┌─────────┬─────────┬──────────────┬───────────────────────┐
│ Ver/IHL │ TOS     │ Total Length │ Identification        │
├─────────┴─────────┼──────────────┼───────────────────────┤
│ Flags/Fragment    │ TTL          │ Protocol              │
├───────────────────┼──────────────┴───────────────────────┤
│ Header Checksum   │ Source IP Address                    │
├───────────────────┼──────────────────────────────────────┤
│                   │ Destination IP Address               │
├───────────────────┴──────────────────────────────────────┤
│ Payload                                                  │
└──────────────────────────────────────────────────────────┘
```

### DHCP Message

```
┌──────────┬──────────┬──────────┬──────────┐
│ Op (1)   │ HType(1) │ HLen (1) │ Hops (1) │
├──────────┴──────────┴──────────┴──────────┤
│ Transaction ID (4)                        │
├───────────────────────────────────────────┤
│ Secs (2)          │ Flags (2)             │
├───────────────────┴───────────────────────┤
│ Client IP Address (4)                     │
├───────────────────────────────────────────┤
│ Your IP Address (4)                       │
├───────────────────────────────────────────┤
│ Server IP Address (4)                     │
├───────────────────────────────────────────┤
│ Gateway IP Address (4)                    │
├───────────────────────────────────────────┤
│ Client Hardware Address (16)              │
├───────────────────────────────────────────┤
│ Server Host Name (64)                     │
├───────────────────────────────────────────┤
│ Boot File Name (128)                      │
├───────────────────────────────────────────┤
│ Magic Cookie (4)                          │
├───────────────────────────────────────────┤
│ Options (variable)                        │
└───────────────────────────────────────────┘
```

### ARP Packet

```
┌──────────────────┬──────────────────┐
│ Hardware Type(2) │ Protocol Type(2) │
├──────────────────┼──────────────────┤
│ HW Addr Len (1)  │ Proto Addr Len(1)│
├──────────────────┴──────────────────┤
│ Operation (2)                       │
├─────────────────────────────────────┤
│ Sender Hardware Address (6)         │
├─────────────────────────────────────┤
│ Sender Protocol Address (4)         │
├─────────────────────────────────────┤
│ Target Hardware Address (6)         │
├─────────────────────────────────────┤
│ Target Protocol Address (4)         │
└─────────────────────────────────────┘
```

## SoftEther Property Pack

SoftEther uses a property-based serialization format for configuration data:

### Property Types

| Type | Description | Format |
|------|-------------|--------|
| Int | 32-bit integer | 4 bytes, big-endian |
| Long | 64-bit integer | 8 bytes, big-endian |
| Boolean | True/False | 4 bytes (0 or 1) |
| ASCII | ASCII string | Length-prefixed |
| UTF-8 | UTF-8 string | Length-prefixed |
| Bytes | Raw bytes | Length-prefixed |
| IP Address | IPv4/IPv6 | 4 or 16 bytes |
| 160-bits | Hash value | 20 bytes |

### Property Pack Structure

```
┌──────────────────────────────────────────┐
│ Property Count (4 bytes)                 │
├──────────────────────────────────────────┤
│ Property 1                               │
│ ├─ Name Length (4 bytes)                 │
│ ├─ Name (variable)                       │
│ ├─ Type (4 bytes)                        │
│ ├─ Value Count (4 bytes)                 │
│ └─ Values (variable)                     │
├──────────────────────────────────────────┤
│ Property 2...                            │
└──────────────────────────────────────────┘
```

## UDP Acceleration

SoftEther supports UDP acceleration for improved performance:

### NAT Traversal

1. Client sends UDP probe to server
2. Server responds with NAT detection info
3. If NAT is detected, hole punching is attempted
4. If successful, data transfers over UDP

### UDP Packet Format

```
┌──────────────────────────────────────────┐
│ Session ID (4 bytes)                     │
├──────────────────────────────────────────┤
│ Sequence Number (8 bytes)                │
├──────────────────────────────────────────┤
│ Flags (4 bytes)                          │
├──────────────────────────────────────────┤
│ Payload (variable)                       │
└──────────────────────────────────────────┘
```

## Keepalive Mechanism

The client sends keepalive packets to maintain the connection:

- Interval: Configurable (default: 50 seconds)
- Format: SoftEther keepalive packet
- Response: Server echoes the packet

## Error Handling

### Connection Errors

| Error Code | Description | Recovery |
|------------|-------------|----------|
| AUTH_FAILED | Authentication failed | Check credentials |
| HUB_NOT_FOUND | Hub doesn't exist | Check hub name |
| TOO_MANY_SESSIONS | Session limit reached | Wait and retry |
| SERVER_BUSY | Server overloaded | Wait and retry |
| TIMEOUT | Connection timeout | Retry connection |

### Runtime Errors

| Error | Description | Recovery |
|-------|-------------|----------|
| Socket closed | Connection lost | Reconnect |
| SSL error | TLS failure | Reconnect |
| DHCP timeout | No IP assigned | Retry DHCP |
| ARP timeout | Gateway not found | Retry ARP |

## Security Considerations

### Certificate Verification

By default, the client verifies the server's SSL certificate.
This can be disabled for self-signed certificates (not recommended).

### Password Hashing

Passwords are hashed using SHA-0 before transmission:

```kotlin
val hashedPassword = sha0(password.toByteArray())
```

Note: SHA-0 is used for compatibility with SoftEther protocol.

### Encryption

All traffic is encrypted using TLS with configurable cipher suites:

- TLS 1.2 minimum (configurable)
- AES-256-GCM preferred
- RSA or ECDHE key exchange

## Implementation Files

| File | Purpose |
|------|---------|
| `SoftEtherClient.kt` | Main protocol implementation |
| `DhcpClient.kt` | DHCP client |
| `ARPClient.kt` | ARP client |
| `TCPTerminal.kt` | TCP/SSL socket handling |
| `UDPTerminal.kt` | UDP acceleration |
| `IPTerminal.kt` | IP packet handling |
| `EthernetFrame.kt` | Ethernet frame parsing |
| `IPv4Packet.kt` | IP packet parsing |
| `DhcpMessage.kt` | DHCP message parsing |
| `ARPPacket.kt` | ARP packet parsing |
| `PropertyPack.kt` | Property serialization |

## References

- [SoftEther VPN Project](https://www.softether.org/)
- [SoftEther VPN Source Code](https://github.com/SoftEtherVPN/SoftEtherVPN)
- [RFC 2131 - DHCP](https://tools.ietf.org/html/rfc2131)
- [RFC 826 - ARP](https://tools.ietf.org/html/rfc826)
- [RFC 791 - IP](https://tools.ietf.org/html/rfc791)
