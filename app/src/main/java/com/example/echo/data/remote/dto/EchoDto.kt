package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EchoDto(
    val id: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("signature_id") val signatureId: String? = null, // Made nullable to prevent crashes
    val replies: List<ReplyDto>? = null
)