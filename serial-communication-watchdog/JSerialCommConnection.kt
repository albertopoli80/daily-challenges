package com.example.serial.connections

import com.fazecast.jSerialComm.SerialPort
import com.fazecast.jSerialComm.SerialPortDataListener
import com.fazecast.jSerialComm.SerialPortEvent
import com.example.serial.interfaces.ConnectionListener
import com.example.serial.interfaces.SerialDeviceConnection
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private const val READ_TIMEOUT_MS = 100
private const val WATCHDOG_INTERVAL_MS = 1000L
private const val CONNECTION_TIMEOUT_MS = 15000L

/**
 * Robust Serial Implementation using jSerialComm.
 * Features: Automatic buffer flushing, Coroutine-based Watchdog, and Thread-safe state management.
 */
class JSerialCommConnection(
    private val portName: String,
    private val baudRate: Int,
    private val dataBits: Int,
    private val stopBits: Int,
    private val parity: Int
) : SerialDeviceConnection, SerialPortDataListener {

    private var serialPort: SerialPort? = null
    private var listener: ConnectionListener? = null
    
    @Volatile private var _isConnected: Boolean = false

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var watchdogJob: Job? = null
    
    @Volatile private var lastContactTimestamp: Long = 0L
    @Volatile private var isWatchdogPaused: Boolean = false
    private val stateMutex = Mutex()

    override val isConnected: Boolean
        get() = _isConnected && serialPort?.isOpen == true

    override fun getLastContactTimestamp(): Long = lastContactTimestamp

    override fun setListener(listener: ConnectionListener) {
        this.listener = listener
    }

    override suspend fun connect() {
        if (isConnected) return
        
        stateMutex.withLock {
            try {
                serialPort = SerialPort.getCommPort(portName).apply {
                    setComPortParameters(baudRate, dataBits, stopBits, parity)
                    // SEMI_BLOCKING allows efficient reading without spinning the CPU
                    setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0)
                }

                val portOpened = withContext(Dispatchers.IO) { serialPort?.openPort() }
                
                if (portOpened == true) {
                    // BUFFER PURGE: Critical to discard stale data left in the OS serial buffer
                    withContext(Dispatchers.IO) {
                        serialPort?.flushIOBuffers()
                    }
                    
                    _isConnected = true
                    lastContactTimestamp = System.currentTimeMillis()
                    serialPort?.addDataListener(this)
                    startWatchdog()
                    listener?.onInfo("Serial port $portName opened. Buffers flushed.")
                } else {
                    _isConnected = false
                    listener?.onWarning("Failed to open serial port $portName.")
                }
            } catch (e: Exception) {
                _isConnected = false
                listener?.onFatalError(IOException("Initialization error on $portName: ${e.message}", e))
            }
        }
    }

    override suspend fun disconnect() {
        handleConnectionLoss(IOException("Manual disconnect triggered."), isManualDisconnect = true)
    }

    override suspend fun send(data: ByteArray): Boolean {
        if (!isConnected) return false
        
        return try {
            val bytesWritten = withContext(Dispatchers.IO) {
                serialPort?.writeBytes(data, data.size.toLong())
            } ?: 0
            
            (bytesWritten > 0).also { success ->
                if (!success) listener?.onWarning("Write operation returned 0 bytes.")
            }
        } catch (e: Exception) {
            handleConnectionLoss(IOException("Write failed: ${e.message}", e))
            false
        }
    }

    // --- SerialPortDataListener Implementation ---

    override fun getListeningEvents(): Int {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE or SerialPort.LISTENING_EVENT_PORT_DISCONNECTED
    }

    override fun serialEvent(event: SerialPortEvent) {
        when (event.eventType) {
            SerialPort.LISTENING_EVENT_DATA_AVAILABLE -> {
                lastContactTimestamp = System.currentTimeMillis()
                val port = event.serialPort
                val numBytes = port?.bytesAvailable() ?: 0
                
                if (numBytes > 0) {
                    val buffer = ByteArray(numBytes)
                    val bytesRead = port?.readBytes(buffer, buffer.size.toLong()) ?: 0
                    if (bytesRead > 0) {
                        listener?.onDataReceived(buffer.copyOf(bytesRead))
                    }
                }
            }
            SerialPort.LISTENING_EVENT_PORT_DISCONNECTED -> {
                handleConnectionLoss(IOException("Hardware disconnected: $portName"))
            }
        }
    }

    // --- Watchdog Logic ---

    override fun pauseWatchdog() { isWatchdogPaused = true }
    override fun resumeWatchdog() { isWatchdogPaused = false }

    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                val now = System.currentTimeMillis()
                
                if (!isWatchdogPaused && _isConnected && (now - lastContactTimestamp > CONNECTION_TIMEOUT_MS)) {
                    handleConnectionLoss(IOException("Inactivity timeout on $portName."))
                    break
                }
            }
        }
    }

    private fun stopWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    /**
     * Centralized cleanup for connection loss.
     * Ensures streams are closed and listeners removed without blocking the caller.
     */
    private fun handleConnectionLoss(error: IOException, isManualDisconnect: Boolean = false) {
        scope.launch {
            val needsCleanup = stateMutex.withLock {
                if (_isConnected) {
                    _isConnected = false
                    true
                } else {
                    false
                }
            }

            if (needsCleanup) {
                stopWatchdog()
                serialPort?.removeDataListener()

                // Graceful resource release on IO dispatcher
                withContext(Dispatchers.IO) {
                    try { serialPort?.inputStream?.close() } catch (_: Exception) {}
                    try { serialPort?.outputStream?.close() } catch (_: Exception) {}
                    serialPort?.closePort()
                }

                serialPort = null
                if (!isManualDisconnect) listener?.onFatalError(error)
                else listener?.onInfo("Connection closed safely.")
            }
        }
    }
}