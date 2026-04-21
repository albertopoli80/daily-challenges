package com.example.serial.interfaces

import java.io.IOException

/**
 * Core interface for Serial Device Connections.
 * Optimized for external hardware connected via USB or Serial.
 */
interface SerialDeviceConnection {
    val isConnected: Boolean

    fun setListener(listener: ConnectionListener)

    /** Establishes the connection to the external device. */
    suspend fun connect()

    /** Closes the connection and releases hardware resources. */
    suspend fun disconnect()

    /** Sends data to the serial device. Returns true if successful. */
    suspend fun send(data: ByteArray): Boolean

    /** Returns the timestamp of the last successful data exchange. */
    fun getLastContactTimestamp(): Long

    /** Pauses the watchdog (useful during long tasks like data sync). */
    fun pauseWatchdog()

    /** Resumes the watchdog monitoring. */
    fun resumeWatchdog()
}

/**
 * Observer for hardware connection events and data flow.
 */
interface ConnectionListener {
    fun onDataReceived(data: ByteArray)
    fun onInfo(message: String)
    fun onWarning(message: String)
    fun onFatalError(error: Exception)
}