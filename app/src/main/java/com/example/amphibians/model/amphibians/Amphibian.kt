package com.example.amphibians.model.amphibians

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Amphibian (
    val id: Int = 0, // api doesn't have id
    @SerialName("name")
    val name: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("img_src")
    val imgSrc: String? = null
)