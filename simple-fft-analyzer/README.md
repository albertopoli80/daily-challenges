# FFT Real-Time Analyzer: The "Virtual Sensor" Engine

This module implements a high-performance Fast Fourier Transform (FFT) designed for real-time audio analysis on Android.

## Real-World Scenario: Frequency-Selective UI
Instead of just showing a generic volume meter, we want to create "Virtual Sensors" that react to different instruments within a single audio stream.
- **Visualizer**: A smooth frequency graph for the UI.
- **Low-Frequency Sensor**: Specifically triggers UI pulses or events when the **Kick Drum** (Bass) is detected.
- **High-Frequency Sensor**: Reacts to the "Sparkle" or **Cymbals**, allowing the UI to shimmer in sync with high-end transients.

## Architecture: Flow-Based Pipeline
To ensure the UI remains responsive, we recommend wrapping the analyzer in a Kotlin Coroutine Flow:
- **Offloading**: Use `.flowOn(Dispatchers.Default)` to move the FFT logic away from the Main Thread.
- **Multicasting**: Use `.shareIn()` so that multiple UI components (e.g., a waveform graph and a bass-drum sensor) can consume the same data without redundant calculations.

## Performance Engineering
Running FFT 60 times per second on a mobile device can easily cause UI stuttering or memory pressure. Our implementation includes:
1. **Iterative Cooley-Tukey Algorithm**: We avoid recursion to save stack memory and prevent `StackOverflowError` on deep buffers.
2. **In-place Bit-Reversal**: Samples are sorted as they are windowed, reducing the number of passes through the data.
3. **Zero Allocation in Loop**: All buffers (`real`, `imag`, `magnitudes`) are pre-allocated and reused. This prevents the Garbage Collector (GC) from triggering "Stop-the-world" pauses that cause audio crackling.
4. **Hamming Window**: Applied to the input to reduce "spectral leakage," ensuring the peaks in the UI are sharp and accurate.

## Why it matters
By dividing the spectrum into bands, we transform a simple microphone into a multi-channel input device. You don't just "hear" sound; you "identify" which part of the music is happening.
