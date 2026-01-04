# App Store Assets Guide

This document describes the required assets for publishing SoftEther Connect
on Google Play Store and other Android app stores.

## App Icon

### Requirements

| Asset | Size | Format | Notes |
|-------|------|--------|-------|
| App Icon | 512x512 px | PNG (32-bit) | No transparency, no rounded corners |
| Adaptive Icon Foreground | 108x108 dp | PNG/Vector | Safe zone: 66x66 dp center |
| Adaptive Icon Background | 108x108 dp | PNG/Vector/Color | Can be solid color |

### Current Icons

Located in `app/src/main/res/`:

- `mipmap-hdpi/` - 72x72 px
- `mipmap-mdpi/` - 48x48 px
- `mipmap-xhdpi/` - 96x96 px
- `mipmap-xxhdpi/` - 144x144 px
- `mipmap-xxxhdpi/` - 192x192 px
- `mipmap-anydpi-v26/` - Adaptive icon XML

### Design Guidelines

- Use the SoftEther brand colors (blue/green gradient)
- Include a VPN shield or lock icon
- Ensure icon is recognizable at small sizes
- Follow Material Design icon guidelines

## Feature Graphic

**Size:** 1024x500 px
**Format:** PNG or JPEG (24-bit, no alpha)

Used as the banner at the top of the Play Store listing.

### Design Tips

- Show the app name prominently
- Include key visual elements (VPN shield, connection status)
- Use brand colors
- Avoid text that may be hard to read on small screens

## Screenshots

### Phone Screenshots

**Size:** 16:9 or 9:16 aspect ratio
**Minimum:** 320 px on shortest side
**Maximum:** 3840 px on longest side
**Format:** PNG or JPEG (24-bit)
**Required:** 2-8 screenshots

### Tablet Screenshots (7-inch)

**Size:** Same requirements as phone
**Required:** Optional but recommended

### Tablet Screenshots (10-inch)

**Size:** Same requirements as phone
**Required:** Optional but recommended

### Recommended Screenshots

1. **Home Screen** - Main connection interface
2. **Connected State** - VPN connected with statistics
3. **Profile List** - Multiple VPN profiles
4. **Settings** - Configuration options
5. **Split Tunneling** - App selection for split tunnel
6. **Statistics** - Connection history and data usage
7. **Dark Mode** - App in dark theme
8. **Quick Settings** - Quick settings tile

### Screenshot Guidelines

- Use real device frames (optional)
- Add captions highlighting features
- Show the app in both light and dark modes
- Ensure text is readable
- Avoid showing sensitive information

## Promotional Video (Optional)

**Format:** YouTube URL
**Length:** 30 seconds to 2 minutes recommended

### Content Ideas

- Quick demo of connecting to VPN
- Show key features (profiles, split tunneling)
- Highlight security and privacy features
- Show cross-device compatibility

## Store Listing Text

### App Title

**Maximum:** 30 characters

```text
SoftEther Connect
```

### Short Description

**Maximum:** 80 characters

```text
Secure VPN client for SoftEther VPN servers. Fast, private, open-source.
```

### Full Description

**Maximum:** 4000 characters

```text
SoftEther Connect is a modern, open-source VPN client for Android that
connects to SoftEther VPN servers. Built with security and privacy in mind.

🔒 SECURITY FEATURES
• End-to-end encryption using SSL/TLS
• Secure credential storage with Android Keystore
• No data collection or tracking
• Open-source and auditable

⚡ PERFORMANCE
• Fast connection speeds
• UDP acceleration support
• Battery-optimized for mobile use
• Adaptive power management

📱 FEATURES
• Multiple connection profiles
• Split tunneling (per-app VPN)
• Auto-connect on boot
• Quick Settings tile
• Connection statistics
• Dark mode support

🌐 COMPATIBILITY
• Works with any SoftEther VPN server
• Supports NAT traversal
• IPv4 support

📖 OPEN SOURCE
SoftEther Connect is free and open-source software. View the source code,
report issues, or contribute on GitHub.

🔐 PRIVACY
We do not collect any personal data. All connection information stays on
your device. See our privacy policy for details.

REQUIREMENTS
• Android 8.0 (Oreo) or higher
• Access to a SoftEther VPN server

SUPPORT
For help and support, visit our GitHub repository or contact us via email.
```

## Content Rating

Complete the content rating questionnaire on Google Play Console.

**Expected Rating:** PEGI 3 / Everyone

- No violence
- No sexual content
- No gambling
- No user-generated content
- No personal data collection

## Privacy Policy

**Required:** Yes

URL to host: `https://your-domain.com/privacy-policy`

See `docs/PRIVACY_POLICY.md` for the full privacy policy text.

## App Category

**Primary:** Tools
**Secondary:** Communication (optional)

## Contact Information

**Email:** Required for Play Store
**Website:** Optional but recommended
**Phone:** Optional

## Asset Checklist

- [ ] App icon (512x512 PNG)
- [ ] Adaptive icon foreground
- [ ] Adaptive icon background
- [ ] Feature graphic (1024x500)
- [ ] Phone screenshots (minimum 2)
- [ ] Tablet 7" screenshots (optional)
- [ ] Tablet 10" screenshots (optional)
- [ ] Promotional video (optional)
- [ ] Short description (80 chars)
- [ ] Full description (4000 chars)
- [ ] Privacy policy URL
- [ ] Content rating questionnaire

## Tools for Creating Assets

### Icon Design

- [Figma](https://figma.com) - Free design tool
- [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/)
- [Inkscape](https://inkscape.org) - Free vector graphics

### Screenshots

- Android Emulator (built-in screenshot)
- [Screener](https://screener.io) - Device frames
- [AppMockUp](https://app-mockup.com) - Screenshot generator

### Feature Graphic

- [Canva](https://canva.com) - Easy graphic design
- [Figma](https://figma.com) - Professional design

## Generating Screenshots

Use the emulator to capture screenshots:

```bash
# Start emulator
just emulator-start-gui

# Install app
just install

# Take screenshot
adb exec-out screencap -p > screenshot.png
```

Or use Android Studio's built-in screenshot tool.
