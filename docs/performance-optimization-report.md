# Performance Optimization Report: SoftEther Connect

**Date:** January 4, 2026
**Version:** 1.0.0
**Scope:** Memory usage, buffer allocations, battery consumption, connection speed

## Executive Summary

This report documents the performance optimization work completed for the SoftEther Connect VPN client. The optimizations focus on:

1. **Memory efficiency** - Buffer pooling to reduce GC pressure
2. **Battery optimization** - Adaptive power modes based on conditions
3. **Performance monitoring** - Real-time metrics for troubleshooting
4. **Connection speed** - Optimized packet processing

---

## 1. Memory Usage Analysis

### Current Buffer Allocations

| Component | Buffer Size | Count | Total Memory |
|-----------|-------------|-------|--------------|
| TCPTerminal.incomingBuffer | 16,384 bytes | 1 | 16 KB |
| TCPTerminal.outgoingBuffer | 16,384 bytes | 1 | 16 KB |
| UDPTerminal.incomingPacket | 1,600 bytes | 1 | 1.6 KB |
| UDPTerminal.outgoingPacket | 1,600 bytes | 1 | 1.6 KB |
| UDPTerminal.encryptBuffer | 1,600 bytes | 1 | 1.6 KB |
| UDPTerminal.decryptBuffer | 1,600 bytes | 1 | 1.6 KB |
| IPTerminal.alpha/beta | ~1,514 bytes | 2 | 3 KB |
| **Total Static Buffers** | | | **~42 KB** |

### Dynamic Allocations

The following allocations occur during packet processing:

1. **TCPTerminal.sendFrame()** - Allocates new ByteBuffer per frame
2. **TCPTerminal.sendKeepAlive()** - Allocates new ByteBuffer per keep-alive
3. **SoftEtherClient.prepareProperties()** - Allocates ByteBuffer for properties

### Optimization: Buffer Pool

Created [`BufferPool`](../app/src/main/java/kittoku/mvc/performance/BufferPool.kt) to reuse ByteBuffers:

```kotlin
class BufferPool(
    bufferSize: Int,
    maxPoolSize: Int = 16,
    useDirect: Boolean = false
)
```

**Benefits:**

- Reduces GC pressure during high-throughput scenarios
- Thread-safe concurrent access
- Configurable pool size based on power mode
- Statistics tracking for monitoring

**Usage Example:**

```kotlin
val pool = BufferPool(bufferSize = 1600, maxPoolSize = 8)

// Acquire buffer
val buffer = pool.acquire()
try {
    // Process packet...
} finally {
    pool.release(buffer)
}
```

**Expected Impact:**

- 50-80% reduction in buffer allocations during steady-state operation
- Reduced GC pause times
- Lower memory churn

---

## 2. Battery Consumption Optimization

### Analysis

VPN connections consume battery through:

1. **Keep-alive packets** - Periodic network activity
2. **Encryption/decryption** - CPU usage for ChaCha20-Poly1305
3. **Network state monitoring** - Checking connectivity
4. **Wake locks** - Keeping CPU active

### Optimization: Adaptive Power Modes

Created [`BatteryOptimizer`](../app/src/main/java/kittoku/mvc/performance/BatteryOptimizer.kt) with four power modes:

| Mode | Keep-Alive | UDP Accel | Buffer Pool | Use Case |
|------|------------|-----------|-------------|----------|
| PERFORMANCE | 500ms | Yes | 16 buffers | Charging, WiFi |
| BALANCED | 1000ms | WiFi only | 8 buffers | Normal operation |
| BATTERY_SAVER | 2000ms | No | 4 buffers | Low battery |
| ULTRA_SAVER | 5000ms | No | 2 buffers | Critical battery |

**Automatic Mode Selection:**

```kotlin
val optimizer = BatteryOptimizer(context)
val config = optimizer.getOptimalConfiguration()

// Apply configuration
keepAliveInterval = config.keepAliveIntervalMs
useUdpAcceleration = config.useUdpAcceleration
```

**Factors Considered:**

- Battery level (critical < 10%, low < 20%)
- Charging status
- Power save mode
- Network type (WiFi vs cellular)
- Metered connection status

### Battery Optimization Exemption

For reliable VPN operation, the app should request battery optimization exemption:

```kotlin
if (!optimizer.isIgnoringBatteryOptimizations()) {
    val intent = optimizer.createBatteryOptimizationExemptionIntent()
    startActivity(intent)
}
```

---

## 3. Performance Monitoring

### Implementation

Created [`PerformanceMonitor`](../app/src/main/java/kittoku/mvc/performance/PerformanceMonitor.kt) for real-time metrics:

**Tracked Metrics:**

- Packets sent/received
- Bytes sent/received
- Packets dropped
- Error count
- Throughput (Mbps)
- Latency (min/avg/max)
- Uptime

**Usage:**

