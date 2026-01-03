# Manual Testing Checklist

## Prerequisites
- [ ] Android device or emulator (API 26+)
- [ ] VPN server accessible
- [ ] Valid credentials

## Test Cases

### TC1: Basic Connection
- [ ] Open app
- [ ] Enter server hostname
- [ ] Enter username and password
- [ ] Tap Connect
- [ ] Verify VPN icon appears in status bar
- [ ] Expected: Connection established

### TC2: IP Address Assignment
- [ ] Connect to VPN
- [ ] Check assigned IP address in app
- [ ] Verify IP is from VPN subnet
- [ ] Expected: Valid IP assigned

### TC3: Internet Connectivity
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
- [ ] While connected, tap Disconnect
- [ ] Verify VPN icon disappears
- [ ] Verify internet works without VPN
- [ ] Expected: Clean disconnect

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

## Test Results

| Test Case | Date | Result | Notes |
|-----------|------|--------|-------|
| TC1 | | | |
| TC2 | | | |
| TC3 | | | |
| TC4 | | | |
| TC5 | | | |
| TC6 | | | |
| TC7 | | | |
