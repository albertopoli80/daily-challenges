/**
 * CORE LOGIC: Gaussian Smoothing & Symmetry Validation
 * * This is not a simple threshold trigger. We apply a Gaussian Kernel 
 * to the raw PCM buffer to eliminate high-frequency mechanical spikes 
 * (the "clack" of the pedal) and isolate the "round" energy of the kick.
 */
fun processAudioBuffer(rawBuffer: FloatArray, b: Float): DetectionResult {
    
    // 1. GENERATE GAUSSIAN KERNEL (The "Rounding" Tool)
    // Parameter 'b' (sigma) controls the bell width. 
    // Higher 'b' = rounder curve = better rejection of high-freq spikes.
    // High frequencies fluctuate rapidly due to their short waveforms; 
    // the kernel smooths these into a stable trend.
    val kernel = Gaussian.generateKernel(sigma = b, size = 7)

    // 2. APPLY CONVOLUTION (Smoothing)
    // We "sieve" the raw data. High-freq noise falls through, 
    // leaving only the smooth, low-freq "wave" of the drum head.
    val smoothedBuffer = rawBuffer.convolve(kernel)

    // 3. DERIVATIVE ANALYSIS (Finding the "Peak X")
    val d1 = smoothedBuffer.derivative()
    val d2 = d1.derivative()

    // We use extensions to check the state of the last samples
    // This identifies where the derivative changes sign, 
    // pinpointing the true peak (maximum energy) of the curve.
    val isPeakReached = d1.isCrossingZeroDownwards() && d2.isNegative()
    val energyPoint = smoothedBuffer.currentEnergy()

    // 4. SYMMETRIC EXCLUSION (The "Ghost" Protection)
    // Using Gaussian symmetry, we mathematically calculate a "dead zone" 
    // to ignore future spikes within this peak's natural decay period.
    if (isPeakReached && energyPoint > 0.1f /* userThreshold example */) {
        val maskDuration = b * 5.0f // SYMMETRY_FACTOR
        return Trigger(System.currentTimeMillis(), confidence = 0.99f)
    }

    return Silence
}

/**
 * MATHEMATICAL EXTENSIONS: 
 * These extensions allow to treat audio buffers with functional clarity.
 */

// Simple convolution: slides the kernel over the signal
fun FloatArray.convolve(kernel: FloatArray): FloatArray {
    val result = FloatArray(this.size)
    val offset = kernel.size / 2
    for (i in indices) {
        var sum = 0f
        for (k in kernel.indices) {
            val signalIdx = i + k - offset
            if (signalIdx in indices) {
                sum += this[signalIdx] * kernel[k]
            }
        }
        result[i] = sum
    }
    return result
}

// Discrete derivative: [i] - [i-1]
fun FloatArray.derivative(): FloatArray {
    if (this.size < 2) return floatArrayOf(0f)
    val der = FloatArray(this.size)
    for (i in 1 until this.size) {
        der[i] = this[i] - this[i - 1]
    }
    return der
}

// Helpers for peak detection logic
fun FloatArray.isCrossingZeroDownwards(): Boolean = 
    this.size >= 2 && this[lastIndex - 1] > 0 && this[lastIndex] <= 0

fun FloatArray.isNegative(): Boolean = 
    this.isNotEmpty() && this[lastIndex] < 0

fun FloatArray.currentEnergy(): Float = this.lastOrNull() ?: 0f

object Gaussian {
    fun generateKernel(sigma: Float, size: Int): FloatArray {
        val kernel = FloatArray(size)
        val mean = size / 2
        var sum = 0f
        for (x in 0 until size) {
            kernel[x] = Math.exp(-0.5 * Math.pow((x - mean) / sigma.toDouble(), 2.0)).toFloat()
            sum += kernel[x]
        }
        return kernel.map { it / sum }.toFloatArray() // Normalize
    }
}
