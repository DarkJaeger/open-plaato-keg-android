package com.openplaato.keg.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Beverage(
    val id: String = "",
    val name: String? = null,
    val brewery: String? = null,
    val style: String? = null,
    val abv: String? = null,
    val ibu: String? = null,
    val color: String? = null,
    val description: String? = null,
    val tasting_notes: String? = null,
    val og: String? = null,
    val fg: String? = null,
    val srm: String? = null,
    val source: String? = null,
    val brewfather_batch_id: String? = null,
    val created_at: String? = null,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: "Unnamed"
}

@Serializable
data class StatusResponse(
    val status: String? = null,
    val error: String? = null,
    val id: String? = null,
)

@Serializable
data class ValueBody(val value: String)
