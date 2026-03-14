package com.openplaato.keg.ui.screens.scales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.preferences.AppPreferences
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScalesUiState(
    val kegs: List<Keg> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class ScalesViewModel @Inject constructor(
    private val repo: PlaatoRepository,
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScalesUiState())
    val uiState: StateFlow<ScalesUiState> = _uiState.asStateFlow()

    init {
        load()
        observeWsEvents()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repo.getKegs()
            if (result.isFailure) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not reach server")
                }
                return@launch
            }

            val fetched = result.getOrDefault(emptyList())
            val savedOrder = prefs.kegOrder.first()
            val ordered = applyOrder(fetched, savedOrder)

            _uiState.update { it.copy(kegs = ordered, isLoading = false, error = null) }
        }
    }

    /**
     * Reorders kegs in memory and persists the new order to DataStore.
     * Called by the UI drag callback; [from] and [to] are list indices.
     */
    fun moveKeg(from: Int, to: Int) {
        val current = _uiState.value.kegs.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        current.add(to, current.removeAt(from))
        _uiState.update { it.copy(kegs = current) }
        viewModelScope.launch {
            prefs.setKegOrder(current.map { it.id })
        }
    }

    private fun observeWsEvents() {
        viewModelScope.launch {
            repo.wsEvents.collect { event ->
                if (event is WsEvent.KegUpdate) {
                    _uiState.update { state ->
                        val idx = state.kegs.indexOfFirst { it.id == event.keg.id }
                        val updated = if (idx >= 0) {
                            // Update the keg data in-place so the user's custom order is preserved.
                            state.kegs.toMutableList().also { it[idx] = event.keg }
                        } else {
                            // New keg from WebSocket — append to end (will be sorted to end on next load too).
                            state.kegs + event.keg
                        }
                        state.copy(kegs = updated)
                    }
                }
            }
        }
    }

    /**
     * Sorts [kegs] according to [savedOrder] (list of IDs).
     * Kegs not present in [savedOrder] are appended in their original API order.
     */
    private fun applyOrder(kegs: List<Keg>, savedOrder: List<String>): List<Keg> {
        if (savedOrder.isEmpty()) return kegs
        val byId = kegs.associateBy { it.id }
        val ordered = savedOrder.mapNotNull { byId[it] }
        val remaining = kegs.filter { it.id !in savedOrder }
        return ordered + remaining
    }
}
