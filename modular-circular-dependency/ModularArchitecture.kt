package com.example.modular.architecture

import javax.inject.Inject
import javax.inject.Singleton
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// --- MODULE A INTERFACE (The "API" Module) ---
// This module contains ONLY interfaces. No heavy logic, no UI.
// Both Module A and Module B can depend on this.
interface ModuleA_Interface {
    fun getProfileName(): String
}

// --- MODULE B INTERFACE (The "API" Module) ---
interface ModuleB_Interface {
    fun triggerNotification(message: String)
}

// --- MODULE A IMPLEMENTATION ---
// Depends on: ModuleA_Interface (to implement it) AND ModuleB_Interface (to use it)
class ModuleA_Impl @Inject constructor(
    private val moduleB: ModuleB_Interface // We use B without knowing its internal implementation!
) : ModuleA_Interface {
    override fun getProfileName(): String {
        moduleB.triggerNotification("Accessing Profile...")
        return "John Doe"
    }
}

// --- MODULE B IMPLEMENTATION ---
// Depends on: ModuleB_Interface (to implement it) AND ModuleA_Interface (to use it)
class ModuleB_Impl @Inject constructor(
    private val moduleA: javax.inject.Provider<ModuleA_Interface> // Use Provider if you have circularity at runtime
) : ModuleB_Interface {
    override fun triggerNotification(message: String) {
        println("Sending notification to: ${message}")
    }
}

// --- HILT BINDINGS (The "Bridge") ---
// These usually live in a "DI" or "App" module that sees everything.
@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {

    @Binds
    @Singleton
    abstract fun bindModuleA(impl: ModuleA_Impl): ModuleA_Interface

    @Binds
    @Singleton
    abstract fun bindModuleB(impl: ModuleB_Impl): ModuleB_Interface
}