# Manual Testing Checklist

## Prerequisites

- [x] Android device or emulator (API 26+) - Tested on Android 15 (API 35) emulator
- [x] VPN server accessible - 172.104.148.166:992 (ping and port accessible)
- [x] Valid credentials - mac/LcqlJT9PBOkdsssJNbQ5, HUB: VPN

## Automated E2E Testing (Maestro)

### Prerequisites for Automated Testing

1. **Maestro installed**: `curl -fsSL "https://get.maestro.mobile.dev" | bash`
2. **Android emulator running**: `emulator -avd test_avd`
3. **App installed**: `./gradlew installDebug`
4. **Stylus handwriting disabled**: `adb shell settings put secure stylus_handwriting_enabled 0`

### Running the Automated E2E Test

```bash
# Configure VPN settings and run the test
python3 scripts/configure-vpn-settings.py
maestro test maestro/vpn-connect-test.yaml

# Or use the just command (includes all steps)
just test-vpn-e2e
```

### Automated Test Results (2026-01-05)

| Step | Result | Notes |
|------|--------|-------|
| Launch app | ✅ PASS | App launches successfully |
| Verify HOME visible | ✅ PASS | HOME tab is displayed |
| Verify settings configured | ✅ PASS | No "[No Value Entered]" fields |
| Take initial screenshot | ✅ PASS | `screenshots/vpn-initial.png` |
| Tap Connect | ✅ PASS | Connect toggle activated |
| Handle VPN permission | ⚪ SKIPPED | Permission already granted |
| Wait for connection | ✅ PASS | "Connected/Established/Connecting" detected |
| Take connected screenshot | ✅ PASS | `screenshots/vpn-connected.png` |
| Tap Connect (disconnect) | ✅ PASS | Disconnect initiated |
| Verify disconnected | ✅ PASS | "[No Connection Established]" visible |
| Take disconnected screenshot | ✅ PASS | `screenshots/vpn-disconnected.png` |

**Overall Result: ✅ PASS** - VPN connection verified working on 2026-01-05

## Manual Test Cases

### TC1: Basic Connection

- [x] Open app
- [x] Enter server hostname
- [x] Enter username and password
- [x] Tap Connect
- [x] Verify VPN connection established
- [x] Expected: Connection established
- **Result: PASS** (verified via automated E2E test on 2026-01-05)

### TC2: VPN Routing Verification (Ping Test)

- [x] Connect to VPN
- [x] Ping 192.168.42.1 from emulator via adb shell
- [x] Verify ping responses received
- [x] Expected: Successful ping to VPN gateway
- **Result: PASS** (verified on 2026-01-05)
- **Details**: 4 packets transmitted, 4 received, 0% packet loss
- **Command**: `adb shell ping -c 4 192.168.42.1`
- **Output**:

```text
PING 192.168.42.1 (192.168.42.1) 56(84) bytes of data.
64 bytes from 192.168.42.1: icmp_seq=2 ttl=255 time=1122 ms
64 bytes from 192.168.42.1: icmp_seq=1 ttl=255 time=2136 ms
64 bytes from 192.168.42.1: icmp_seq=3 ttl=255 time=99.7 ms
64 bytes from 192.168.42.1: icmp_seq=4 ttl=255 time=156 ms

--- 192.168.42.1 ping statistics ---
4 packets transmitted, 4 received, 0% packet loss, time 3040ms
rtt min/avg/max/mdev = 99.793/878.756/2136.667/832.263 ms, pipe 3
```

### TC3: IP Address Assignment

- [ ] Connect to VPN
- [ ] Check assigned IP address in app
- [ ] Verify IP is from VPN subnet
- [ ] Expected: Valid IP assigned

### TC4: Internet Connectivity

- [ ] Connect to VPN
- [ ] Open browser
- [ ] Navigate to https://whatismyip.com
- [ ] Verify IP matches VPN server's public IP
- [ ] Expected: Traffic routed through VPN

### TC4: UDP Acceleration

- [ ] Enable UDP acceleration in settings
- [ ] Connect to VPN
- [ ] Verify UDP mode is active (check logs)
- [ ] Expected: UDP acceleration working

### TC5: Disconnect

- [x] While connected, tap Disconnect
- [x] Verify VPN disconnects
- [x] Expected: Clean disconnect
- **Result: PASS** (verified via automated E2E test on 2026-01-05)

### TC6: Reconnection

