package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Airlock(
    val id: String = "",
    val label: String? = null,
    val temperature: Double? = null,
    val bubbles_per_min: Double? = null,
) {
    val displayName: String get() = label?.takeIf { it.isNotBlank() } ?: id
}
