package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Airlock(
    val id: String = "",
    val label: String? = null,
    val temperature: Double? = null,
    val bubbles_per_min: Double? = null,
    // Grainfather config
    val grainfather_enabled: String? = null,
    val grainfather_unit: String? = null,
    val grainfather_specific_gravity: String? = null,
    val grainfather_url: String? = null,
    // Brewfather config
    val brewfather_enabled: String? = null,
    val brewfather_temp_unit: String? = null,
    val brewfather_sg: String? = null,
    val brewfather_url: String? = null,
    val brewfather_og: String? = null,
    val brewfather_batch_volume: String? = null,
) {
    val displayName: String get() = label?.takeIf { it.isNotBlank() } ?: id
    val isGrainfatherEnabled: Boolean get() = grainfather_enabled == "true"
    val isBrewfatherEnabled: Boolean get() = brewfather_enabled == "true"
}
