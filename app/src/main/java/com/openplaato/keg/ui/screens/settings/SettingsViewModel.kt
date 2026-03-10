package com.openplaato.keg.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val serverUrl: String = "",
    val saved: Boolean = false,
    val airlockEnabled: Boolean = true,
    val brewfatherConfigured: Boolean = false,
    val brewfatherUserId: String = "",
    val brewfatherApiKey: String = "",
    val brewfatherSaved: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val url = repo.serverUrl.first()
            _state.update { it.copy(serverUrl = url) }
        }
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val appConfigDeferred = async { repo.getAppConfig() }
            val bfConfigDeferred = async { repo.getBrewfatherConfig() }
            appConfigDeferred.await().getOrNull()?.let { config ->
                _state.update { it.copy(airlockEnabled = config.airlock_enabled) }
            }
            bfConfigDeferred.await().getOrNull()?.let { config ->
                _state.update { it.copy(brewfatherConfigured = config.configured) }
            }
        }
    }

    fun onUrlChange(url: String) = _state.update { it.copy(serverUrl = url, saved = false) }

    fun save() {
        viewModelScope.launch {
            repo.saveServerUrl(_state.value.serverUrl.trim())
            _state.update { it.copy(saved = true) }
        }
    }

    fun toggleAirlockEnabled(enabled: Boolean) {
        _state.update { it.copy(airlockEnabled = enabled) }
        viewModelScope.launch { repo.setAirlockEnabled(enabled) }
    }

    fun onBrewfatherUserIdChange(value: String) =
        _state.update { it.copy(brewfatherUserId = value, brewfatherSaved = false) }

    fun onBrewfatherApiKeyChange(value: String) =
        _state.update { it.copy(brewfatherApiKey = value, brewfatherSaved = false) }

    fun saveBrewfatherCreds() {
        viewModelScope.launch {
            val result = repo.saveBrewfatherCreds(
                _state.value.brewfatherUserId.trim(),
                _state.value.brewfatherApiKey.trim(),
            )
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        brewfatherSaved = true,
                        brewfatherConfigured = _state.value.brewfatherUserId.isNotBlank(),
                    )
                }
            }
        }
    }
}
