package com.openplaato.keg.ui.screens.scales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScalesUiState(
    val kegs: List<Keg> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ScalesViewModel @Inject constructor(
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScalesUiState())
    val uiState: StateFlow<ScalesUiState> = _uiState.asStateFlow()

    init {
        load()
        observeWsEvents()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repo.getKegs()
            _uiState.update {
                it.copy(
                    kegs = result.getOrDefault(emptyList()),
                    isLoading = false,
                    error = if (result.isFailure) "Could not reach server" else null,
                )
            }
        }
    }

    private fun observeWsEvents() {
        viewModelScope.launch {
            repo.wsEvents.collect { event ->
                if (event is WsEvent.KegUpdate) {
                    _uiState.update { state ->
                        val idx = state.kegs.indexOfFirst { it.id == event.keg.id }
                        val updated = if (idx >= 0)
                            state.kegs.toMutableList().also { it[idx] = event.keg }
                        else
                            state.kegs + event.keg
                        state.copy(kegs = updated)
                    }
                }
            }
        }
    }
}
