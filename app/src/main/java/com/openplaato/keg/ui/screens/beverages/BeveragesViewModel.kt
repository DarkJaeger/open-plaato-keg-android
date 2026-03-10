package com.openplaato.keg.ui.screens.beverages

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.data.repository.PlaatoRepository
import com.openplaato.keg.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BeveragesUiState(
    val beverages: List<Beverage> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class BeveragesViewModel @Inject constructor(private val repo: PlaatoRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BeveragesUiState())
    val uiState: StateFlow<BeveragesUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repo.getBeverages()
            _uiState.update { it.copy(beverages = result.getOrDefault(emptyList()), isLoading = false) }
        }
    }
}

// ---- Edit ViewModel ----

data class BeverageEditState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val id: String = "",
    val name: String = "",
    val brewery: String = "",
    val style: String = "",
    val abv: String = "",
    val ibu: String = "",
    val color: String = "#c9a849",
    val description: String = "",
    val tastingNotes: String = "",
    val og: String = "",
    val fg: String = "",
)

@HiltViewModel
class BeverageEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val bevId: String = savedStateHandle[Screen.BeverageEdit.ARG] ?: "new"

    private val _state = MutableStateFlow(BeverageEditState())
    val state: StateFlow<BeverageEditState> = _state.asStateFlow()

    init {
        if (bevId != "new") load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val bev = repo.getBeverages().getOrNull()?.find { it.id == bevId }
            if (bev != null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        id = bev.id,
                        name = bev.name ?: "",
                        brewery = bev.brewery ?: "",
                        style = bev.style ?: "",
                        abv = bev.abv ?: "",
                        ibu = bev.ibu ?: "",
                        color = bev.color ?: "#c9a849",
                        description = bev.description ?: "",
                        tastingNotes = bev.tasting_notes ?: "",
                        og = bev.og ?: "",
                        fg = bev.fg ?: "",
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun update(block: BeverageEditState.() -> BeverageEditState) = _state.update(block)

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isSaving = true, error = null) }
            val id = if (bevId == "new") generateId() else bevId
            val body = Beverage(
                id = id,
                name = s.name.trim(),
                brewery = s.brewery.trim(),
                style = s.style.trim(),
                abv = s.abv.trim(),
                ibu = s.ibu.trim(),
                color = s.color.trim(),
                description = s.description.trim(),
                tasting_notes = s.tastingNotes.trim(),
                og = s.og.trim().takeIf { it.isNotBlank() },
                fg = s.fg.trim().takeIf { it.isNotBlank() },
                source = "manual",
            )
            val result = repo.saveBeverage(id, body)
            if (result.isSuccess) {
                _state.update { it.copy(isSaving = false) }
                onSuccess()
            } else {
                _state.update { it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Save failed") }
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        if (bevId == "new") return
        viewModelScope.launch {
            repo.deleteBeverage(bevId)
            onSuccess()
        }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(8)
}
