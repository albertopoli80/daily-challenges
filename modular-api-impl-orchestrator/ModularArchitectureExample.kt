package com.example.modular.architecture

/**
 * MODULAR ARCHITECTURE EXAMPLE (MULTI-MODULE SIMULATION)
 * 
 * In a real project, these classes would be distributed across separate Gradle modules.
 */

// --- MODULE: :core-api (Common Data) ---
data class UserProfile(val id: String, val name: String, val photoUrl: String)
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}

// =================================================================================
// SECTION: PROFILE MODULE
// =================================================================================

// --- MODULE: :feature-profile-api ---
interface IProfileRepository {
    suspend fun getProfile(userId: String): Result<UserProfile>
    suspend fun updateLastLoginTime(userId: String)
}

// --- MODULE: :feature-profile-impl ---
class ProfileRepositoryImpl : IProfileRepository {
    override suspend fun getProfile(userId: String): Result<UserProfile> {
        // Simulating API/DB call
        return Result.Success(UserProfile(userId, "John Doe", "https://avatar.io/123"))
    }

    override suspend fun updateLastLoginTime(userId: String) {
        println("ProfileModule: Updating login timestamp for user $userId")
    }
}

// =================================================================================
// SECTION: LOGIN MODULE
// =================================================================================

// --- MODULE: :feature-login-api ---
interface ILoginService {
    suspend fun performLogin(user: String, pass: String): Result<Boolean>
}

// --- MODULE: :feature-login-impl ---

/**
 * USE CASE: THE ORCHESTRATOR
 * Resides in the 'impl' module because it contains business logic and coordinates multiple modules.
 * 
 * NOTE: There is no circular dependency because LoginUseCase asks for IProfileRepository (Interface).
 * The Login-Impl module knows nothing about the internal code of Profile-Impl.
 */
class LoginUseCase(
    private val profileRepository: IProfileRepository, // Interface from another module
    private val loginRepo: InternalLoginRepository      // Local repository of this module
) {
    suspend fun execute(user: String, pass: String): Result<Boolean> {
        val loginResult = loginRepo.authenticate(user, pass)
        
        if (loginResult is Result.Success) {
            // Orchestration: after successful login, update the profile status
            profileRepository.updateLastLoginTime(user)
            return Result.Success(true)
        }
        
        return Result.Error("Invalid credentials")
    }
}

class InternalLoginRepository {
    suspend fun authenticate(u: String, p: String): Result<Boolean> = Result.Success(true)
}

// =================================================================================
// SECTION: APP MODULE (The Glue)
// =================================================================================

/**
 * Example of how Hilt/Dagger would handle "Binding" in the main module.
 * This allows interfaces to be resolved to their concrete implementations.
 */
object DependencyInjectionMock {
    // In Hilt, you would use @Binds within a @Module
    fun provideProfileRepository(): IProfileRepository = ProfileRepositoryImpl()
    
    fun provideLoginUseCase(): LoginUseCase {
        return LoginUseCase(
            profileRepository = provideProfileRepository(),
            loginRepo = InternalLoginRepository()
        )
    }
}

fun main() {
    // Simulating a ViewModel calling the UseCase
    val useCase = DependencyInjectionMock.provideLoginUseCase()
    
    // Execution using coroutines
    kotlinx.coroutines.runBlocking {
        println("Starting Login process...")
        val finalResult = useCase.execute("admin", "1234")
        
        when (finalResult) {
            is Result.Success -> println("Login completed successfully!")
            is Result.Error -> println("Error: ${finalResult.message}")
        }
    }
}
