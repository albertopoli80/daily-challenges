package com.example.adaptivebpm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.adaptivebpm.domain.processor.FftAnalyzer
import com.example.adaptivebpm.domain.processor.FftAnalyzer.BandEnergy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

/**
 * Example ViewModel demonstrating how to expose FFT data to the UI.
 * 
 * We use a Flow-based pipeline to transform raw audio buffers into 
 * high-level frequency data, ensuring the heavy lifting happens off-thread.
 */
class FrequencyViewModel(
    private val fftAnalyzer: FftAnalyzer,
    private val audioSource: AudioSource // Mock source representing the microphone stream
) : ViewModel() {

    /**
     * The frequency data stream exposed to the UI.
     * 
     * FLOW STRATEGY:
     * 1. .map: Executes the FFT analysis on each incoming buffer.
     * 2. .flowOn(Default): Offloads the CPU-intensive FFT logic to the Default dispatcher 
     *    to keep the UI at 60 FPS.
     * 3. .shareIn: Converts the cold flow to a hot SharedFlow, allowing multiple 
     *    UI components (Visualizer, Peak Sensors) to consume the same calculation.
     */
    val frequencyFlow: SharedFlow<BandEnergy> = audioSource.bufferFlow()
        .map { buffer ->
            fftAnalyzer.analyze(buffer)
        }
        .flowOn(Dispatchers.Default)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 1
        )
}

// Mock interface for the example
interface AudioSource {
    fun bufferFlow(): Flow<ShortArray>
}