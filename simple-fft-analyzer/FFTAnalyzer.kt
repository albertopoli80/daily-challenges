package com.example.adaptivebpm.domain.processor

import kotlin.math.*

/**
 * Optimized FFT Analyzer (Iterative, In-place).
 * Designed for Android performance to prevent UI thread freezing and minimize GC pressure.
 * 
 * SCENARIO: Real-time audio spectrum visualization and frequency-band analysis.
 * PURPOSE: Acting as a "Virtual Sensor" that can trigger events based on specific frequency energy.
 */
class FftAnalyzer(val fftSize: Int = 1024) {
    private val real = DoubleArray(fftSize)
    private val imag = DoubleArray(fftSize)
    
    // Hamming window to reduce spectral leakage
    private val window = DoubleArray(fftSize) { i ->
        0.54 - 0.46 * cos(2.0 * PI * i / (fftSize - 1))
    }

    // Reusable buffer for magnitudes to expose to the UI
    private val magnitudes = FloatArray(fftSize / 2)

    // Pre-computed bit-reversal table to optimize the sorting phase
    private val reverseTable = IntArray(fftSize) { i ->
        var r = 0
        var x = i
        val bits = (ln(fftSize.toDouble()) / ln(2.0)).toInt()
        for (j in 0 until bits) {
            r = (r shl 1) or (x and 1)
            x = x shr 1
        }
        r
    }

    /**
     * Analyzes the audio buffer and extracts energy from specific bands.
     * @param buffer Raw PCM short array from the microphone.
     * @return BandEnergy containing low/high band values and the full magnitude spectrum.
     */
    fun analyze(buffer: ShortArray): BandEnergy {
        // 1. PREPARATION WITH WINDOWING (In-place bit-reversal)
        // We move samples to their bit-reversed positions immediately to save a pass.
        for (i in 0 until fftSize) {
            val idx = reverseTable[i]
            if (i < buffer.size) {
                real[idx] = buffer[i].toDouble() * window[i]
            } else {
                real[idx] = 0.0
            }
            imag[idx] = 0.0
        }

        // 2. ITERATIVE FFT (In-place) - ZERO ALLOCATIONS
        // Using an iterative Cooley-Tukey approach avoids the stack overhead of recursion.
        var n = 1
        while (n < fftSize) {
            val step = n shl 1
            val theta = -PI / n
            for (i in 0 until n) {
                val wRe = cos(i * theta)
                val wIm = sin(i * theta)
                for (j in i until fftSize step step) {
                    val k = j + n
                    val tRe = wRe * real[k] - wIm * imag[k]
                    val tIm = wRe * imag[k] + wIm * real[k]
                    real[k] = (real[j] - tRe)
                    imag[k] = (imag[j] - tIm)
                    real[j] += tRe
                    imag[j] += tIm
                }
            }
            n = step
        }

        // 3. ENERGY EXTRACTION & BAND ANALYSIS
        // We divide the spectrum into "Virtual Sensors" (e.g., Low for Kick, High for Cymbals).
        var lowSum = 0.0
        var highSum = 0.0
        for (i in 0 until fftSize / 2) {
            val mag = sqrt(real[i] * real[i] + imag[i] * imag[i]).toFloat()
            magnitudes[i] = mag

            // Example frequency mapping (depending on sample rate)
            if (i in 1..4) lowSum += mag      // Low-end energy (Virtual Kick Sensor)
            if (i in 116..350) highSum += mag // High-end energy (Virtual Sparkle/Clarity Sensor)
        }

        return BandEnergy(
            low = (lowSum / (fftSize * 500.0)).toFloat(),
            high = (highSum / (fftSize * 500.0)).toFloat(),
            allMagnitudes = magnitudes.copyOf()
        )
    }

    data class BandEnergy(
        val low: Float,
        val high: Float,
        val allMagnitudes: FloatArray
    )
}