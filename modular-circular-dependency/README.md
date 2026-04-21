# Solving Circular Dependencies with Interface-Based Modularity

In large-scale Android applications, modules often need to talk to each other. A common mistake is creating a direct dependency (`Module A -> Module B` and `Module B -> Module A`), which results in a **Circular Dependency Error**.

## The Solution: Dependency Inversion (API & Implementation)

Instead of modules depending on each other's *internals*, we split each feature into two separate Gradle modules:
1. **`:feature-api`**: Contains only Interfaces, DTOs, and Constants. No logic.
2. **`:feature-impl`**: Contains ViewModels, DataSources, and UI.

### The Dependency Graph
- **Module A (Impl)** depends on **Module A (API)** and **Module B (API)**.
- **Module B (Impl)** depends on **Module B (API)** and **Module A (API)**.

Since neither implementation module knows about the other's "heavy" code (UI, DB, logic), the circularity is broken at the compilation level.

## How it works with Hilt
Hilt acts as the "matchmaker." 
- `Module A` asks Hilt for an instance of `ModuleB_Interface`.
- Hilt knows that `ModuleB_Impl` is the one providing that interface.
- It injects the implementation without `Module A` ever knowing that `Module B` even exists as a concrete class.

## Real-World Benefits
1. **Compilation Speed**: If you change the UI in `Module B`, `Module A` does **not** need to recompile because the interface in the API module hasn't changed.
2. **Team Independence**: One team can work on the implementation of A while another works on B, agreeing only on the shared "Contract" (the Interface).
3. **Clean Testing**: You can easily swap the real implementation with a `Fake` or `Mock` during testing without touching the other module.

## The "Provider" Trick
If you have a circularity even at the logic level (e.g., calling each other in a loop), use `javax.inject.Provider<Interface>`. This delays the resolution of the dependency until the moment it's actually used, preventing crashes during object creation.
