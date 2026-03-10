package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Keg(
    val id: String = "",
    val amount_left: Double? = null,
    val percent_of_beer_left: Double? = null,
    val keg_temperature: Double? = null,
    val temperature_unit: String? = "°C",
    val beer_left_unit: String? = "litre",
    val is_pouring: String? = "0",
    val last_pour: Double? = null,
    val my_label: String? = null,
)

/** Merged view of a Tap + its live Keg data for the list screen. */
data class TapWithKeg(
    val tap: Tap,
    val keg: Keg? = null,
) {
    val displayName: String get() = tap.name?.takeIf { it.isNotBlank() } ?: "Tap ${tap.tap_number ?: "?"}"
    val amountLeft: Double get() = keg?.amount_left ?: 0.0
    val percentLeft: Double get() = keg?.percent_of_beer_left ?: 0.0
    val temperature: Double? get() = keg?.keg_temperature
    val tempUnit: String get() = keg?.temperature_unit ?: "°C"
    val volumeUnit: String get() {
        return when (keg?.beer_left_unit) {
            "litre" -> "L"
            else -> keg?.beer_left_unit ?: "L"
        }
    }
    val isPouring: Boolean get() = keg?.is_pouring?.let { it != "0" && it.isNotBlank() } ?: false
    val lastPour: Double get() = keg?.last_pour ?: 0.0
    val isLow: Boolean get() = percentLeft in 0.01..19.99
}
