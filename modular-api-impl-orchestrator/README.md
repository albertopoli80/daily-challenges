# Modular Architecture: API/Impl Split & UseCase Orchestration

This challenge explores the professional "API/Impl Split" structure, used in large-scale Android projects (such as banking or enterprise apps) to ensure scalability, fast build times, and the absence of circular dependencies.

## Key Principles

### 1. API/Impl Separation
Each feature (e.g., Profile, Login, Payments) is divided into two separate Gradle modules:
- **`:feature-api`**: Contains only **Interfaces** (Repositories, Services) and **Domain Models**. It has no business logic and does not depend on other `impl` modules.
- **`:feature-impl`**: Contains the **Concrete Logic**, databases, network calls, and **UseCases**. It depends on its own `:feature-api` and the `:feature-api` of other modules it needs to interact with.

### 2. The Role of the Interface (Contract)
The interface in the `api` module (e.g., `IProfileRepository`) is not a UseCase. It is an **entry point for data** or elementary functions. It serves to decouple "what" is needed from "how" it is implemented.

### 3. The UseCase as an Orchestrator
The **UseCase** resides in the `impl` module. Its responsibility is to:
- Coordinate (orchestrate) different repositories.
- Apply Business Logic.
- Be injected into the ViewModel.

## Advantages
- **Zero Circular Dependencies**: `impl` modules only communicate via other modules' `api`. The dependency graph remains unidirectional.
- **Incremental Builds**: Modifying code in an `impl` module does not require recompilation of modules that depend on its `api`.
- **Testability**: It is extremely easy to create "Mocks" of the `api` interfaces to test UseCases or ViewModels in isolation.

---

## Example Flow
1. `LoginViewModel` (in `:login-impl`) calls `LoginUseCase`.
2. `LoginUseCase` (in `:login-impl`) validates credentials and, if correct, calls `IProfileRepository.updateLastLogin()` (interface defined in `:profile-api`).
3. Hilt will inject `ProfileRepositoryImpl` (from `:profile-impl`) at runtime to perform the operation.
