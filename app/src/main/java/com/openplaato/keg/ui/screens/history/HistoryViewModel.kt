package com.openplaato.keg.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.api.PlaatoApiService
import com.openplaato.keg.data.model.AirlockHistoryEntry
import com.openplaato.keg.data.model.KegHistoryEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val kegHistory: List<KegHistoryEntry> = emptyList(),
    val airlockHistory: List<AirlockHistoryEntry> = emptyList(),
    val error: String? = null,
    val currentRange: String = "7d"
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val api: PlaatoApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    fun loadKegHistory(id: String, range: String = "7d") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentRange = range, kegHistory = emptyList()) }
            try {
                val history = api.getKegHistory(id, range)
                _uiState.update { it.copy(kegHistory = history, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun loadAirlockHistory(id: String, range: String = "7d") {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, currentRange = range, airlockHistory = emptyList()) }
            try {
                val history = api.getAirlockHistory(id, range)
                _uiState.update { it.copy(airlockHistory = history, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
