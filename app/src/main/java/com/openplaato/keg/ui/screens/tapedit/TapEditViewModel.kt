package com.openplaato.keg.ui.screens.tapedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openplaato.keg.data.model.Beverage
import com.openplaato.keg.data.model.Keg
import com.openplaato.keg.data.model.Tap
import com.openplaato.keg.data.model.TapSaveBody
import com.openplaato.keg.data.repository.PlaatoRepository
import com.openplaato.keg.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TapEditState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val tap: Tap? = null,
    val kegs: List<Keg> = emptyList(),
    val beverages: List<Beverage> = emptyList(),

    // Form fields
    val tapNumber: String = "",
    val name: String = "",
    val brewery: String = "",
    val style: String = "",
    val abv: String = "",
    val ibu: String = "",
    val color: String = "#c9a849",
    val description: String = "",
    val tastingNotes: String = "",
    val kegId: String = "",
    val deviceId: String = "",
    val handleImage: String? = null,
    val tapHandles: List<String> = emptyList(),
    val isUploadingHandle: Boolean = false,
)

@HiltViewModel
class TapEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: PlaatoRepository,
) : ViewModel() {

    private val tapId: String = savedStateHandle[Screen.TapEdit.ARG] ?: "new"

    private val _state = MutableStateFlow(TapEditState())
    val state: StateFlow<TapEditState> = _state.asStateFlow()
    val serverUrl get() = repo.serverUrl

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val kegs = repo.getKegs().getOrDefault(emptyList())
            val beverages = repo.getBeverages().getOrDefault(emptyList())
            val tapHandles = repo.getTapHandles().getOrDefault(emptyList())

            if (tapId == "new") {
                _state.update {
                    it.copy(isLoading = false, kegs = kegs, beverages = beverages, tapHandles = tapHandles)
                }
                return@launch
            }

            val tap = repo.getTaps().getOrNull()?.find { it.id == tapId }
            if (tap != null) {
                _state.update { s ->
                    s.copy(
                        isLoading = false,
                        tap = tap,
                        kegs = kegs,
                        beverages = beverages,
                        tapHandles = tapHandles,
                        tapNumber = tap.tap_number?.toString() ?: "",
                        name = tap.name ?: "",
                        brewery = tap.brewery ?: "",
                        style = tap.style ?: "",
                        abv = tap.abv ?: "",
                        ibu = tap.ibu ?: "",
                        color = tap.color ?: "#c9a849",
                        description = tap.description ?: "",
                        tastingNotes = tap.tasting_notes ?: "",
                        kegId = tap.keg_id ?: "",
                        deviceId = tap.device_id ?: "",
                        handleImage = tap.handle_image,
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false, error = "Tap not found") }
            }
        }
    }

    fun update(block: TapEditState.() -> TapEditState) = _state.update(block)

    fun selectHandle(filename: String?) = _state.update { it.copy(handleImage = filename) }

    fun uploadHandle(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingHandle = true) }
            repo.uploadTapHandle(context, uri).getOrNull()?.let { resp ->
                _state.update {
                    it.copy(
                        handleImage = resp.filename,
                        tapHandles = (it.tapHandles + resp.filename).distinct(),
                        isUploadingHandle = false,
                    )
                }
            } ?: _state.update { it.copy(isUploadingHandle = false) }
        }
    }

    fun fillFromBeverage(bev: Beverage) {
        _state.update { s ->
            s.copy(
                name = bev.name ?: s.name,
                brewery = bev.brewery ?: s.brewery,
                style = bev.style ?: s.style,
                abv = bev.abv ?: s.abv,
                ibu = bev.ibu ?: s.ibu,
                color = bev.color ?: s.color,
                description = bev.description ?: s.description,
                tastingNotes = bev.tasting_notes ?: s.tastingNotes,
            )
        }
    }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            _state.update { it.copy(isSaving = true, error = null) }

            val id = if (tapId == "new") generateId() else tapId
            val body = TapSaveBody(
                tap_number = s.tapNumber.toIntOrNull(),
                name = s.name.trim(),
                brewery = s.brewery.trim(),
                style = s.style.trim(),
                abv = s.abv.trim(),
                ibu = s.ibu.trim(),
                color = s.color.trim().let { if (it.startsWith("#")) it else "#$it" },
                description = s.description.trim(),
                tasting_notes = s.tastingNotes.trim(),
                keg_id = s.kegId.takeIf { it.isNotBlank() },
                device_id = s.deviceId.trim().takeIf { it.isNotBlank() },
                handle_image = s.handleImage,
            )

            val result = repo.saveTap(id, body)
            if (result.isSuccess) {
                _state.update { it.copy(isSaving = false, saved = true) }
                onSuccess()
            } else {
                _state.update {
                    it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Save failed")
                }
            }
        }
    }

    fun delete(onSuccess: () -> Unit) {
        if (tapId == "new") return
        viewModelScope.launch {
            repo.deleteTap(tapId)
            onSuccess()
        }
    }

    private fun generateId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(12)
}
