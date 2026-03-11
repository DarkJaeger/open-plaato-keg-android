package com.openplaato.keg.ui.screens.airlocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Airlock
import com.openplaato.keg.data.model.BrewfatherBody
import com.openplaato.keg.data.model.GrainfatherBody
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GrainfatherInputs(
    val enabled: Boolean = false,
    val unit: String = "celsius",
    val sg: String = "1.0",
    val url: String = "",
)

data class BrewfatherInputs(
    val enabled: Boolean = false,
    val unit: String = "celsius",
    val sg: String = "1.0",
    val url: String = "",
    val og: String = "",
    val batchVolume: String = "",
)

data class AirlockSetupUiState(
    val airlocks: List<Airlock> = emptyList(),
    val labelInputs: Map<String, String> = emptyMap(),
    val grainfatherInputs: Map<String, GrainfatherInputs> = emptyMap(),
    val brewfatherInputs: Map<String, BrewfatherInputs> = emptyMap(),
    val isLoading: Boolean = true,
    val feedback: String? = null,
)

@HiltViewModel
class AirlockSetupViewModel @Inject constructor(
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AirlockSetupUiState())
    val state: StateFlow<AirlockSetupUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val airlocks = repo.getAirlocks().getOrDefault(emptyList())
            _state.update {
                it.copy(
                    isLoading = false,
                    airlocks = airlocks,
                    labelInputs = airlocks.associate { a -> a.id to (a.label ?: "") },
                    grainfatherInputs = airlocks.associate { a ->
                        a.id to GrainfatherInputs(
                            enabled = a.isGrainfatherEnabled,
                            unit = a.grainfather_unit ?: "celsius",
                            sg = a.grainfather_specific_gravity ?: "1.0",
                            url = a.grainfather_url ?: "",
                        )
                    },
                    brewfatherInputs = airlocks.associate { a ->
                        a.id to BrewfatherInputs(
                            enabled = a.isBrewfatherEnabled,
                            unit = a.brewfather_temp_unit ?: "celsius",
                            sg = a.brewfather_sg ?: "1.0",
                            url = a.brewfather_url ?: "",
                            og = a.brewfather_og ?: "",
                            batchVolume = a.brewfather_batch_volume ?: "",
                        )
                    },
                )
            }
        }
    }

    fun updateLabel(id: String, value: String) =
        _state.update { it.copy(labelInputs = it.labelInputs + (id to value)) }

    fun updateGrainfather(id: String, block: GrainfatherInputs.() -> GrainfatherInputs) {
        val current = _state.value.grainfatherInputs[id] ?: GrainfatherInputs()
        _state.update { it.copy(grainfatherInputs = it.grainfatherInputs + (id to current.block())) }
    }

    fun updateBrewfather(id: String, block: BrewfatherInputs.() -> BrewfatherInputs) {
        val current = _state.value.brewfatherInputs[id] ?: BrewfatherInputs()
        _state.update { it.copy(brewfatherInputs = it.brewfatherInputs + (id to current.block())) }
    }

    fun saveLabel(id: String) = sendCommand("Label saved") {
        repo.setAirlockLabel(id, _state.value.labelInputs[id]?.trim() ?: "")
    }

    fun saveGrainfather(id: String) {
        val inputs = _state.value.grainfatherInputs[id] ?: return
        sendCommand("Grainfather saved") {
            repo.setGrainfather(
                id, GrainfatherBody(
                    enabled = inputs.enabled,
                    unit = inputs.unit,
                    specific_gravity = inputs.sg.trim(),
                    url = inputs.url.trim(),
                )
            )
        }
    }

    fun saveBrewfather(id: String) {
        val inputs = _state.value.brewfatherInputs[id] ?: return
        sendCommand("Brewfather saved") {
            repo.setBrewfather(
                id, BrewfatherBody(
                    enabled = inputs.enabled,
                    unit = inputs.unit,
                    specific_gravity = inputs.sg.trim(),
                    url = inputs.url.trim(),
                    og = inputs.og.trim().takeIf { it.isNotBlank() },
                    batch_volume = inputs.batchVolume.trim().takeIf { it.isNotBlank() },
                )
            )
        }
    }

    private fun sendCommand(successMsg: String, block: suspend () -> Result<*>) {
        viewModelScope.launch {
            val result = block()
            if (result.isSuccess) {
                _state.update { it.copy(feedback = successMsg) }
                delay(1500)
                _state.update { it.copy(feedback = null) }
                load()
            } else {
                _state.update { it.copy(feedback = "Failed: ${result.exceptionOrNull()?.message}") }
            }
        }
    }
}
