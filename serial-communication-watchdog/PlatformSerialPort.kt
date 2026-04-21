package com.example.serial.models

/**
 * Common data class to represent a serial port across platforms.
 */
data class PlatformSerialPort(
    val portName: String,
    val description: String,
    val manufacturer: String,
    val vendorId: Int
)