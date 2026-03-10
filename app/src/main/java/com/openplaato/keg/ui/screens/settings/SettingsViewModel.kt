package com.openplaato.keg.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    }

    fun onUrlChange(url: String) = _state.update { it.copy(serverUrl = url, saved = false) }

    fun save() {
        viewModelScope.launch {
            repo.saveServerUrl(_state.value.serverUrl.trim())
            _state.update { it.copy(saved = true) }
        }
    }
}
