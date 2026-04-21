# Adaptive Audio Processing: The "Gaussian Engine"

This challenge solves a common problem in real-time audio analysis: detecting a clean "hit" from a noisy, mechanical signal.

## Real-World Scenario: The "Dirty" Kick Drum
Imagine a live stage environment. A drummer hits the kick, but the raw signal is a mess:
- **Mechanical "Clack"**: The plastic beater hits the head, creating a high-frequency spike.
- **Pedal Noise**: The iron spring of the pedal rattles, creating micro-vibrations.
- **Double-Bounce**: The beater accidentally grazes the head twice (the "ghost" hit).

## Comparison: Old vs. New

### ❌ The "Old" Way (Static Thresholds)
A simple threshold would trigger 3 times in 10ms. You'd try to fix it with a `sleep(200ms)`, but then you'd miss the drummer's actual fast double-stroke. It's a game of "whack-a-mole" with parameters.

### ✅ The "Adaptive" Way ( Gaussian Filter)
- **The Sieve**: The Gaussian Filter sees the "Clack" and the "Rattle" as thin noise. Because they lack the "mass" (low-frequency energy) of the actual drum hit, the filter rounds them down to near-zero.
- **The Slope**: We track the acceleration of the "rounded" curve. We don't wait for a threshold; we look for the **Momentum** (Derivative Analysis).
- **The Shield**: Once we detect the real peak, we calculate its **Symmetric Decay**. We don't use a fixed timer; we use the wave's own geometry to build a "protective shield" that ignores the "Double-Bounce" but stays ready for the next intentional hit.

## Key Result
A rock-solid, zero-latency trigger that feels like the software "understands" the physics of the drum.
