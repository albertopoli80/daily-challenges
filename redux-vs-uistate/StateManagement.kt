package com.example.adaptivebpm.ui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * COMMON STATE OBJECT
 */
data class CounterState(
    val count: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

// --- APPROACH 1: STANDARD UI STATE (Jetpack Compose Style) ---
// This is the "Standard" way: simple, direct, and less boilerplate.
class CounterViewModel {
    private val _uiState = MutableStateFlow(CounterState())
    val uiState = _uiState.asStateFlow()

    fun increment() {
        _uiState.update { it.copy(isLoading = true) }
        // Imagine an async call here
        _uiState.update { it.copy(count = it.count + 1, isLoading = false) }
    }

    fun setError(message: String) {
        _uiState.update { it.copy(error = message, isLoading = false) }
    }
}

// --- APPROACH 2: REDUX/MVI STYLE (Action-Driven) ---
// This is the "Predictable" way: every change is an explicit Action.
sealed class CounterAction {
    object StartIncrement : CounterAction()
    data class IncrementSuccess(val newValue: Int) : CounterAction()
    data class OnError(val message: String) : CounterAction()
}

class ReduxStore {
    private val _state = MutableStateFlow(CounterState())
    val state = _state.asStateFlow()

    // The Reducer: Pure function (State, Action) -> NewState
    fun dispatch(action: CounterAction) {
        _state.update { currentState ->
            when (action) {
                is CounterAction.StartIncrement -> 
                    currentState.copy(isLoading = true)
                
                is CounterAction.IncrementSuccess -> 
                    currentState.copy(count = action.newValue, isLoading = false, error = null)
                
                is CounterAction.OnError -> 
                    currentState.copy(error = action.message, isLoading = false)
            }
        }
    }
}