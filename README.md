# Daily Android & Kotlin Challenges

A collection of high-performance code snippets, architectural patterns, and mathematical optimizations for modern Android development.

## 📂 Project Structure

### ⚡ UI & Responsiveness
- **[Kotlin UI Responsiveness](./kotlin-ui-responsiveness)**: Comparison between `Dispatchers.Default` and `Dispatchers.Main.immediate` for high-frequency UI events.
- **[Redux vs. UI State](./redux-vs-uistate)**: Exploring State Management patterns in Jetpack Compose.

### 🔊 Audio & Signal Processing
- **[Gaussian Smoothing Filter](./kotlin-smoothing-low-pass-filter)**: An adaptive low-pass filter using Gaussian kernels and derivative analysis for drum trigger detection.
- **[FFT Analyzer](./simple-fft-analyzer)**: Real-time Fast Fourier Transform with zero-allocation in-place calculations and "Virtual Sensor" logic.
- **[MIDI-over-UDP](./midi-udp-sender)**: Low-latency wireless MIDI triggering for DAWs.

### 🔌 Hardware & Communication
- **[Serial Device Connection](./serial-communication-watchdog)**: A resilient KMP (JVM/Android) serial handler with a coroutine-based watchdog and buffer purging.
- **[Camera Color Picker](./android-camera-color-picker)**: High-performance circular color extraction using scanline geometric pruning directly from YUV camera buffers.

### 🏛️ Architecture & Best Practices
- **[Modular Circular Dependency](./modular-circular-dependency)**: Solving module circularity using Dependency Inversion with Hilt and API/Implementation splitting.

---

## 🚀 Key Philosophy
Each snippet in this repository focuses on:
1. **Performance**: Zero-allocation loops, efficient threading, and mathematical optimizations.
2. **Readability**: Clean, idiomatic Kotlin with professional English documentation.
3. **Real-World Scenarios**: Every challenge solves a specific, practical problem faced by Android developers.
