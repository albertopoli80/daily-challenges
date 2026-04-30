# Resilient Serial Communication with Coroutine Watchdog

This challenge implements a production-grade serial communication handler using `jSerialComm` and Kotlin Coroutines. It's designed for environments where hardware stability cannot be guaranteed (e.g., Serial Android Devices, industrial sensors).

## Key Features

### 1. The Coroutine Watchdog
A passive inactivity timer that monitors the `lastContactTimestamp`. 
- **Automatic Recovery**: If the device stops responding for more than 15 seconds, the watchdog triggers a `handleConnectionLoss`, forcing a clean resource teardown and notifying the system.
- **Pausable**: Essential for long-running operations like firmware updates where inactivity is expected and shouldn't trigger a disconnect.

### 2. Zero-Stale Data (Buffer Purge)
On every successful connection, we call `flushIOBuffers()`.
- **The Problem**: Operating systems often cache serial data even when your app isn't listening. When you reconnect, you might receive "ghost" messages from minutes ago.
- **The Solution**: Purging buffers ensures that the first byte you read is the first byte sent *after* the connection was established.

### 3. Thread-Safe State Management
Using a `Mutex`, we ensure that `connect()` and `disconnect()` operations never overlap, preventing race conditions that could lead to "Port Busy" errors or leaked handles.

### 4. Hardware Disconnect Detection
By listening for `LISTENING_EVENT_PORT_DISCONNECTED`, the handler reacts instantly if the USB cable is physically pulled, rather than waiting for a timeout.

## Best Practices Included
- **Non-Blocking I/O**: Heavy operations (open, close, write) are offloaded to `Dispatchers.IO`.
- **Memory Safety**: Uses a `SupervisorJob` to ensure that a failure in the watchdog doesn't crash the entire communication scope.
- **Graceful Cleanup**: Even in a crash, we attempt to close the `InputStream`, `OutputStream`, and the `SerialPort` to prevent OS-level port locking.