- [ ] Connect to VPN
- [ ] Disconnect
- [ ] Connect again
- [ ] Expected: Successful reconnection

### TC7: Background Persistence

- [ ] Connect to VPN
- [ ] Press Home button
- [ ] Wait 5 minutes
- [ ] Return to app
- [ ] Verify still connected
- [ ] Expected: Connection persists

## Test Results Summary

| Test Case | Date | Result | Notes |
|-----------|------|--------|-------|
| TC1 | 2026-01-05 | PASS | Verified via Maestro E2E test |
| TC2 | 2026-01-05 | PASS | Ping to 192.168.42.1 successful (0% packet loss) |
| TC3 | - | PENDING | Manual verification needed |
| TC4 | - | PENDING | Manual verification needed |
| TC5 | - | PENDING | Manual verification needed |
| TC6 | 2026-01-05 | PASS | Verified via Maestro E2E test |
| TC7 | - | PENDING | Manual verification needed |
| TC8 | - | PENDING | Manual verification needed |

## Pre-Connection Testing Results

The following aspects were verified before connection testing:

| Aspect | Date | Result | Notes |
|--------|------|--------|-------|
| App Installation | 2026-01-05 | PASS | App installs and launches without crash |
| UI Navigation | 2026-01-05 | PASS | All screens accessible |
| Settings Entry | 2026-01-05 | PASS | Can enter hostname, hub, username, password |
| VPN Permission Dialog | 2026-01-05 | PASS | VPN permission dialog appears when needed |
| Foreground Service | 2026-01-05 | PASS | FGS starts successfully (Android 14+ compatible) |
| Network Connectivity | 2026-01-05 | PASS | Emulator can ping VPN server and reach port 992 |
| VPN Connection | 2026-01-05 | PASS | Connection established and verified via E2E test |
| VPN Disconnection | 2026-01-05 | PASS | Clean disconnect verified via E2E test |

## E2E Test Files

The following files are used for automated E2E testing:

- `maestro/vpn-connect-test.yaml` - Maestro flow for VPN connect/disconnect test
- `scripts/configure-vpn-settings.py` - Python script to configure VPN settings via SharedPreferences
- `scripts/vpn-e2e-test.sh` - Bash wrapper script for running the full E2E test
- `.env` - VPN credentials (not committed to git)

## Screenshots

Test screenshots are saved to the `screenshots/` directory:

- `vpn-initial.png` - App state before connection
- `vpn-connected.png` - App state during connection
- `vpn-disconnected.png` - App state after disconnection

## How to Connect in Android Emulator

### Method 1: Using Maestro (Automated)

```bash
# 1. Configure VPN settings from .env file
python3 scripts/configure-vpn-settings.py

# 2. Connect and stay connected
maestro test maestro/vpn-connect-only.yaml

# 3. Verify VPN is working by pinging the gateway
adb shell ping -c 4 192.168.42.1
```

### Method 2: Manual UI Interaction

1. **Start the emulator**:

   ```bash
   emulator -avd test_avd
   ```

2. **Install the app**:

   ```bash
   ./gradlew installDebug
   ```

3. **Launch the app**:

   ```bash
   adb shell am start -n kittoku.mvc/.MainActivity
   ```

4. **Configure VPN settings** (via script or manually):

   ```bash
   # Option A: Use the configuration script
   python3 scripts/configure-vpn-settings.py

   # Option B: Enter manually in the app UI
   # - Tap Hostname → Enter server address
   # - Tap Virtual HUB Name → Enter hub name
   # - Tap Username → Enter username
   # - Tap Password → Enter password
   ```

5. **Tap the Connect toggle** to establish the VPN connection

6. **Accept VPN permission** if prompted (tap "OK" or "Allow")

7. **Verify connection**:
   - The "Current Status" should show connection info
   - Run `adb shell ping -c 4 192.168.42.1` to verify routing

### VPN Settings Reference

| Setting | Value | Notes |
|---------|-------|-------|
| Hostname | 172.104.148.166 | VPN server address |
| SSL Port | 992 | Default SoftEther port |
| Virtual HUB Name | VPN | Hub name on server |
| Username | mac | VPN account username |
| Password | (from .env) | VPN account password |

## Known Issues

1. **SharedPreferences format** - SSL_PORT must be stored as a string, not an int (app uses getString().toIntOrNull())
2. **Stylus handwriting interference** - Must disable stylus handwriting on emulator: `adb shell settings put secure stylus_handwriting_enabled 0`

See `docs/known-issues.md` for full details.
