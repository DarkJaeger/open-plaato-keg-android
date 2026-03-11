package com.openplaato.keg.ui.screens.scales

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Keg
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

enum class TareState { IDLE, TARING, RELEASING, DONE, ERROR }

data class ScaleConfigUiState(
    val keg: Keg? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val feedback: String? = null,

    // Editable field buffers
    val emptyKegWeightInput: String = "",
    val maxVolumeInput: String = "",
    val calWeightInput: String = "",
    val tempOffsetInput: String = "",

    // Tare / empty-keg hold state
    val tareState: TareState = TareState.IDLE,
    val emptyKegState: TareState = TareState.IDLE,
)

@HiltViewModel
class ScaleConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PlaatoRepository,
) : ViewModel() {

    val kegId: String = savedStateHandle[Screen.ScaleConfig.ARG] ?: ""

    private val _state = MutableStateFlow(ScaleConfigUiState())
    val state: StateFlow<ScaleConfigUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val keg = repo.getKegs().getOrNull()?.find { it.id == kegId }
            _state.update {
                it.copy(
                    isLoading = false,
                    keg = keg,
                    emptyKegWeightInput = keg?.empty_keg_weight ?: "",
                    maxVolumeInput = keg?.max_keg_volume ?: "",
                    tempOffsetInput = keg?.temperature_offset ?: "",
                    error = if (keg == null) "Scale not found" else null,
                )
            }
        }
    }

    fun updateInput(block: ScaleConfigUiState.() -> ScaleConfigUiState) = _state.update(block)

    // ---- Unit toggles (fire-and-forget; reload on success) ----

    fun setUnit(value: String) = sendCommand { repo.setUnit(kegId, value) }
    fun setMeasureUnit(value: String) = sendCommand { repo.setMeasureUnit(kegId, value) }
    fun setSensitivity(value: String) = sendCommand { repo.setSensitivity(kegId, value) }
    fun setKegMode(value: String) = sendCommand { repo.setKegMode(kegId, value) }

    // ---- Calibration inputs ----

    fun saveEmptyKegWeight() = sendCommand {
        repo.setEmptyKegWeight(kegId, _state.value.emptyKegWeightInput.trim())
    }

    fun saveMaxVolume() = sendCommand {
        repo.setMaxKegVolume(kegId, _state.value.maxVolumeInput.trim())
    }

    fun saveCalibrateKnownWeight() = sendCommand {
        repo.calibrateKnownWeight(kegId, _state.value.calWeightInput.trim())
    }

    fun saveTemperatureOffset() = sendCommand {
        repo.setTemperatureOffset(kegId, _state.value.tempOffsetInput.trim())
    }

    // ---- Tare (send command, hold 3 s, release) ----

    fun tare() {
        viewModelScope.launch {
            _state.update { it.copy(tareState = TareState.TARING, feedback = null) }
            val result = repo.tare(kegId)
            if (result.isFailure) {
                _state.update { it.copy(tareState = TareState.ERROR, feedback = "Tare failed") }
                return@launch
            }
            _state.update { it.copy(tareState = TareState.RELEASING, feedback = "Holding…") }
            delay(3000)
            repo.tareRelease(kegId)
            _state.update { it.copy(tareState = TareState.DONE, feedback = "Tare complete") }
            delay(2000)
            _state.update { it.copy(tareState = TareState.IDLE, feedback = null) }
            load()
        }
    }

    // ---- Set empty keg (same hold pattern) ----

    fun setEmptyKeg() {
        viewModelScope.launch {
            _state.update { it.copy(emptyKegState = TareState.TARING, feedback = null) }
            val result = repo.emptyKeg(kegId)
            if (result.isFailure) {
                _state.update { it.copy(emptyKegState = TareState.ERROR, feedback = "Command failed") }
                return@launch
            }
            _state.update { it.copy(emptyKegState = TareState.RELEASING, feedback = "Holding…") }
            delay(3000)
            repo.emptyKegRelease(kegId)
            _state.update { it.copy(emptyKegState = TareState.DONE, feedback = "Empty keg set") }
            delay(2000)
            _state.update { it.copy(emptyKegState = TareState.IDLE, feedback = null) }
            load()
        }
    }

    // ---- Reset last pour ----

    fun resetLastPour() = sendCommand { repo.resetLastPour(kegId) }

    // ---- Helper ----

    private fun sendCommand(block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _state.update { it.copy(feedback = null) }
            val result = block()
            if (result.isSuccess) {
                _state.update { it.copy(feedback = "Saved") }
                delay(1500)
                _state.update { it.copy(feedback = null) }
                load()
            } else {
                _state.update { it.copy(feedback = "Failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }
}
