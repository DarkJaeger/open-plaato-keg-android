package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TransferScale(
    val id: String = "",
    val label: String? = null,
    val raw_weight: Double? = null,
    val empty_keg_weight: Double? = null,
    val target_weight: Double? = null,
    val fill_percent: Double? = null,
    val last_updated: Long? = null,
) {
    val displayName: String get() = label?.takeIf { it.isNotBlank() } ?: id
}
