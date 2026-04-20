package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KegHistoryEntry(
    val timestamp: Long,
    val amount_left: Double? = null,
    val percent_of_beer_left: Double? = null,
    val keg_temperature: Double? = null,
    val beer_left_unit: String? = null,
    val temperature_unit: String? = null
)

@Serializable
data class AirlockHistoryEntry(
    val timestamp: Long,
    val temperature: Double? = null,
    val bubbles_per_min: Double? = null
)
