package com.openplaato.keg.ui.screens.airlocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AirlocksUiState(
    val airlocks: List<Airlock> = emptyList(),
    val isLoading: Boolean = true,
    val airlockEnabled: Boolean = true,
)

@HiltViewModel
class AirlocksViewModel @Inject constructor(private val repo: PlaatoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AirlocksUiState())
    val uiState: StateFlow<AirlocksUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val configDeferred = async { repo.getAppConfig() }
            val airlocksDeferred = async { repo.getAirlocks() }
            val enabled = configDeferred.await().getOrNull()?.airlock_enabled ?: true
            val airlocks = airlocksDeferred.await().getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    isLoading = false,
                    airlockEnabled = enabled,
                    airlocks = if (enabled) airlocks else emptyList(),
                )
            }
        }
    }

    fun onWsUpdate(airlock: Airlock) {
        _uiState.update { state ->
            if (!state.airlockEnabled) return@update state
            val existing = state.airlocks.indexOfFirst { it.id == airlock.id }
            val updated = if (existing >= 0) {
                state.airlocks.toMutableList().also { it[existing] = airlock }
            } else {
                state.airlocks + airlock
            }
            state.copy(airlocks = updated)
        }
    }
}
