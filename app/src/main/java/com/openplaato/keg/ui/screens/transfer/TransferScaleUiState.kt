package com.openplaato.keg.ui.screens.transfer

import com.openplaato.keg.data.model.TransferScale

data class TransferScaleUiState(
    val scales: List<TransferScale> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
