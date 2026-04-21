package com.example.serial.connections

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.example.serial.interfaces.ConnectionListener
import com.example.serial.interfaces.SerialDeviceConnection
import com.example.serial.models.PlatformSerialPort
import kotlinx.coroutines.*
import java.io.IOException

/**
 * Android Implementation for USB-Serial communication.
 * Uses 'usb-serial-for-android' library to bridge Android USB Host to Serial.
 */
class AndroidUsbConnection(
    private val platformPort: PlatformSerialPort,
    private val baudRate: Int,
    private val dataBits: Int,
    private val stopBits: Int,
    private val parity: Int,
    private val context: Context // Injected context (e.g. from Hilt or ApplicationHolder)
) : SerialDeviceConnection {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var usbSerialPort: UsbSerialPort? = null
    private var listener: ConnectionListener? = null
    private var readJob: Job? = null
    
    // Dedicated scope for background I/O operations
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override var isConnected: Boolean = false
        private set

    private var lastContactTimestamp: Long = 0L
    private var isWatchdogPaused = false

    override fun setListener(listener: ConnectionListener) {
        this.listener = listener
    }

    override suspend fun connect() {
        withContext(Dispatchers.IO) {
            try {
                // Handling Emulator Mock for dev-friendliness
                if (platformPort.portName == "EMULATOR_MOCK_PORT") {
                    simulateMockConnection()
                    return@withContext
                }

                // 1. Find the correct driver from the available list
                val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
                var targetPort: UsbSerialPort? = null
                
                for (driver in availableDrivers) {
                    driver.ports.forEachIndexed { index, port ->
                        if ("${driver.device.deviceName}_$index" == platformPort.portName) {
                            targetPort = port
                        }
                    }
                }

                val port = targetPort ?: throw IOException("Hardware device ${platformPort.portName} not found")

                // 2. Request permission and open connection
                val connection = usbManager.openDevice(port.driver.device) 
                    ?: throw IOException("USB Permission denied. Please reconnect the device.")

                port.open(connection)
                port.setParameters(baudRate, dataBits, stopBits, parity)
                
                usbSerialPort = port
                isConnected = true
                lastContactTimestamp = System.currentTimeMillis()
                
                startReading()
                listener?.onInfo("Connected to Serial Device: ${platformPort.description}")
            } catch (e: Exception) {
                isConnected = false
                listener?.onFatalError(e)
            }
        }
    }

    /**
     * Continuous read loop. 
     * Uses a small delay to prevent CPU spinning on older Android hardware.
     */
    private fun startReading() {
        readJob?.cancel()
        readJob = scope.launch {
            val buffer = ByteArray(2048)
            while (isActive && isConnected) {
                try {
                    val port = usbSerialPort ?: break
                    val len = port.read(buffer, 1000)
                    if (len > 0) {
                        lastContactTimestamp = System.currentTimeMillis()
                        listener?.onDataReceived(buffer.copyOf(len))
                    }
                } catch (e: Exception) {
                    if (isConnected) {
                        listener?.onWarning("I/O Read Error: ${e.message}")
                        disconnect()
                    }
                }
                delay(20) // Power optimization
            }
        }
    }

    override suspend fun disconnect() {
        isConnected = false
        readJob?.cancel()
        withContext(Dispatchers.IO) {
            try {
                usbSerialPort?.close()
            } catch (e: Exception) { /* Silent close */ } finally {
                usbSerialPort = null
                listener?.onInfo("Serial Device disconnected safely.")
            }
        }
    }

    override suspend fun send(data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (platformPort.portName == "EMULATOR_MOCK_PORT") return@withContext true
                
                usbSerialPort?.write(data, 1000)
                true
            } catch (e: Exception) {
                listener?.onWarning("Send operation failed: ${e.message}")
                false
            }
        }
    }

    private fun simulateMockConnection() {
        isConnected = true
        lastContactTimestamp = System.currentTimeMillis()
        listener?.onInfo("EMULATOR: Connected to Virtual Mock Port")
        
        readJob = scope.launch {
            while(isActive && isConnected) {
                delay(5000)
                listener?.onInfo("EMULATOR: Mock heartbeat received.")
            }
        }
    }

    override fun getLastContactTimestamp(): Long = lastContactTimestamp
    override fun pauseWatchdog() { isWatchdogPaused = true }
    override fun resumeWatchdog() { isWatchdogPaused = false }
}