package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoodHistoryDto(
    val id: Int? = null,

    @SerialName("signature_id")
    val signatureId: String,

    val mood: Int,

    @SerialName("created_at")
    val createdAt: String
)