# MIDI-over-UDP: Low Latency Network Triggering

This module provides a lightweight solution for sending MIDI commands from an Android device to a Digital Audio Workstation (DAW) over a local network (Wi-Fi).

## Real-World Scenario: The Wireless Drummer
In modern stage setups, drummers often use mobile devices or sensors to trigger sounds on a remote computer (DAW) running software like Ableton Live or MainStage.
- **Problem**: USB or Bluetooth connections have physical range limits.
- **Solution**: UDP (User Datagram Protocol) allows wireless transmission across the entire stage.

## Why UDP?
We choose UDP over TCP for several critical reasons in the context of live music:
1. **Low Latency**: UDP has no "handshake" or re-transmission logic. If a packet is lost, it's better to miss one note than to delay all future notes while waiting for a retry.
2. **Speed**: MIDI messages (3 bytes) are tiny. UDP's header overhead is minimal, ensuring the trigger feels "instant" to the performer.
3. **Non-Blocking**: The `send()` operation doesn't wait for a response, ensuring the audio/processing thread isn't stalled by network congestion.

## Usage Guide
Ensure your Android device and DAW are on the same local network. Configure the DAW's MIDI-over-UDP listener to match the IP and port specified in the `MidiUdpSender` instance.
