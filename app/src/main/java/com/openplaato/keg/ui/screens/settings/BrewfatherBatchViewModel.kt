package com.openplaato.keg.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.BrewfatherBatch
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrewfatherBatchUiState(
    val batches: List<BrewfatherBatch> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val importing: String? = null,        // batch id currently being imported
    val importedIds: Set<String> = emptySet(),
    val feedback: String? = null,
)

@HiltViewModel
class BrewfatherBatchViewModel @Inject constructor(
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrewfatherBatchUiState())
    val state: StateFlow<BrewfatherBatchUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, feedback = null) }
            val result = repo.getBrewfatherBatches()
            if (result.isSuccess) {
                _state.update { it.copy(isLoading = false, batches = result.getOrDefault(emptyList())) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Failed to load batches"
                _state.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    fun importBatch(batchId: String) {
        viewModelScope.launch {
            _state.update { it.copy(importing = batchId, feedback = null) }
            val result = repo.importBrewfatherBatch(batchId)
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        importing = null,
                        importedIds = it.importedIds + batchId,
                        feedback = "Imported to beverage library",
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        importing = null,
                        feedback = "Import failed: ${result.exceptionOrNull()?.message}",
                    )
                }
            }
        }
    }
}
