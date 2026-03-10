package com.openplaato.keg.ui.screens.taplist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.api.WsEvent
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapWithKeg
import com.openplaato.keg.data.repository.PlaatoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TapListUiState(
    val items: List<TapWithKeg> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class TapListViewModel @Inject constructor(
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TapListUiState())
    val uiState: StateFlow<TapListUiState> = _uiState.asStateFlow()

    val serverUrl = repo.serverUrl
    val wsEvents: SharedFlow<WsEvent> get() = repo.wsEvents

    // Local keg cache for WebSocket updates
    private val kegCache = mutableMapOf<String, Keg>()

    init {
        load()
        observeWsEvents()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val tapsResult = repo.getTaps()
            val kegsResult = repo.getKegs()

            if (tapsResult.isFailure) {
                _uiState.update { it.copy(isLoading = false, error = "Could not connect to server") }
                return@launch
            }

            val taps: List<Tap> = tapsResult.getOrDefault(emptyList())
            val kegs: List<Keg> = kegsResult.getOrDefault(emptyList())

            kegs.forEach { kegCache[it.id] = it }

            val items = buildItems(taps, kegCache)
            _uiState.update { it.copy(items = items, isLoading = false, error = null) }
        }
    }

    fun connectWebSocket(baseUrl: String) = repo.connectWebSocket(baseUrl)

    private fun observeWsEvents() {
        viewModelScope.launch {
            wsEvents.collect { event ->
                when (event) {
                    is WsEvent.KegUpdate -> {
                        kegCache[event.keg.id] = event.keg
                        _uiState.update { state ->
                            state.copy(items = state.items.map { twk ->
                                if (twk.tap.keg_id == event.keg.id) twk.copy(keg = event.keg) else twk
                            })
                        }
                    }
                    is WsEvent.AirlockUpdate -> { /* handled by AirlocksScreen */ }
                }
            }
        }
    }

    private fun buildItems(taps: List<Tap>, kegs: Map<String, Keg>): List<TapWithKeg> =
        taps
            .sortedWith(compareBy(nullsLast()) { it.tap_number })
            .map { tap -> TapWithKeg(tap = tap, keg = tap.keg_id?.let { kegs[it] }) }
}
