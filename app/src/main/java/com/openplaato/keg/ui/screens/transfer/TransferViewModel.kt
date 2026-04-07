package com.openplaato.keg.ui.screens.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.model.TransferScale
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(private val repo: PlaatoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferScaleUiState())
    val uiState: StateFlow<TransferScaleUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            repo.wsEvents.collect { event ->
                if (event is WsEvent.TransferScaleUpdate) onWsUpdate(event.scale)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repo.getTransferScales()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    scales = result.getOrDefault(emptyList()),
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun onWsUpdate(scale: TransferScale) {
        _uiState.update { state ->
            val idx = state.scales.indexOfFirst { it.id == scale.id }
            val updated = if (idx >= 0) {
                val existing = state.scales[idx]
                val merged = scale.copy(
                    label = scale.label ?: existing.label,
                    empty_keg_weight = scale.empty_keg_weight ?: existing.empty_keg_weight,
                    target_weight = scale.target_weight ?: existing.target_weight,
                )
                state.scales.toMutableList().also { it[idx] = merged }
            } else {
                state.scales + scale
            }
            state.copy(scales = updated)
        }
    }
}
