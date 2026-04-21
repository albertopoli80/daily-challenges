/**
 * Example: Counter with different Dispatchers.
 * * SCENARIO: The user taps furiously on the screen. 
 * We want to update the UI counter and log the value.
 */

// --- APPROACH 1: DISPATCHERS.DEFAULT (The common mistake) ---
fun onUserTapDefault() {
    CoroutineScope(Dispatchers.Default).launch {
        // 1. The tap is moved to the CPU thread pool.
        // 2. If the pool is saturated (e.g., other background calculations), 
        //    the tap "waits its turn".
        val result = incrementCounter() 
        
        withContext(Dispatchers.Main) {
            // 3. Switch back to Main to update the UI.
            updateUi(result)
        }
    }
}

// --- APPROACH 2: DISPATCHERS.MAIN.IMMEDIATE (The "Queue-Jumper") ---
fun onUserTapImmediate() {
    CoroutineScope(Dispatchers.Main.immediate).launch {
        // 1. Check: are we already on the Main Thread? YES.
        // 2. INSTANT execution. We don't go through the message queue.
        // NOTE: While faster, if overused or for heavy tasks, it can starve the 
        // UI loop, leading to dropped frames (jank) and potential stuttering.
        val result = incrementCounter()
        updateUi(result)
    }
}