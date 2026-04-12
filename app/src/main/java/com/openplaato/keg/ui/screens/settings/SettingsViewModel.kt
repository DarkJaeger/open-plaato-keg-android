package com.openplaato.keg.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.preferences.AppPreferences
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
    val serverVersion: String? = null,
    val serverVersionError: String? = null,
    val airlockEnabled: Boolean = true,
    val brewfatherConfigured: Boolean = false,
    val brewfatherUserId: String = "",
    val brewfatherApiKey: String = "",
    val brewfatherSaved: Boolean = false,
    val pourNotificationsEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: PlaatoRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val url = repo.serverUrl.first()
            _state.update { it.copy(serverUrl = url) }
            loadServerVersion(url)
        }
        viewModelScope.launch {
            val enabled = prefs.pourNotificationsEnabled.first()
            _state.update { it.copy(pourNotificationsEnabled = enabled) }
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
            val url = _state.value.serverUrl.trim()
            repo.saveServerUrl(url)
            _state.update { it.copy(saved = true) }
            loadServerVersion(url)
        }
    }

    fun refreshServerVersion() {
        viewModelScope.launch {
            loadServerVersion(_state.value.serverUrl)
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

    fun togglePourNotifications() {
        val enabled = !_state.value.pourNotificationsEnabled
        _state.update { it.copy(pourNotificationsEnabled = enabled) }
        viewModelScope.launch { prefs.setPourNotificationsEnabled(enabled) }
    }

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

    private suspend fun loadServerVersion(url: String) {
        val result = repo.getServerVersion(url)
        val error = result.exceptionOrNull()?.let { throwable ->
            throwable.message ?: throwable::class.java.simpleName
        }
        _state.update {
            it.copy(
                serverVersion = result.getOrNull(),
                serverVersionError = error,
            )
        }
    }
}