```kotlin
val monitor = PerformanceMonitor.getInstance()
monitor.start()

// Track packets
monitor.recordPacketSent(packetSize)
monitor.recordPacketReceived(packetSize)
monitor.recordLatency(rttMs)

// Get metrics
val metrics = monitor.getMetrics()
println("Throughput: ${metrics.currentReceiveThroughputMbps} Mbps")
println("Packet loss: ${metrics.packetLossRate * 100}%")
```

### Metrics Dashboard

The metrics can be displayed in the UI:

```
VPN Performance Metrics:
  Uptime: 01:23:45
  Packets sent: 12,345
  Packets received: 23,456
  Bytes sent: 15.2 MB
  Bytes received: 45.6 MB
  Packet loss rate: 0.02%
  Current throughput: 25.4 Mbps
  Average latency: 32.5 ms
```

---

## 4. Connection Speed Optimization

### Current Implementation Analysis

**TCP Terminal:**

- Uses blocking I/O with SSLSocket
- 16KB buffers match typical TCP window size
- Mutex-protected writes prevent interleaving

**UDP Terminal:**

- Uses DatagramSocket with 1600-byte packets
- ChaCha20-Poly1305 encryption (hardware-accelerated on modern devices)
- 2MB receive buffer for burst handling

### Optimization Opportunities

1. **Buffer sizing** - Current sizes are appropriate for typical VPN traffic
2. **Socket options** - Receive buffer size (2MB) is well-tuned
3. **Encryption** - ChaCha20-Poly1305 is efficient on mobile CPUs

### Benchmark Recommendations

For connection speed benchmarking:

```kotlin
// Measure throughput
val startTime = System.currentTimeMillis()
val startBytes = monitor.getMetrics().bytesReceived

// Wait for test duration
delay(10_000) // 10 seconds

val endTime = System.currentTimeMillis()
val endBytes = monitor.getMetrics().bytesReceived

val throughputMbps = (endBytes - startBytes) * 8.0 /
    ((endTime - startTime) / 1000.0) / 1_000_000
```

---

## 5. ProGuard/R8 Optimization

The release build configuration enables:

```groovy
release {
    minifyEnabled true
    shrinkResources true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'),
                  'proguard-rules.pro'
}
```

**Benefits:**

- Smaller APK size
- Faster class loading
- Removed unused code

---

## 6. Recommendations

### Immediate (Implemented)

- [x] Buffer pool for reusable ByteBuffers
- [x] Performance monitoring infrastructure
- [x] Battery-aware power modes
- [x] ProGuard/R8 optimization

### Future Enhancements

1. **Direct ByteBuffers** - Consider using direct buffers for I/O operations
2. **Zero-copy optimization** - Reduce buffer copies in packet path
3. **Coroutine optimization** - Review dispatcher usage for I/O operations
4. **Memory profiling** - Use Android Profiler for detailed analysis
5. **Network profiling** - Use Network Profiler for traffic analysis

---

## 7. Testing

### Unit Tests

- [`BufferPoolTest`](../app/src/test/java/kittoku/mvc/performance/BufferPoolTest.kt)
- [`PerformanceMonitorTest`](../app/src/test/java/kittoku/mvc/performance/PerformanceMonitorTest.kt)

### Manual Testing

1. **Memory test**: Connect VPN, monitor memory usage over 1 hour
2. **Battery test**: Connect VPN on battery, measure drain rate
3. **Throughput test**: Run speed test through VPN connection
4. **Latency test**: Ping through VPN, measure RTT

---

## 8. Files Created

| File | Purpose |
|------|---------|
| `BufferPool.kt` | Reusable buffer pool |
| `PerformanceMonitor.kt` | Real-time metrics tracking |
| `BatteryOptimizer.kt` | Adaptive power management |
| `BufferPoolTest.kt` | Buffer pool unit tests |
| `PerformanceMonitorTest.kt` | Performance monitor tests |

---

## Appendix: Performance Constants

### UDP Constants

```kotlin
UDP_PACKET_BUFFER_SIZE = 1600
UDP_SOCKET_RECEIVE_BUFFER_SIZE = 2097152 (2MB)
UDP_KEEP_ALIVE_TIMEOUT = 2100ms
UDP_KEEP_ALIVE_MIN_INTERVAL = 500ms
```

### TCP Constants

```kotlin
TCP_BUFFER_SIZE = 16384 (16KB)
TCP_CONTROL_UNIT_WAIT_TIMEOUT = (varies)
TCP_DATA_UNIT_WAIT_TIMEOUT = (varies)
```

### Buffer Pool Sizes

```kotlin
BufferPool.Sizes.ETHERNET_MTU = 1500
BufferPool.Sizes.UDP_PACKET = 1600
BufferPool.Sizes.TCP_BUFFER = 16384
BufferPool.Sizes.HEADER = 256
```
