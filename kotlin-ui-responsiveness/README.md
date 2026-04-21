# Kotlin UI Responsiveness: Dispatchers Comparison

This challenge demonstrates the impact of using different Coroutine Dispatchers when handling high-frequency UI events, such as a user "tapping furiously" on a counter button.

## The Scenario
We need to increment a counter and update the UI immediately every time the user taps. Even small delays can make the app feel "mushy" or unresponsive.

## Approaches

### 1. `Dispatchers.Default` (The Common Mistake)
In this approach, the work is offloaded to a background worker thread.
- **Problem**: Even if the task is light, switching threads introduces latency.
- **Risk**: If the CPU-bound thread pool is saturated by other background tasks, the UI update will wait in a queue, causing noticeable lag between the physical tap and the visual feedback.

### 2. `Dispatchers.Main.immediate` (The "Queue-Jumper")
This approach executes the coroutine immediately if we are already on the Main thread, bypassing the standard `Handler` message queue.
- **Benefit**: Zero-latency UI updates. The response is instantaneous.
- **Trade-off/Risk**: Because it bypasses the queue, if the code inside is heavy or triggered too many times per second, it can **starve the UI loop**. This prevents the system from rendering frames, leading to **jank (dropped frames)** and potential app instability/stuttering.

## Key Takeaway
Use `Dispatchers.Main.immediate` for UI logic that must feel instantaneous, but ensure the work performed is extremely lightweight to avoid blocking the rendering pipeline.
