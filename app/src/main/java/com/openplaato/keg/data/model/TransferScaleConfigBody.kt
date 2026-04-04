package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransferScaleConfigBody(
    val label: String? = null,
    val empty_keg_weight: Double? = null,
    val target_weight: Double? = null,
)
