package com.openplaato.keg.ui.screens.scales

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.KegHistoryEntry
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.repository.PlaatoRepository
import com.openplaato.keg.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "KegMode"

enum class TareState { IDLE, TARING, RELEASING, DONE, ERROR }

data class ScaleConfigUiState(
    val keg: Keg? = null,
    val history: List<KegHistoryEntry> = emptyList(),
    val currentRange: String = "7d",
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

    init {
        load()
        loadHistory()
        observeWebSocket()
    }

    private fun observeWebSocket() {
        repo.wsEvents
            .filterIsInstance<WsEvent.KegUpdate>()
            .onEach { event ->
                if (event.keg.id == kegId) {
                    // mergeLive keeps config fields (including keg_mode set by load() or an
                    // optimistic update) and refreshes only live sensor readings from WS.
                    _state.update { it.copy(keg = it.keg?.mergeLive(event.keg) ?: event.keg) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun load(showLoader: Boolean = true) {
        viewModelScope.launch {
            if (showLoader) _state.update { it.copy(isLoading = true, error = null) }
            val keg = repo.getKegs().getOrNull()?.find { it.id == kegId }
            Log.d(TAG, "load() — keg_mode from server: ${keg?.keg_mode}")
            _state.update {
                it.copy(
                    isLoading = false,
                    keg = keg,
                    emptyKegWeightInput = it.emptyKegWeightInput.ifBlank { keg?.empty_keg_weight ?: "" },
                    maxVolumeInput = it.maxVolumeInput.ifBlank { keg?.max_keg_volume ?: "" },
                    tempOffsetInput = it.tempOffsetInput.ifBlank { keg?.temperature_offset ?: "" },
                    error = if (keg == null) "Scale not found" else null,
                )
            }
        }
    }

    fun loadHistory(range: String = _state.value.currentRange) {
        viewModelScope.launch {
            _state.update { it.copy(currentRange = range) }
            val history = repo.getKegHistory(kegId, range).getOrNull() ?: emptyList()
            _state.update { it.copy(history = history) }
        }
    }

    fun updateInput(block: ScaleConfigUiState.() -> ScaleConfigUiState) = _state.update(block)

    // ---- Unit toggles (optimistic update + fire-and-forget) ----

    fun setUnit(value: String) {
        val mapped = if (value == "us") "2" else "1"
        _state.update { it.copy(keg = it.keg?.copy(unit = mapped)) }
        sendCommand { repo.setUnit(kegId, mapped) }
    }

    fun setMeasureUnit(value: String) {
        val mapped = if (value == "weight") "1" else "2"
        _state.update { it.copy(keg = it.keg?.copy(measure_unit = mapped)) }
        sendCommand { repo.setMeasureUnit(kegId, mapped) }
    }

    fun setSensitivity(value: String) {
        val mapped = when (value) {
            "very_low" -> "1"
            "low" -> "2"
            "medium" -> "3"
            "high" -> "4"
            else -> "3"
        }
        _state.update { it.copy(keg = it.keg?.copy(sensitivity = mapped)) }
        sendCommand { repo.setSensitivity(kegId, mapped) }
    }

    fun setKegMode(value: String) {
        val mapped = if (value == "co2") "2" else "1"
        Log.d(TAG, "setKegMode() — value=$value mapped=$mapped current=${_state.value.keg?.keg_mode}")
        _state.update { it.copy(keg = it.keg?.copy(keg_mode = mapped)) }
        Log.d(TAG, "setKegMode() — optimistic state now: ${_state.value.keg?.keg_mode}")
        sendCommand { repo.setKegMode(kegId, mapped) }
    }

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
                load(showLoader = false)
            } else {
                _state.update { it.copy(feedback = "Failed: ${result.exceptionOrNull()?.message}") }
                load(showLoader = false) // Rollback to actual server state
            }
        }
    }
}
