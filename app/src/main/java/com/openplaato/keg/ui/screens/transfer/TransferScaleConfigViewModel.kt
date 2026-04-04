package com.openplaato.keg.ui.screens.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.TransferScale
import com.openplaato.keg.data.model.TransferScaleConfigBody
import com.openplaato.keg.data.repository.PlaatoRepository
import com.openplaato.keg.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransferScaleConfigUiState(
    val scale: TransferScale? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val feedback: String? = null,
    val labelInput: String = "",
    val emptyKegWeightInput: String = "",
    val targetWeightInput: String = "",
    val isSaving: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val kegsForAutofill: List<Keg>? = null,
)

@HiltViewModel
class TransferScaleConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PlaatoRepository,
) : ViewModel() {

    val scaleId: String = savedStateHandle[Screen.TransferScaleConfig.ARG] ?: ""

    private val _state = MutableStateFlow(TransferScaleConfigUiState())
    val state: StateFlow<TransferScaleConfigUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val scale = repo.getTransferScale(scaleId).getOrNull()
            _state.update {
                it.copy(
                    isLoading = false,
                    scale = scale,
                    labelInput = scale?.label ?: "",
                    emptyKegWeightInput = scale?.empty_keg_weight?.let { g -> "%.3f".format(g / 1000.0) } ?: "",
                    targetWeightInput = scale?.target_weight?.let { g -> "%.3f".format(g / 1000.0) } ?: "",
                    error = if (scale == null) "Transfer scale not found" else null,
                )
            }
        }
    }

    fun updateLabel(value: String) = _state.update { it.copy(labelInput = value) }
    fun updateEmptyKegWeight(value: String) = _state.update { it.copy(emptyKegWeightInput = value) }
    fun updateTargetWeight(value: String) = _state.update { it.copy(targetWeightInput = value) }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, feedback = null) }
            val body = TransferScaleConfigBody(
                label = _state.value.labelInput.trim().takeIf { it.isNotEmpty() },
                empty_keg_weight = _state.value.emptyKegWeightInput.trim().toDoubleOrNull()?.let { it * 1000.0 },
                target_weight = _state.value.targetWeightInput.trim().toDoubleOrNull()?.let { it * 1000.0 },
            )
            val result = repo.configureTransferScale(scaleId, body)
            if (result.isSuccess) {
                _state.update { it.copy(isSaving = false, feedback = "Saved") }
                delay(1500)
                _state.update { it.copy(feedback = null) }
                load()
            } else {
                _state.update {
                    it.copy(isSaving = false, feedback = "Failed: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun showDeleteDialog() = _state.update { it.copy(showDeleteDialog = true) }
    fun dismissDeleteDialog() = _state.update { it.copy(showDeleteDialog = false) }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(showDeleteDialog = false) }
            val result = repo.deleteTransferScale(scaleId)
            if (result.isSuccess) {
                onDeleted()
            } else {
                _state.update { it.copy(feedback = "Delete failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }

    fun loadKegsForAutofill() {
        viewModelScope.launch {
            val kegs = repo.getKegs().getOrDefault(emptyList())
            _state.update { it.copy(kegsForAutofill = kegs) }
        }
    }

    fun dismissAutofillDialog() = _state.update { it.copy(kegsForAutofill = null) }

    fun autofillFromKeg(keg: Keg) {
        val weightKg = keg.empty_keg_weight?.toDoubleOrNull()
        _state.update {
            it.copy(
                kegsForAutofill = null,
                emptyKegWeightInput = weightKg?.let { w -> "%.3f".format(w) } ?: it.emptyKegWeightInput,
            )
        }
    }
}
