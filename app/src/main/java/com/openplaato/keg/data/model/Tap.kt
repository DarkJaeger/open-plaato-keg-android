package com.openplaato.keg.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tap(
    val id: String = "",
    val tap_number: Int? = null,
    val name: String? = null,
    val brewery: String? = null,
    val style: String? = null,
    val abv: String? = null,
    val ibu: String? = null,
    val color: String? = null,
    val description: String? = null,
    val tasting_notes: String? = null,
    val keg_id: String? = null,
    val handle_image: String? = null,
    val device_id: String? = null,
)

@Serializable
data class TapHandleUploadResponse(val filename: String)

@Serializable
data class TapSaveBody(
    val tap_number: Int? = null,
    val name: String = "",
    val brewery: String = "",
    val style: String = "",
    val abv: String = "",
    val ibu: String = "",
    val color: String = "#c9a849",
    val description: String = "",
    val tasting_notes: String = "",
    val keg_id: String? = null,
    val handle_image: String? = null,
    val device_id: String? = null,
)
