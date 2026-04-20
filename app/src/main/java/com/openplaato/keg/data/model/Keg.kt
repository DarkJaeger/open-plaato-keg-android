package com.openplaato.keg.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Keg(
    val id: String = "",
    // Live readings
    val amount_left: Double? = null,
    val percent_of_beer_left: Double? = null,
    val keg_temperature: Double? = null,
    val temperature_unit: String? = "°C",
    val beer_left_unit: String? = "litre",
    val is_pouring: String? = "0",
    val last_pour: Double? = null,
    val weight_raw: String? = null,
    // User labels
    val my_label: String? = null,
    // Hardware config (stored as strings by the server)
    val unit: String? = null,            // "1"=metric, "2"=US
    val measure_unit: String? = null,    // "1"=weight, "2"=volume
    val sensitivity: String? = null,     // "1"–"4"
    val empty_keg_weight: String? = null,
    val max_keg_volume: String? = null,
    val temperature_offset: String? = null,
    @SerialName("keg_mode_c02_beer")
    val keg_mode: String? = null,        // "1"=beer, "2"=co2
) {
    /**
     * Heuristic-based detection of CO2 mode.
     * Prioritizes explicit keg_mode flag if set to "1" or "2".
     * Falls back to heuristics if keg_mode is null or blank.
     */
    val isCo2Mode: Boolean get() = when (keg_mode) {
        "2" -> true
        "1" -> false
        else -> {
            my_label?.contains("CO2", ignoreCase = true) == true ||
            beer_left_unit?.contains("CO2", ignoreCase = true) == true ||
            (amount_left ?: 0.0) < -0.1
        }
    }

    /**
     * Merges another Keg object into this one.
     * Use [mergeLive] for WebSocket updates to avoid overwriting optimistic config changes.
     */
    fun merge(other: Keg): Keg {
        return this.copy(
            amount_left = other.amount_left ?: this.amount_left,
            percent_of_beer_left = other.percent_of_beer_left ?: this.percent_of_beer_left,
            keg_temperature = other.keg_temperature ?: this.keg_temperature,
            temperature_unit = other.temperature_unit ?: this.temperature_unit,
            beer_left_unit = other.beer_left_unit ?: this.beer_left_unit,
            is_pouring = other.is_pouring ?: this.is_pouring,
            last_pour = other.last_pour ?: this.last_pour,
            weight_raw = other.weight_raw ?: this.weight_raw,
            my_label = other.my_label ?: this.my_label,
            unit = other.unit ?: this.unit,
            measure_unit = other.measure_unit ?: this.measure_unit,
            sensitivity = other.sensitivity ?: this.sensitivity,
            empty_keg_weight = other.empty_keg_weight ?: this.empty_keg_weight,
            max_keg_volume = other.max_keg_volume ?: this.max_keg_volume,
            temperature_offset = other.temperature_offset ?: this.temperature_offset,
            keg_mode = other.keg_mode ?: this.keg_mode,
        )
    }

    /**
     * Merges only live reading fields from another Keg object.
     * Preserves configuration fields.
     */
    fun mergeLive(other: Keg): Keg {
        return this.copy(
            amount_left = other.amount_left ?: this.amount_left,
            percent_of_beer_left = other.percent_of_beer_left ?: this.percent_of_beer_left,
            keg_temperature = other.keg_temperature ?: this.keg_temperature,
            temperature_unit = other.temperature_unit ?: this.temperature_unit,
            beer_left_unit = other.beer_left_unit ?: this.beer_left_unit,
            is_pouring = other.is_pouring ?: this.is_pouring,
            last_pour = other.last_pour ?: this.last_pour,
            weight_raw = other.weight_raw ?: this.weight_raw,
        )
    }
}

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
    val isCo2Mode: Boolean get() = keg?.isCo2Mode ?: false
}
