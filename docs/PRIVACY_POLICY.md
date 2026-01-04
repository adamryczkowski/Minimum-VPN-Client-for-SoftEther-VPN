# Privacy Policy - SoftEther Connect

**Last Updated:** January 4, 2026

## Introduction

SoftEther Connect ("the App") is an open-source VPN client for Android that
connects to SoftEther VPN servers. This privacy policy explains how the App
handles your data.

## Summary

**We do not collect, store, or transmit any personal data to external servers.**

The App operates entirely on your device and communicates only with the VPN
server you configure.

## Data Collection

### What We DO NOT Collect

- Personal information (name, email, phone number)
- Device identifiers or advertising IDs
- Location data
- Usage analytics or telemetry
- Browsing history or network traffic content
- Any data for advertising purposes

### What the App Stores Locally

The following data is stored **only on your device**:

1. **VPN Connection Profiles**
   - Server hostname/IP address
   - Port number
   - Virtual hub name
   - Username (stored encrypted)
   - Password (stored encrypted using Android Keystore)

2. **App Settings**
   - Connection preferences
   - UI preferences
   - Split tunneling configuration

3. **Connection Statistics** (optional)
   - Bytes sent/received
   - Connection duration
   - Connection timestamps

4. **Diagnostic Logs** (optional)
   - VPN connection events
   - Error messages for troubleshooting
   - Crash reports (stored locally only)

## Data Transmission

### VPN Traffic

When connected to a VPN server:

- All your network traffic is encrypted and routed through the VPN tunnel
- The VPN server you connect to may log connection data according to its
  own policies
- We have no control over or access to VPN server logs

### No External Services

The App does not:

- Connect to any analytics services
- Send crash reports to external servers
- Make any network requests except to your configured VPN server
- Include any third-party tracking SDKs

## Data Security

### Credential Storage

- Passwords are encrypted using Android's EncryptedSharedPreferences
- Encryption keys are stored in Android Keystore (hardware-backed when available)
- Credentials are never transmitted unencrypted

### Local Data

- All local data can be deleted by uninstalling the App
- You can export and delete diagnostic logs from within the App

## Permissions

The App requests the following permissions:

| Permission | Purpose |
|------------|---------|
| `INTERNET` | Required to establish VPN connections |
| `FOREGROUND_SERVICE` | Required to maintain VPN connection in background |
| `RECEIVE_BOOT_COMPLETED` | Optional: Auto-connect on device boot |
| `ACCESS_NETWORK_STATE` | Detect network changes for auto-reconnect |
| `QUERY_ALL_PACKAGES` | Optional: Split tunneling app selection |

## Children's Privacy

The App does not knowingly collect any data from children under 13 years of age.
Since we do not collect any personal data, this is not applicable.

## Open Source

SoftEther Connect is open-source software. You can review the complete source
code to verify our privacy practices:

- **Repository:** [GitHub](https://github.com/your-username/softether-android)
- **License:** Apache 2.0

## Third-Party Services

The App does not integrate with any third-party services that collect user data.

## Your Rights

Since we do not collect any personal data, traditional data subject rights
(access, deletion, portability) are not applicable. However:

- You can delete all local data by uninstalling the App
- You can export diagnostic logs before deletion
- You can review the source code to verify our practices

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be:

- Posted in the App's repository
- Noted in release notes
- Effective immediately upon posting

## Contact

If you have questions about this privacy policy:

- Open an issue on GitHub
- Email: [your-email@example.com]

## Legal Basis (GDPR)

For users in the European Economic Area:

Since we do not collect or process any personal data, GDPR data processing
requirements do not apply. The App operates entirely locally on your device.

## California Privacy Rights (CCPA)

For California residents:

We do not sell personal information. We do not collect personal information
as defined by CCPA. Therefore, CCPA requirements do not apply.

---

*This privacy policy is provided in good faith. The App is open-source,
allowing you to verify all claims by reviewing the source code.*
