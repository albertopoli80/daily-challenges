package com.example.adaptivebpm.data.midi

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * MIDI over UDP Sender.
 * Sends raw MIDI byte arrays to a specific network host and port.
 * 
 * SCENARIO: Sending real-time MIDI triggers from an Android device 
 * to a Digital Audio Workstation (DAW) over a local network (Wi-Fi).
 */
class MidiUdpSender(
    private val host: String,
    private val port: Int
) {
    // DatagramSocket for connectionless UDP transmission
    private var socket: DatagramSocket? = DatagramSocket()
    
    // Resolve host address once to minimize overhead during transmission
    private val address: InetAddress = InetAddress.getByName(host)

    /**
     * Sends a byte array representing a MIDI message (e.g., Note On, Note Off).
     * @param bytes The raw MIDI bytes to transmit.
     */
    fun send(bytes: ByteArray) {
        try {
            // UDP packets are lightweight, perfect for low-latency MIDI triggers
            val packet = DatagramPacket(bytes, bytes.size, address, port)
            socket?.send(packet)
        } catch (e: Exception) {
            // Silent error during transmission to prevent UI/Audio thread blocking
            // In a production environment, consider logging or counting errors
        }
    }

    /**
     * Safely closes the socket and releases network resources.
     */
    fun close() {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            // Ignore errors during closure
        }
    }
}